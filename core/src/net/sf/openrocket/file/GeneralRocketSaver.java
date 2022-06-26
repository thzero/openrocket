package net.sf.openrocket.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.sf.openrocket.appearance.Appearance;
import net.sf.openrocket.appearance.Decal;
import net.sf.openrocket.appearance.DecalImage;
import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.StorageOptions;
import net.sf.openrocket.document.StorageOptions.FileType;
import net.sf.openrocket.file.openrocket.OpenRocketSaver;
import net.sf.openrocket.file.rocksim.export.RocksimSaver;
import net.sf.openrocket.rocketcomponent.InsideColorComponent;
import net.sf.openrocket.rocketcomponent.RocketComponent;
import net.sf.openrocket.util.DecalNotFoundException;
import net.sf.openrocket.util.MathUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneralRocketSaver {
	private static final Logger log = LoggerFactory.getLogger(GeneralRocketLoader.class);
	
	/**
	 * Save the document to the specified file using the default storage options.
	 * 
	 * @param dest			the destination file.
	 * @param document		the document to save.
	 * @throws IOException	in case of an I/O error.
	 */
	public final void save(File dest, OpenRocketDocument document) throws IOException, DecalNotFoundException {
		save(dest, document, document.getDefaultStorageOptions());
	}
	
	/**
	 * Save the document to the specified file using the given storage options.
	 * 
	 * @param dest			the destination file.
	 * @param document		the document to save.
	 * @param options		the storage options.
	 * @throws IOException	in case of an I/O error.
	 */
	public final void save(File dest, OpenRocketDocument document, StorageOptions options) throws IOException, DecalNotFoundException {
		save(dest, document, options, null);
	}
	
	/**
	 * Save the document to a file with default StorageOptions and a SavingProgress callback object.
	 * 
	 * @param dest			the destination stream.
	 * @param doc			the document to save.
	 * @param progress      a SavingProgress object used to provide progress information
	 * @throws IOException	in case of an I/O error.
	 */
	public final void save(File dest, OpenRocketDocument doc, SavingProgress progress) throws IOException, DecalNotFoundException {
		save(dest, doc, doc.getDefaultStorageOptions(), progress);
	}
	
	/**
	 * Save the document to a file with the given StorageOptions and a SavingProgress callback object.
	 * 
	 * @param dest			the destination stream.
	 * @param doc			the document to save.
	 * @param opts			the storage options.
	 * @param progress      a SavingProgress object used to provide progress information
	 * @throws IOException	in case of an I/O error.
	 */
	public final void save(File dest, OpenRocketDocument doc, StorageOptions opts, SavingProgress progress) throws IOException, DecalNotFoundException {
		// This method is the core operational method.  It saves the document into a new (hopefully unique)
		// file, then if the save is successful, it will copy the file over the old one.
		
		// Write to a temporary file in the same directory as the specified file.
		File temporaryNewFile = File.createTempFile("ORSave", ".tmp", dest.getParentFile());
		
		OutputStream stream = null;
		try {
			stream = new BufferedOutputStream(new FileOutputStream(temporaryNewFile));
			if (progress != null) {
				long estimatedSize = this.estimateFileSize(doc, opts);
				stream = new ProgressOutputStream(stream, estimatedSize, progress);
			}

			try {
				save(dest.getName(), stream, doc, opts);
			} catch (DecalNotFoundException ex) {
				temporaryNewFile.delete();
				throw ex;
			}
		} finally {
			try {
				if (stream != null) {
					stream.close();
				}
			} catch (IOException ex) {
				log.error("Exception saving the file", ex);
			}
		}
		
		// Move the temporary new file over the specified file.
		boolean destExists = dest.exists();
		File oldBackupFile = new File(dest.getParentFile(), dest.getName() + "-bak");
		if (destExists) {
			dest.renameTo(oldBackupFile);
		}

		// since we created the temporary new file in the same directory as the dest file,
		// it is on the same filesystem, so File.renameTo will work just fine.
		boolean success = temporaryNewFile.renameTo(dest);
		if (success) {
			if (destExists) {
				oldBackupFile.delete();
			}
		}
	}
	
	/**
	 * Provide an estimate of the file size when saving the document with the
	 * specified options.  This is used as an indication to the user and when estimating
	 * file save progress.
	 * 
	 * @param doc		the document.
	 * @param options	the save options, compression must be taken into account.
	 * @return			the estimated number of bytes the storage would take.
	 */
	public long estimateFileSize(OpenRocketDocument doc, StorageOptions options) {
		if (options.getFileType() == StorageOptions.FileType.ROCKSIM) {
			return new RocksimSaver().estimateFileSize(doc, options);
		}

		return new OpenRocketSaver().estimateFileSize(doc, options);
	}
	
	private void save(String fileName, OutputStream output, OpenRocketDocument document, StorageOptions options) throws IOException, DecalNotFoundException {
		// For now, we don't save decal information in ROCKSIM files, so don't do anything which follows.
		// TODO - add support for decals in ROCKSIM files?
		if (options.getFileType() == FileType.ROCKSIM) {
			saveInternal(output, document, options);
			output.close();
			return;
		}
		
		Set<DecalImage> usedDecals = new TreeSet<DecalImage>();
		
		// Look for all decals used in the rocket.
		Appearance ap;
		Appearance ap_in;
		for (RocketComponent component : document.getRocket()) {
			ap = component.getAppearance();
			ap_in = null;
			if (component instanceof InsideColorComponent) {
				ap_in = ((InsideColorComponent) component).getInsideColorComponentHandler().getInsideAppearance();
			}

			if ((ap == null) && (ap_in == null)) {
				continue;
			}

			if (ap != null) {
				Decal decal = ap.getTexture();
				if (decal != null)
					usedDecals.add(decal.getImage());
			}
			if (ap_in != null) {
				Decal decal = ap_in.getTexture();
				if (decal != null)
					usedDecals.add(decal.getImage());
			}
		}
		
		saveAllPartsZipFile(output, document, options, usedDecals);
	}
	
	public void saveAllPartsZipFile(OutputStream output, OpenRocketDocument document, StorageOptions options, Set<DecalImage> decals) throws IOException, DecalNotFoundException {
		ZipOutputStream zos = null;
		try {
			zos = new ZipOutputStream(output);
			zos.setLevel(9);

			ZipEntry mainFile = new ZipEntry("rocket.ork");
			zos.putNextEntry(mainFile);
			saveInternal(zos, document, options);
			zos.closeEntry();
			
			// Now we write out all the decal images files.
			String name;
			ZipEntry decal;
			InputStream is;
			int bytesRead;
			byte[] buffer;
			for (DecalImage image : decals) {
				if (image.isIgnored()) {
					image.setIgnored(false);
					continue;
				}

				name = image.getName();
				decal = new ZipEntry(name);
				zos.putNextEntry(decal);
				
				is = image.getBytes();
				bytesRead = 0;
				buffer = new byte[2048];
				while ((bytesRead = is.read(buffer)) > 0) {
					zos.write(buffer, 0, bytesRead);
				}
				zos.closeEntry();
			}
			
			zos.flush();
		} finally {
			try {
				if (zos != null) {
					zos.close();
				}
			} catch (IOException ex) {
				log.error("Exception saving the file", ex);
			}
		}
	}

	// package scope for testing.
	
	private void saveInternal(OutputStream output, OpenRocketDocument document, StorageOptions options)
			throws IOException {
		if (options.getFileType() == StorageOptions.FileType.ROCKSIM) {
			new RocksimSaver().save(output, document, options);
			return;
		}

		new OpenRocketSaver().save(output, document, options);
	}

	/**
	 * Interface which can be implemented by the caller to receive progress information.
	 *
	 */
	public interface SavingProgress {
		/**
		 * Inform the callback of the current progress.
		 * It is guaranteed that the value will be an integer between 0 and 100 representing
		 * percent complete.  The SavingProgress object might not be notified the through
		 * setProgress when the save is complete.  When called with the value 100, the saving process
		 * may not be complete, do not use this as an indication of completion.
		 *
		 * @param progress  int value between 0 and 100 representing percent complete.
		 */
		public void setProgress(int progress);
	}
	
	private static class ProgressOutputStream extends FilterOutputStream {
		private final long estimatedSize;
		private long bytesWritten = 0;
		private final SavingProgress progressCallback;
		
		ProgressOutputStream(OutputStream stream, long estimatedSize, SavingProgress progressCallback) {
			super(stream);
			this.estimatedSize = estimatedSize;
			this.progressCallback = progressCallback;
		}
		
		@Override
		public void write(int b) throws IOException {
			super.write(b);
			bytesWritten++;
			updateProgress();
		}
		
		@Override
		public void write(byte[] b) throws IOException {
			super.write(b);
			bytesWritten += b.length;
			updateProgress();
		}
		
		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			super.write(b, off, len);
			bytesWritten += len;
			updateProgress();
		}
		
		private void updateProgress() {
			if (progressCallback != null) {
				int p = 50;
				if (estimatedSize > 0) {
					p = (int) Math.floor(bytesWritten * 100.0 / estimatedSize);
					p = MathUtil.clamp(p, 0, 100);
				}
				progressCallback.setProgress(p);
			}
		}
	}
}
