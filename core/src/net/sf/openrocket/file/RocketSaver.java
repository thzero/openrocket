package net.sf.openrocket.file;

import net.sf.openrocket.document.OpenRocketDocument;
import net.sf.openrocket.document.StorageOptions;

import java.io.IOException;
import java.io.OutputStream;

public interface RocketSaver {
    void save(OutputStream dest, OpenRocketDocument doc) throws IOException;

    /**
     * Save the document to the specified output stream using the given storage options.
     *
     * @param dest    the destination stream.
     * @param doc     the document to save.
     * @param options the storage options.
     * @throws IOException in case of an I/O error.
     */
    void save(OutputStream dest, OpenRocketDocument doc, StorageOptions options) throws IOException;

    /**
     * Provide an estimate of the file size when saving the document with the
     * specified options.  This is used as an indication to the user and when estimating
     * file save progress.
     *
     * @param doc     the document.
     * @param options the save options, compression must be taken into account.
     * @return the estimated number of bytes the storage would take.
     */
    long estimateFileSize(OpenRocketDocument doc, StorageOptions options);
}
