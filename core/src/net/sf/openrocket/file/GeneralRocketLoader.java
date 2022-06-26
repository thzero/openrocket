package net.sf.openrocket.file;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import net.sf.openrocket.aerodynamics.WarningSet;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.OpenRocketDocumentFactory;
import net.sf.openrocket.file.openrocket.importt.OpenRocketLoader;
import net.sf.openrocket.file.rocksim.importt.RocksimLoader;
import net.sf.openrocket.util.ArrayUtils;
import net.sf.openrocket.util.TextUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A rocket loader that auto-detects the document type and uses the appropriate
 * loading.  Supports loading of GZIPed files as well with transparent
 * uncompression.
 * 
 * @author Sampo Niskanen <sampo.niskanen@iki.fi>
 */
public class GeneralRocketLoader {
	private static final Logger log = LoggerFactory.getLogger(GeneralRocketLoader.class);

	protected final WarningSet warnings = new WarningSet();

	private static final int READ_BYTES = 300;
	private static final byte[] GZIP_SIGNATURE = { 31, -117 }; // 0x1f, 0x8b
	private static final byte[] ZIP_SIGNATURE = TextUtil.asciiBytes("PK");
	private static final byte[] OPENROCKET_SIGNATURE = TextUtil.asciiBytes("<openrocket");
	private static final byte[] ROCKSIM_SIGNATURE = TextUtil.asciiBytes("<RockSimDoc");
	
	private final OpenRocketLoader openRocketLoader = new OpenRocketLoader();
	
	private final RocksimLoader rocksimLoader = new RocksimLoader();
	
	private final File baseFile;
	private final URL jarURL;
	private boolean isContainer;
	
	private final MotorFinder motorFinder;
	private AttachmentFactory attachmentFactory;
	private final OpenRocketDocument doc = OpenRocketDocumentFactory.createEmptyRocket();
	
	public GeneralRocketLoader(File file) {
		this.baseFile = file;
		this.jarURL = null;
		this.motorFinder = new DatabaseMotorFinder();
	}
	
	public GeneralRocketLoader(URL jarURL) {
		this.baseFile = null;
		this.jarURL = jarURL;
		this.motorFinder = new DatabaseMotorFinder();
	}
	
	/**
	 * Loads a rocket from the File object used in the constructor
	 */
	public final OpenRocketDocument load() throws RocketLoadException {
		warnings.clear();

		InputStream stream = null;
		try {
			stream = new BufferedInputStream(new FileInputStream(baseFile));
			load(stream);
			return doc;
		} catch (Exception ex) {
			throw new RocketLoadException("Exception loading file: " + baseFile + " , " + ex.getMessage(), ex);
		} finally {
			if (stream != null) {
				try {
					stream.close();
				} catch (IOException ex) {
					log.error("Exception loading file while closing stream: " + baseFile, ex);
				}
			}
		}
	}
	
	public final OpenRocketDocument load(InputStream source) throws RocketLoadException {
		try {
			loadStep1(source);
			doc.getRocket().enableEvents();
			return doc;
		} catch (Exception ex) {
			throw new RocketLoadException("Exception loading stream: " + ex.getMessage(), ex);
		}
	}
	
	public final WarningSet getWarnings() {
		return warnings;
	}
	
	/**
	 * This method determines the type file contained in the stream then calls the appropriate loading mecahnism.
	 * 
	 * If the stream is a gzip file, the argument is wrapped in a GzipInputStream and the rocket loaded.
	 * 
	 * If the stream is a zip container, the first zip entry with name ending in .ork or .rkt is loaded as the rocket.
	 * 
	 * If the stream is neither, then it is assumed to be an xml file containing either an ork or rkt format rocket.
	 * 
	 * @param source
	 * @throws IOException
	 * @throws RocketLoadException
	 */
	private void loadStep1(InputStream source) throws IOException, RocketLoadException {
		// Check for mark() support
		if (!source.markSupported()) {
			source = new BufferedInputStream(source);
		}
		
		// Read using mark()
		byte[] buffer = new byte[READ_BYTES];
		int count;
		source.mark(READ_BYTES + 10);
		count = source.read(buffer);
		source.reset();
		
		if (count < 10) {
			throw new RocketLoadException("Unsupported or corrupt file.");
		}

		// Detect the appropriate loader
		
		// Check for GZIP
		if (buffer[0] == GZIP_SIGNATURE[0] && buffer[1] == GZIP_SIGNATURE[1]) {
			isContainer = false;
			setAttachmentFactory();
			loadRocket(new GZIPInputStream(source));
			return;
		}
		
		// Check for ZIP (for future compatibility)
		if (buffer[0] == ZIP_SIGNATURE[0] && buffer[1] == ZIP_SIGNATURE[1]) {
			isContainer = true;
			setAttachmentFactory();

			ZipInputStream searchStream = null;
			try {
				// Search for entry with name *.ork
				searchStream = new ZipInputStream(source);

				// This while loop does not actually loop.
//				while (true) {
					ZipEntry entry = searchStream.getNextEntry();
					if (entry == null) {
						throw new RocketLoadException("Unsupported or corrupt file.");
					}
					if (entry.getName().matches(".*\\.[oO][rR][kK]$")) {
						loadRocket(searchStream);
					} else if (entry.getName().matches(".*\\.[rR][kK][tT]$")) {
						loadRocket(searchStream);
					}
					return;
//				}
			}
			finally {
				if (searchStream != null) {
					try {
						searchStream.close();
					} catch (IOException ex) {
						log.error("Exception while finding entry with name of *.ork.", ex);
					}
				}
			}
		}
		
		isContainer = false;
		setAttachmentFactory();
		loadRocket(source);
	}
	
	private void loadRocket(InputStream source) throws IOException, RocketLoadException {
		// Check for mark() support
		if (!source.markSupported()) {
			source = new BufferedInputStream(source);
		}
		
		// Read using mark()
		byte[] buffer = new byte[READ_BYTES];
		int count;
		source.mark(READ_BYTES + 10);
		count = source.read(buffer);
		source.reset();
		
		if (count < 10) {
			throw new RocketLoadException("Unsupported or corrupt file.");
		}
		
		// Check for OpenRocket
		int match = 0;
		for (int i = 0; i < count; i++) {
			if (buffer[i] == OPENROCKET_SIGNATURE[match]) {
				match++;
				if (match == OPENROCKET_SIGNATURE.length) {
					loadUsing(openRocketLoader, source);
					return;
				}
			} else {
				match = 0;
			}
		}
		
		byte[] typeIdentifier = ArrayUtils.copyOf(buffer, ROCKSIM_SIGNATURE.length);
		if (Arrays.equals(ROCKSIM_SIGNATURE, typeIdentifier)) {
			loadUsing(rocksimLoader, source);
			return;
		}

		throw new RocketLoadException("Unsupported or corrupt file.");
	}
	
	private void setAttachmentFactory() {
		attachmentFactory = new FileSystemAttachmentFactory(null);
		if (jarURL != null && isContainer) {
			attachmentFactory = new ZipFileAttachmentFactory(jarURL);
			return;
		}

		if (isContainer) {
			try {
				attachmentFactory = new ZipFileAttachmentFactory(baseFile.toURI().toURL());
			} catch (MalformedURLException ignore) { }
		} else if (baseFile != null) {
			attachmentFactory = new FileSystemAttachmentFactory(baseFile.getParentFile());
		}
	}
	
	private void loadUsing(RocketLoader loader, InputStream source) throws RocketLoadException {
		warnings.clear();
		DocumentLoadingContext context = new DocumentLoadingContext();
		context.setOpenRocketDocument(doc);
		context.setMotorFinder(motorFinder);
		context.setAttachmentFactory(attachmentFactory);
		loader.load(context, source);
		warnings.addAll(loader.getWarnings());
	}
}
