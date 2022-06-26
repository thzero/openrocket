package net.sf.openrocket.file;

import java.io.IOException;
import java.io.OutputStream;

import net.sf.openrocket.document.OpenRocketDocument;

public abstract class AbstractRocketSaver implements RocketSaver {
	/**
	 * Save the document to the specified output stream using the default storage options.
	 * 
	 * @param dest			the destination stream.
	 * @param doc			the document to save.
	 * @throws IOException	in case of an I/O error.
	 */
	@Override
	public final void save(OutputStream dest, OpenRocketDocument doc) throws IOException {
		save(dest, doc, doc.getDefaultStorageOptions());
	}
}
