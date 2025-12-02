package com.github.znlgis.gis.service;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.SpatialPredicate;

/**
 * Service for spatial analysis operations.
 */
public interface SpatialAnalysisService {
    
    /**
     * Creates a buffer around features.
     *
     * @param sessionId Session identifier
     * @param sourceDataId Source data ID
     * @param distance Buffer distance
     * @param units Distance units (meters, kilometers, etc.)
     * @return Data reference to buffered result
     */
    DataReference buffer(String sessionId, String sourceDataId, double distance, String units);
    
    /**
     * Computes intersection of two datasets.
     *
     * @param sessionId Session identifier
     * @param dataIdA First dataset ID
     * @param dataIdB Second dataset ID
     * @return Data reference to intersection result
     */
    DataReference intersect(String sessionId, String dataIdA, String dataIdB);
    
    /**
     * Computes union of multiple datasets.
     *
     * @param sessionId Session identifier
     * @param dataIds Array of dataset IDs
     * @return Data reference to union result
     */
    DataReference union(String sessionId, String... dataIds);
    
    /**
     * Performs spatial query using a geometry and predicate.
     *
     * @param sessionId Session identifier
     * @param dataId Target dataset ID
     * @param wkt Query geometry in WKT format
     * @param predicate Spatial predicate
     * @return Data reference to query result
     */
    DataReference spatialQuery(String sessionId, String dataId, String wkt, SpatialPredicate predicate);
    
    /**
     * Filters features by attribute expression.
     *
     * @param sessionId Session identifier
     * @param dataId Dataset ID
     * @param expression Filter expression (CQL/SQL-like)
     * @return Data reference to filtered result
     */
    DataReference attributeFilter(String sessionId, String dataId, String expression);
    
    /**
     * Computes the difference between two geometries.
     *
     * @param sessionId Session identifier
     * @param dataIdA First dataset ID
     * @param dataIdB Second dataset ID
     * @return Data reference to difference result
     */
    DataReference difference(String sessionId, String dataIdA, String dataIdB);
    
    /**
     * Computes the symmetric difference between two geometries.
     *
     * @param sessionId Session identifier
     * @param dataIdA First dataset ID
     * @param dataIdB Second dataset ID
     * @return Data reference to symmetric difference result
     */
    DataReference symmetricDifference(String sessionId, String dataIdA, String dataIdB);
    
    /**
     * Computes the convex hull of geometries.
     *
     * @param sessionId Session identifier
     * @param dataId Dataset ID
     * @return Data reference to convex hull result
     */
    DataReference convexHull(String sessionId, String dataId);
    
    /**
     * Computes centroids of geometries.
     *
     * @param sessionId Session identifier
     * @param dataId Dataset ID
     * @return Data reference to centroids result
     */
    DataReference centroid(String sessionId, String dataId);
}
