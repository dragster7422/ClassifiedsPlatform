package com.classifiedsplatform.application.port.out;

import java.io.IOException;

public interface FileStoragePort {

    /**
     * Store file and return storage path
     */
    String store(String filename, byte[] fileData) throws IOException;

    /**
     * Delete file by storage path
     */
    void delete(String storagePath) throws IOException;

    /**
     * Check if file exists
     */
    boolean exists(String storagePath);
}