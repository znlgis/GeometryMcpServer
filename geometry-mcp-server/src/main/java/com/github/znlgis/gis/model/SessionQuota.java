package com.github.znlgis.gis.model;

/**
 * Session quota configuration and usage tracking.
 */
public record SessionQuota(
    long maxDataSize,      // Maximum total data size in bytes
    int maxDataCount,      // Maximum number of data references
    long currentDataSize,  // Current total data size in bytes
    int currentDataCount   // Current number of data references
) {
    public static final long DEFAULT_MAX_DATA_SIZE = 10L * 1024 * 1024 * 1024; // 10GB
    public static final int DEFAULT_MAX_DATA_COUNT = 1000;
    
    public static SessionQuota createDefault() {
        return new SessionQuota(DEFAULT_MAX_DATA_SIZE, DEFAULT_MAX_DATA_COUNT, 0, 0);
    }
    
    public SessionQuota withUsage(long dataSize, int dataCount) {
        return new SessionQuota(maxDataSize, maxDataCount, dataSize, dataCount);
    }
    
    public boolean canAddData(long additionalSize) {
        return currentDataSize + additionalSize <= maxDataSize 
            && currentDataCount < maxDataCount;
    }
    
    public double getUsagePercentage() {
        return (double) currentDataSize / maxDataSize * 100;
    }
}
