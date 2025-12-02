package com.github.znlgis.gis.service;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.DataType;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for managing GIS data storage and retrieval.
 */
public interface DataManagementService {
    
    /**
     * Uploads data to the storage system.
     *
     * @param sessionId Session identifier for data isolation
     * @param inputStream Data input stream
     * @param fileName Original file name
     * @param format Data format (e.g., "GeoJSON", "Shapefile")
     * @param type Data type
     * @return Created data reference
     */
    DataReference uploadData(String sessionId, InputStream inputStream, String fileName, String format, DataType type);
    
    /**
     * Uploads data from a URL.
     *
     * @param sessionId Session identifier
     * @param url URL to fetch data from
     * @param format Expected data format
     * @param type Data type
     * @return Created data reference
     */
    DataReference uploadDataFromUrl(String sessionId, String url, String format, DataType type);
    
    /**
     * Uploads GeoJSON data directly.
     *
     * @param sessionId Session identifier
     * @param geoJson GeoJSON string content
     * @return Created data reference
     */
    DataReference uploadGeoJson(String sessionId, String geoJson);
    
    /**
     * Lists all data references for a session.
     *
     * @param sessionId Session identifier
     * @param filters Optional filters
     * @return List of data references
     */
    List<DataReference> listData(String sessionId, Map<String, String> filters);
    
    /**
     * Gets detailed information about a data reference.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @return Data reference details
     */
    Optional<DataReference> describeData(String sessionId, String dataId);
    
    /**
     * Deletes a data reference and its associated storage.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @return true if deleted successfully
     */
    boolean deleteData(String sessionId, String dataId);
    
    /**
     * Fetches actual data content with optional formatting.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @param format Output format (preview/full/summary)
     * @param maxFeatures Maximum number of features to return
     * @return Data content as string
     */
    String fetchResult(String sessionId, String dataId, String format, int maxFeatures);
    
    /**
     * Gets the raw data as InputStream.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @return InputStream of the data
     */
    Optional<InputStream> getDataStream(String sessionId, String dataId);
    
    /**
     * Creates a new data reference for derived data.
     *
     * @param sessionId Session identifier
     * @param geoJson GeoJSON content
     * @param sourceDataId Source data ID for reference tracking
     * @return Created data reference
     */
    DataReference createDerivedData(String sessionId, String geoJson, String sourceDataId);
}
