package com.github.znlgis.gis.service;

import com.github.znlgis.gis.model.DataReference;

/**
 * Service for coordinate reference system transformations.
 */
public interface CoordinateTransformService {
    
    /**
     * Transforms data from one CRS to another.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @param sourceCrs Source coordinate reference system (e.g., "EPSG:4326")
     * @param targetCrs Target coordinate reference system (e.g., "EPSG:3857")
     * @return Data reference to transformed result
     */
    DataReference transformCrs(String sessionId, String dataId, String sourceCrs, String targetCrs);
    
    /**
     * Reprojects raster data to a new CRS.
     *
     * @param sessionId Session identifier
     * @param rasterId Raster data reference ID
     * @param targetCrs Target coordinate reference system
     * @param resamplingMethod Resampling method (e.g., "nearest", "bilinear", "cubic")
     * @return Data reference to reprojected raster
     */
    DataReference reprojectRaster(String sessionId, String rasterId, String targetCrs, String resamplingMethod);
    
    /**
     * Gets the CRS of a dataset.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @return CRS identifier
     */
    String getCrs(String sessionId, String dataId);
    
    /**
     * Validates if a CRS code is valid.
     *
     * @param crsCode CRS code to validate
     * @return true if valid
     */
    boolean isValidCrs(String crsCode);
}
