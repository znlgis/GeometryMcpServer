package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.DataState;
import com.github.znlgis.gis.model.enums.DataType;
import com.github.znlgis.gis.repository.DataReferenceRepository;
import com.github.znlgis.gis.service.DataManagementService;
import com.github.znlgis.gis.service.SessionService;
import com.github.znlgis.gis.util.GeoJsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of DataManagementService for GIS data storage and retrieval.
 */
@Service
@Transactional
public class DataManagementServiceImpl implements DataManagementService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataManagementServiceImpl.class);
    private static final long SMALL_DATA_THRESHOLD = 10 * 1024 * 1024; // 10MB
    
    private final DataReferenceRepository dataReferenceRepository;
    private final SessionService sessionService;
    private final Path storageBasePath;
    private final HttpClient httpClient;
    
    public DataManagementServiceImpl(
            DataReferenceRepository dataReferenceRepository,
            SessionService sessionService,
            @Value("${gis.storage.path:./data}") String storagePath) {
        this.dataReferenceRepository = dataReferenceRepository;
        this.sessionService = sessionService;
        this.storageBasePath = Path.of(storagePath);
        this.httpClient = HttpClient.newHttpClient();
        
        // Ensure storage directory exists
        try {
            Files.createDirectories(storageBasePath);
        } catch (IOException e) {
            logger.error("Failed to create storage directory", e);
        }
    }
    
    @Override
    public DataReference uploadData(String sessionId, InputStream inputStream, String fileName, 
                                    String format, DataType type) {
        validateSession(sessionId);
        
        try {
            // Read data
            byte[] data = inputStream.readAllBytes();
            long size = data.length;
            
            // Check quota
            if (!sessionService.getQuota(sessionId).canAddData(size)) {
                throw new IllegalStateException("Session quota exceeded");
            }
            
            // Generate storage location
            String storageLocation = generateStorageLocation(sessionId, format);
            
            // Store data
            Path storagePath = storageBasePath.resolve(storageLocation);
            Files.createDirectories(storagePath.getParent());
            Files.write(storagePath, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Create data reference
            DataReference dataRef = new DataReference(sessionId, type, format, storageLocation);
            dataRef.setSize(size);
            
            // Extract metadata if GeoJSON
            if ("GeoJSON".equalsIgnoreCase(format)) {
                String geoJson = new String(data, StandardCharsets.UTF_8);
                updateMetadataFromGeoJson(dataRef, geoJson);
            }
            
            // Save and update session
            dataRef = dataReferenceRepository.save(dataRef);
            sessionService.addDataToSession(sessionId, dataRef.getId(), size);
            
            logger.info("Uploaded data: {} for session: {}", dataRef.getId(), sessionId);
            return dataRef;
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload data", e);
        }
    }
    
    @Override
    public DataReference uploadDataFromUrl(String sessionId, String url, String format, DataType type) {
        validateSession(sessionId);
        
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();
            
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to fetch data from URL: " + response.statusCode());
            }
            
            return uploadData(sessionId, response.body(), url, format, type);
            
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to fetch data from URL", e);
        }
    }
    
    @Override
    public DataReference uploadGeoJson(String sessionId, String geoJson) {
        validateSession(sessionId);
        
        byte[] data = geoJson.getBytes(StandardCharsets.UTF_8);
        return uploadData(sessionId, new ByteArrayInputStream(data), "data.geojson", "GeoJSON", DataType.FEATURE_COLLECTION);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<DataReference> listData(String sessionId, Map<String, String> filters) {
        validateSession(sessionId);
        
        List<DataReference> refs = dataReferenceRepository.findBySessionId(sessionId);
        
        // Apply filters
        if (filters != null) {
            if (filters.containsKey("type")) {
                DataType filterType = DataType.valueOf(filters.get("type"));
                refs = refs.stream().filter(r -> r.getType() == filterType).toList();
            }
            if (filters.containsKey("format")) {
                String filterFormat = filters.get("format");
                refs = refs.stream().filter(r -> r.getFormat().equalsIgnoreCase(filterFormat)).toList();
            }
        }
        
        return refs;
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<DataReference> describeData(String sessionId, String dataId) {
        validateSession(sessionId);
        return dataReferenceRepository.findByIdAndSessionId(dataId, sessionId);
    }
    
    @Override
    public boolean deleteData(String sessionId, String dataId) {
        validateSession(sessionId);
        
        Optional<DataReference> refOpt = dataReferenceRepository.findByIdAndSessionId(dataId, sessionId);
        if (refOpt.isEmpty()) {
            return false;
        }
        
        DataReference ref = refOpt.get();
        
        // Check if referenced by other data
        if (ref.getReferenceCount() > 0) {
            throw new IllegalStateException("Cannot delete data that is referenced by other data");
        }
        
        // Delete storage
        try {
            Path storagePath = storageBasePath.resolve(ref.getStorageLocation());
            Files.deleteIfExists(storagePath);
        } catch (IOException e) {
            logger.error("Failed to delete storage for data: {}", dataId, e);
        }
        
        // Update session and delete reference
        sessionService.removeDataFromSession(sessionId, dataId, ref.getSize() != null ? ref.getSize() : 0);
        dataReferenceRepository.delete(ref);
        
        logger.info("Deleted data: {} from session: {}", dataId, sessionId);
        return true;
    }
    
    @Override
    @Transactional(readOnly = true)
    public String fetchResult(String sessionId, String dataId, String format, int maxFeatures) {
        validateSession(sessionId);
        
        Optional<DataReference> refOpt = dataReferenceRepository.findByIdAndSessionId(dataId, sessionId);
        if (refOpt.isEmpty()) {
            throw new IllegalArgumentException("Data not found: " + dataId);
        }
        
        DataReference ref = refOpt.get();
        
        try {
            Path storagePath = storageBasePath.resolve(ref.getStorageLocation());
            String content = Files.readString(storagePath, StandardCharsets.UTF_8);
            
            // Format output based on request
            return switch (format.toLowerCase()) {
                case "preview" -> GeoJsonUtils.truncateFeatures(content, maxFeatures);
                case "summary" -> GeoJsonUtils.generateSummary(content, ref);
                case "full" -> content;
                default -> content;
            };
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to read data: " + dataId, e);
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<InputStream> getDataStream(String sessionId, String dataId) {
        validateSession(sessionId);
        
        Optional<DataReference> refOpt = dataReferenceRepository.findByIdAndSessionId(dataId, sessionId);
        if (refOpt.isEmpty()) {
            return Optional.empty();
        }
        
        try {
            Path storagePath = storageBasePath.resolve(refOpt.get().getStorageLocation());
            return Optional.of(Files.newInputStream(storagePath));
        } catch (IOException e) {
            logger.error("Failed to open data stream for: {}", dataId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public DataReference createDerivedData(String sessionId, String geoJson, String sourceDataId) {
        validateSession(sessionId);
        
        // Create the derived data
        DataReference derivedRef = uploadGeoJson(sessionId, geoJson);
        
        // Increment reference count on source
        if (sourceDataId != null) {
            dataReferenceRepository.findById(sourceDataId).ifPresent(sourceRef -> {
                sourceRef.incrementReferenceCount();
                if (sourceRef.getState() == DataState.ACTIVE) {
                    sourceRef.setState(DataState.REFERENCED);
                }
                dataReferenceRepository.save(sourceRef);
            });
        }
        
        derivedRef.setState(DataState.ACTIVE);
        return dataReferenceRepository.save(derivedRef);
    }
    
    /**
     * Scheduled cleanup of expired data.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void cleanupExpiredData() {
        Instant now = Instant.now();
        
        // Find expired data with no references
        List<DataReference> expired = dataReferenceRepository.findExpiredWithNoRefs(DataState.ACTIVE, now);
        expired.addAll(dataReferenceRepository.findExpiredWithNoRefs(DataState.CREATED, now));
        
        for (DataReference ref : expired) {
            ref.setState(DataState.MARKED_FOR_DELETION);
            dataReferenceRepository.save(ref);
            logger.info("Marked for deletion: {}", ref.getId());
        }
        
        // Physical delete after 24 hours
        Instant deleteThreshold = now.minus(24, ChronoUnit.HOURS);
        List<DataReference> toDelete = dataReferenceRepository.findMarkedForDeletionBefore(deleteThreshold);
        
        for (DataReference ref : toDelete) {
            try {
                Path storagePath = storageBasePath.resolve(ref.getStorageLocation());
                Files.deleteIfExists(storagePath);
                dataReferenceRepository.delete(ref);
                logger.info("Physically deleted: {}", ref.getId());
            } catch (IOException e) {
                logger.error("Failed to delete storage for: {}", ref.getId(), e);
            }
        }
    }
    
    private void validateSession(String sessionId) {
        if (!sessionService.validateSession(sessionId)) {
            throw new IllegalArgumentException("Invalid or expired session: " + sessionId);
        }
    }
    
    private String generateStorageLocation(String sessionId, String format) {
        String extension = getExtension(format);
        return sessionId + "/" + UUID.randomUUID() + extension;
    }
    
    private String getExtension(String format) {
        return switch (format.toLowerCase()) {
            case "geojson" -> ".geojson";
            case "shapefile" -> ".shp";
            case "geotiff", "tiff" -> ".tif";
            case "kml" -> ".kml";
            case "gml" -> ".gml";
            default -> ".dat";
        };
    }
    
    private void updateMetadataFromGeoJson(DataReference ref, String geoJson) {
        try {
            double[] bounds = GeoJsonUtils.extractBounds(geoJson);
            ref.setBounds(bounds);
            
            long featureCount = GeoJsonUtils.countFeatures(geoJson);
            ref.setFeatureCount(featureCount);
            
            String crs = GeoJsonUtils.extractCrs(geoJson);
            if (crs != null) {
                ref.setCrs(crs);
            }
        } catch (Exception e) {
            logger.warn("Failed to extract metadata from GeoJSON", e);
        }
    }
}
