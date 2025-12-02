package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.SpatialPredicate;
import com.github.znlgis.gis.service.DataManagementService;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import com.github.znlgis.gis.util.GeoJsonUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of SpatialAnalysisService for spatial operations.
 */
@Service
@Transactional
public class SpatialAnalysisServiceImpl implements SpatialAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpatialAnalysisServiceImpl.class);
    
    private final DataManagementService dataManagementService;
    private final WKTReader wktReader = new WKTReader();
    
    public SpatialAnalysisServiceImpl(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public DataReference buffer(String sessionId, String sourceDataId, double distance, String units) {
        logger.info("Performing buffer analysis: source={}, distance={} {}", sourceDataId, distance, units);
        
        String sourceGeoJson = dataManagementService.fetchResult(sessionId, sourceDataId, "full", Integer.MAX_VALUE);
        
        // Convert distance to degrees if necessary
        double distanceInDegrees = convertToDegreesIfNeeded(distance, units);
        
        // Perform buffer operation
        List<Geometry> sourceGeometries = GeoJsonUtils.parseGeometries(sourceGeoJson);
        List<Geometry> bufferedGeometries = new ArrayList<>();
        
        for (Geometry geom : sourceGeometries) {
            Geometry buffered = geom.buffer(distanceInDegrees);
            bufferedGeometries.add(buffered);
        }
        
        // Convert back to GeoJSON
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(bufferedGeometries);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, sourceDataId);
    }
    
    @Override
    public DataReference intersect(String sessionId, String dataIdA, String dataIdB) {
        logger.info("Performing intersection: {} ∩ {}", dataIdA, dataIdB);
        
        String geoJsonA = dataManagementService.fetchResult(sessionId, dataIdA, "full", Integer.MAX_VALUE);
        String geoJsonB = dataManagementService.fetchResult(sessionId, dataIdB, "full", Integer.MAX_VALUE);
        
        List<Geometry> geometriesA = GeoJsonUtils.parseGeometries(geoJsonA);
        List<Geometry> geometriesB = GeoJsonUtils.parseGeometries(geoJsonB);
        
        List<Geometry> intersections = new ArrayList<>();
        
        for (Geometry geomA : geometriesA) {
            for (Geometry geomB : geometriesB) {
                if (geomA.intersects(geomB)) {
                    Geometry intersection = geomA.intersection(geomB);
                    if (!intersection.isEmpty()) {
                        intersections.add(intersection);
                    }
                }
            }
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(intersections);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataIdA);
    }
    
    @Override
    public DataReference union(String sessionId, String... dataIds) {
        logger.info("Performing union of {} datasets", dataIds.length);
        
        List<Geometry> allGeometries = new ArrayList<>();
        
        for (String dataId : dataIds) {
            String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
            allGeometries.addAll(GeoJsonUtils.parseGeometries(geoJson));
        }
        
        if (allGeometries.isEmpty()) {
            return dataManagementService.createDerivedData(sessionId, 
                GeoJsonUtils.emptyFeatureCollection(), dataIds.length > 0 ? dataIds[0] : null);
        }
        
        // Perform cascaded union
        Geometry unionResult = allGeometries.get(0);
        for (int i = 1; i < allGeometries.size(); i++) {
            unionResult = unionResult.union(allGeometries.get(i));
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(List.of(unionResult));
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, 
            dataIds.length > 0 ? dataIds[0] : null);
    }
    
    @Override
    public DataReference spatialQuery(String sessionId, String dataId, String wkt, SpatialPredicate predicate) {
        logger.info("Performing spatial query: dataId={}, predicate={}", dataId, predicate);
        
        Geometry queryGeometry;
        try {
            queryGeometry = wktReader.read(wkt);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid WKT: " + wkt, e);
        }
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
        
        List<Geometry> matchedGeometries = new ArrayList<>();
        
        for (Geometry geom : geometries) {
            boolean matches = switch (predicate) {
                case INTERSECTS -> geom.intersects(queryGeometry);
                case WITHIN -> geom.within(queryGeometry);
                case CONTAINS -> geom.contains(queryGeometry);
                case CROSSES -> geom.crosses(queryGeometry);
                case TOUCHES -> geom.touches(queryGeometry);
                case OVERLAPS -> geom.overlaps(queryGeometry);
                case DISJOINT -> geom.disjoint(queryGeometry);
                case EQUALS -> geom.equals(queryGeometry);
            };
            
            if (matches) {
                matchedGeometries.add(geom);
            }
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(matchedGeometries);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
    }
    
    @Override
    public DataReference attributeFilter(String sessionId, String dataId, String expression) {
        logger.info("Performing attribute filter: dataId={}, expression={}", dataId, expression);
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        
        // For now, return all features - full CQL parsing would be more complex
        // This is a simplified implementation
        String filteredGeoJson = GeoJsonUtils.filterByExpression(geoJson, expression);
        
        return dataManagementService.createDerivedData(sessionId, filteredGeoJson, dataId);
    }
    
    @Override
    public DataReference difference(String sessionId, String dataIdA, String dataIdB) {
        logger.info("Performing difference: {} - {}", dataIdA, dataIdB);
        
        String geoJsonA = dataManagementService.fetchResult(sessionId, dataIdA, "full", Integer.MAX_VALUE);
        String geoJsonB = dataManagementService.fetchResult(sessionId, dataIdB, "full", Integer.MAX_VALUE);
        
        List<Geometry> geometriesA = GeoJsonUtils.parseGeometries(geoJsonA);
        List<Geometry> geometriesB = GeoJsonUtils.parseGeometries(geoJsonB);
        
        // Create union of B geometries
        Geometry unionB = null;
        for (Geometry geomB : geometriesB) {
            unionB = (unionB == null) ? geomB : unionB.union(geomB);
        }
        
        List<Geometry> differences = new ArrayList<>();
        for (Geometry geomA : geometriesA) {
            Geometry diff = (unionB != null) ? geomA.difference(unionB) : geomA;
            if (!diff.isEmpty()) {
                differences.add(diff);
            }
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(differences);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataIdA);
    }
    
    @Override
    public DataReference symmetricDifference(String sessionId, String dataIdA, String dataIdB) {
        logger.info("Performing symmetric difference: {} △ {}", dataIdA, dataIdB);
        
        String geoJsonA = dataManagementService.fetchResult(sessionId, dataIdA, "full", Integer.MAX_VALUE);
        String geoJsonB = dataManagementService.fetchResult(sessionId, dataIdB, "full", Integer.MAX_VALUE);
        
        List<Geometry> geometriesA = GeoJsonUtils.parseGeometries(geoJsonA);
        List<Geometry> geometriesB = GeoJsonUtils.parseGeometries(geoJsonB);
        
        // Create unions
        Geometry unionA = null;
        for (Geometry geom : geometriesA) {
            unionA = (unionA == null) ? geom : unionA.union(geom);
        }
        
        Geometry unionB = null;
        for (Geometry geom : geometriesB) {
            unionB = (unionB == null) ? geom : unionB.union(geom);
        }
        
        Geometry result;
        if (unionA == null && unionB == null) {
            result = null;
        } else if (unionA == null) {
            result = unionB;
        } else if (unionB == null) {
            result = unionA;
        } else {
            result = unionA.symDifference(unionB);
        }
        
        String resultGeoJson = result != null && !result.isEmpty() 
            ? GeoJsonUtils.toFeatureCollection(List.of(result))
            : GeoJsonUtils.emptyFeatureCollection();
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataIdA);
    }
    
    @Override
    public DataReference convexHull(String sessionId, String dataId) {
        logger.info("Computing convex hull for: {}", dataId);
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
        
        List<Geometry> hulls = new ArrayList<>();
        for (Geometry geom : geometries) {
            hulls.add(geom.convexHull());
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(hulls);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
    }
    
    @Override
    public DataReference centroid(String sessionId, String dataId) {
        logger.info("Computing centroids for: {}", dataId);
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
        
        List<Geometry> centroids = new ArrayList<>();
        for (Geometry geom : geometries) {
            centroids.add(geom.getCentroid());
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(centroids);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
    }
    
    // Distance conversion constants (approximate values at the equator)
    // Note: These are approximations and vary by latitude. For precise conversions,
    // use proper geodetic calculations based on the actual location.
    private static final double METERS_PER_DEGREE = 111320.0;      // 1 degree ≈ 111.32 km at equator
    private static final double KILOMETERS_PER_DEGREE = 111.32;    // 1 degree ≈ 111.32 km at equator
    private static final double MILES_PER_DEGREE = 69.0;           // 1 degree ≈ 69 miles at equator
    private static final double FEET_PER_DEGREE = 364320.0;        // 1 degree ≈ 364,320 feet at equator
    
    /**
     * Converts distance to degrees (approximate conversion for geographic coordinates).
     * Note: This is an approximation that assumes equatorial distances.
     * For more accurate results at different latitudes, use proper geodetic calculations.
     */
    private double convertToDegreesIfNeeded(double distance, String units) {
        return switch (units.toLowerCase()) {
            case "meters", "m" -> distance / METERS_PER_DEGREE;
            case "kilometers", "km" -> distance / KILOMETERS_PER_DEGREE;
            case "miles", "mi" -> distance / MILES_PER_DEGREE;
            case "feet", "ft" -> distance / FEET_PER_DEGREE;
            case "degrees", "deg" -> distance;
            default -> distance; // Assume degrees
        };
    }
}
