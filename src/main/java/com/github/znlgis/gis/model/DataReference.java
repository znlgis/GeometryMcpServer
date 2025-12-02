package com.github.znlgis.gis.model;

import com.github.znlgis.gis.model.enums.DataState;
import com.github.znlgis.gis.model.enums.DataType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data reference model for managing GIS data with metadata and lifecycle.
 * 
 * This entity represents a reference to stored GIS data with associated
 * metadata, session information, and lifecycle management properties.
 */
@Entity
@Table(name = "data_references")
public class DataReference {
    
    @Id
    private String id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType type;
    
    @Column(nullable = false)
    private String format;
    
    @Column(name = "storage_location", nullable = false)
    private String storageLocation;
    
    @Column(name = "bounds_min_x")
    private Double boundsMinX;
    
    @Column(name = "bounds_min_y")
    private Double boundsMinY;
    
    @Column(name = "bounds_max_x")
    private Double boundsMaxX;
    
    @Column(name = "bounds_max_y")
    private Double boundsMaxY;
    
    @Column(nullable = false)
    private String crs = "EPSG:4326";
    
    @Column(name = "feature_count")
    private Long featureCount;
    
    @Column(name = "data_size")
    private Long size;
    
    @Column(name = "attributes_json", length = 4000)
    private String attributesJson;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "reference_count")
    private Integer referenceCount = 0;
    
    @ElementCollection
    @CollectionTable(name = "data_reference_tags", joinColumns = @JoinColumn(name = "data_reference_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataState state = DataState.CREATED;
    
    public DataReference() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.lastAccessedAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60); // 24h default TTL
    }
    
    public DataReference(String sessionId, DataType type, String format, String storageLocation) {
        this();
        this.sessionId = sessionId;
        this.type = type;
        this.format = format;
        this.storageLocation = storageLocation;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public DataType getType() {
        return type;
    }
    
    public void setType(DataType type) {
        this.type = type;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public String getStorageLocation() {
        return storageLocation;
    }
    
    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }
    
    public double[] getBounds() {
        if (boundsMinX == null || boundsMinY == null || boundsMaxX == null || boundsMaxY == null) {
            return new double[]{0, 0, 0, 0};
        }
        return new double[]{boundsMinX, boundsMinY, boundsMaxX, boundsMaxY};
    }
    
    public void setBounds(double[] bounds) {
        if (bounds != null && bounds.length >= 4) {
            this.boundsMinX = bounds[0];
            this.boundsMinY = bounds[1];
            this.boundsMaxX = bounds[2];
            this.boundsMaxY = bounds[3];
        }
    }
    
    public String getCrs() {
        return crs;
    }
    
    public void setCrs(String crs) {
        this.crs = crs;
    }
    
    public Long getFeatureCount() {
        return featureCount;
    }
    
    public void setFeatureCount(Long featureCount) {
        this.featureCount = featureCount;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public String getAttributesJson() {
        return attributesJson;
    }
    
    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Integer getReferenceCount() {
        return referenceCount;
    }
    
    public void setReferenceCount(Integer referenceCount) {
        this.referenceCount = referenceCount;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public DataState getState() {
        return state;
    }
    
    public void setState(DataState state) {
        this.state = state;
    }
    
    /**
     * Updates the last accessed timestamp and extends TTL.
     */
    public void touch() {
        this.lastAccessedAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
    }
    
    /**
     * Increments the reference count.
     */
    public void incrementReferenceCount() {
        this.referenceCount++;
    }
    
    /**
     * Decrements the reference count.
     */
    public void decrementReferenceCount() {
        if (this.referenceCount > 0) {
            this.referenceCount--;
        }
    }
    
    /**
     * Checks if the data has expired.
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
