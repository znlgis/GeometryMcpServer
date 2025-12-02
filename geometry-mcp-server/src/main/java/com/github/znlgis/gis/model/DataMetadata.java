package com.github.znlgis.gis.model;

import java.util.Map;

/**
 * Metadata for GIS data including spatial bounds, CRS, feature count, etc.
 */
public record DataMetadata(
    double[] bounds,           // [minX, minY, maxX, maxY] - spatial extent
    String crs,                // Coordinate Reference System (e.g., "EPSG:4326")
    long featureCount,         // Number of features
    long size,                 // Size in bytes
    Map<String, String> attributes  // Attribute name to type mapping
) {
    /**
     * Creates an empty metadata instance.
     */
    public static DataMetadata empty() {
        return new DataMetadata(new double[]{0, 0, 0, 0}, "EPSG:4326", 0, 0, Map.of());
    }
    
    /**
     * Creates a copy of this metadata with updated bounds.
     */
    public DataMetadata withBounds(double[] newBounds) {
        return new DataMetadata(newBounds, crs, featureCount, size, attributes);
    }
    
    /**
     * Creates a copy of this metadata with updated CRS.
     */
    public DataMetadata withCrs(String newCrs) {
        return new DataMetadata(bounds, newCrs, featureCount, size, attributes);
    }
    
    /**
     * Creates a copy of this metadata with updated feature count.
     */
    public DataMetadata withFeatureCount(long newCount) {
        return new DataMetadata(bounds, crs, newCount, size, attributes);
    }
    
    /**
     * Creates a copy of this metadata with updated size.
     */
    public DataMetadata withSize(long newSize) {
        return new DataMetadata(bounds, crs, featureCount, newSize, attributes);
    }
}
