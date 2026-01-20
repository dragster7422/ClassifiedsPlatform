package com.classifiedsplatform.infrastructure.storage;

import com.classifiedsplatform.application.port.out.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

@Component
public class LocalFileStorageAdapter implements FileStoragePort {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageAdapter.class);

    private final Path uploadDir;

    public LocalFileStorageAdapter(@Value("${file-storage.upload-dir}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
        log.info("File storage initialized at: {}", this.uploadDir);
    }

    @Override
    public String store(String filename, byte[] fileData) throws IOException {
        // Generate unique filename to avoid collisions
        String extension = getFileExtension(filename);
        String uniqueFilename = UUID.randomUUID() + extension;
        Path targetLocation = this.uploadDir.resolve(uniqueFilename);

        // Ensure the file is created in the upload directory
        if (!targetLocation.normalize().startsWith(this.uploadDir)) {
            throw new IOException("Cannot store file outside upload directory");
        }

        Files.write(targetLocation, fileData, StandardOpenOption.CREATE_NEW);
        log.debug("File stored: {}", uniqueFilename);

        return uniqueFilename;
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Path fileToDelete = this.uploadDir.resolve(storagePath).normalize();

        // Security check
        if (!fileToDelete.startsWith(this.uploadDir)) {
            throw new IOException("Cannot delete file outside upload directory");
        }

        Files.deleteIfExists(fileToDelete);
        log.debug("File deleted: {}", storagePath);
    }

    @Override
    public boolean exists(String storagePath) {
        Path filePath = this.uploadDir.resolve(storagePath).normalize();
        return Files.exists(filePath);
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex > 0) {
            return filename.substring(lastDotIndex);
        }
        return "";
    }
}