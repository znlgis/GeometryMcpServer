package org.opengis.mcp.storage;

import org.opengis.mcp.config.GisServerProperties;
import org.opengis.mcp.core.GisDataObject;
import org.opengis.mcp.core.GisMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

/**
 * File storage service for persisting GIS data objects.
 * Handles medium to large data sets that should not be kept in memory.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final GisServerProperties properties;
    private final Path baseDirectory;

    public FileStorageService(GisServerProperties properties) {
        this.properties = properties;
        this.baseDirectory = Paths.get(properties.getStorage().getBaseDirectory());
        
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(baseDirectory);
            log.info("File storage initialized at: {}", baseDirectory);
        } catch (IOException e) {
            log.error("Failed to initialize file storage: {}", e.getMessage());
            throw new RuntimeException("Cannot initialize file storage", e);
        }
    }

    /**
     * Saves data to file storage.
     */
    public boolean save(String id, byte[] data) {
        try {
            Path filePath = getFilePath(id);
            Files.write(filePath, data);
            log.debug("Saved to file storage: {} ({} bytes)", id, data.length);
            return true;
        } catch (IOException e) {
            log.error("Failed to save to file storage: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Loads data from file storage.
     */
    public Optional<byte[]> load(String id) {
        try {
            Path filePath = getFilePath(id);
            if (Files.exists(filePath)) {
                byte[] data = Files.readAllBytes(filePath);
                log.debug("Loaded from file storage: {} ({} bytes)", id, data.length);
                return Optional.of(data);
            }
            return Optional.empty();
        } catch (IOException e) {
            log.error("Failed to load from file storage: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Deletes data from file storage.
     */
    public boolean delete(String id) {
        try {
            Path filePath = getFilePath(id);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.debug("Deleted from file storage: {}", id);
            }
            return deleted;
        } catch (IOException e) {
            log.error("Failed to delete from file storage: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks if a file exists.
     */
    public boolean exists(String id) {
        return Files.exists(getFilePath(id));
    }

    /**
     * Gets file size.
     */
    public long getSize(String id) {
        try {
            Path filePath = getFilePath(id);
            return Files.exists(filePath) ? Files.size(filePath) : 0;
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Validates that a path is within allowed directories.
     */
    public boolean isPathAllowed(String path) {
        try {
            Path normalizedPath = Paths.get(path).toAbsolutePath().normalize();
            return properties.getStorage().getAllowedPaths().stream()
                .anyMatch(allowedPath -> {
                    Path allowed = Paths.get(allowedPath).toAbsolutePath().normalize();
                    return normalizedPath.startsWith(allowed);
                });
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Cleans up expired files.
     * Note: File-based expiration is not currently tracked. 
     * Memory-based objects are managed by SessionManager with TTL.
     * @throws UnsupportedOperationException as file expiration is not implemented
     */
    public int cleanupExpired() {
        // File storage doesn't track expiration - memory objects are managed by SessionManager
        throw new UnsupportedOperationException(
            "File expiration cleanup not implemented. Use SessionManager for TTL-based cleanup.");
    }

    private Path getFilePath(String id) {
        // Sanitize the ID to prevent path traversal
        String sanitizedId = id.replaceAll("[^a-zA-Z0-9_-]", "_");
        return baseDirectory.resolve(sanitizedId + ".gis");
    }
}
