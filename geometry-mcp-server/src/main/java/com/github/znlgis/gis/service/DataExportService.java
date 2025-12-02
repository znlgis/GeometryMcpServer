package com.github.znlgis.gis.service;

import com.github.znlgis.gis.model.DataReference;

/**
 * Service for data export operations.
 */
public interface DataExportService {
    
    /**
     * Generates a thumbnail image of the data.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @param width Image width
     * @param height Image height
     * @param style Optional style configuration
     * @return Base64 encoded image or URL
     */
    String generateThumbnail(String sessionId, String dataId, int width, int height, String style);
    
    /**
     * Exports data to a specific format.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @param format Export format (GeoJSON, KML, Shapefile, etc.)
     * @param compression Whether to compress the output
     * @return Export result with download URL and expiration time
     */
    ExportResult exportData(String sessionId, String dataId, String format, boolean compression);
    
    /**
     * Export result containing download information.
     */
    record ExportResult(
        String downloadUrl,
        long expiresIn,
        String format,
        long size
    ) {}
}
