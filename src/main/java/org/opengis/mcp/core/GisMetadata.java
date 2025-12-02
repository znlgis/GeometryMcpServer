package org.opengis.mcp.core;

import java.time.Instant;

/**
 * Metadata for a GIS data object.
 */
public class GisMetadata {
    
    private String id;
    private DataType type;
    private String crs;
    private double[] bounds; // [minX, minY, maxX, maxY]
    private long featureCount;
    private String[] attributeNames;
    private long sizeInBytes;
    private Instant createdAt;
    private Instant expiresAt;
    private String alias;
    
    public GisMetadata() {
        this.createdAt = Instant.now();
    }
    
    public GisMetadata(String id, DataType type) {
        this();
        this.id = id;
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public DataType getType() {
        return type;
    }

    public void setType(DataType type) {
        this.type = type;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public double[] getBounds() {
        return bounds;
    }

    public void setBounds(double[] bounds) {
        this.bounds = bounds;
    }

    public long getFeatureCount() {
        return featureCount;
    }

    public void setFeatureCount(long featureCount) {
        this.featureCount = featureCount;
    }

    public String[] getAttributeNames() {
        return attributeNames;
    }

    public void setAttributeNames(String[] attributeNames) {
        this.attributeNames = attributeNames;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public void setSizeInBytes(long sizeInBytes) {
        this.sizeInBytes = sizeInBytes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    /**
     * Types of GIS data objects.
     */
    public enum DataType {
        GEOMETRY,      // Single geometry (Point, LineString, Polygon, etc.)
        FEATURE,       // Single feature (geometry + attributes)
        FEATURE_COLLECTION,  // Collection of features
        LAYER,         // Full layer with schema and features
        RASTER         // Raster data
    }
}
