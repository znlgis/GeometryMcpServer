package org.opengis.mcp.service;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.locationtech.jts.io.geojson.GeoJsonReader;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.opengis.mcp.core.GisDataObject;
import org.opengis.mcp.core.GisMetadata;
import org.opengis.mcp.core.GisMetadata.DataType;
import org.opengis.mcp.core.IdGenerator;
import org.opengis.mcp.core.ToolResult;
import org.opengis.mcp.data.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Core GIS service providing geometry operations and spatial analysis.
 */
@Service
public class GisService {

    private static final Logger log = LoggerFactory.getLogger(GisService.class);

    private final SessionManager sessionManager;
    private final IdGenerator idGenerator;
    private final GeometryFactory geometryFactory;
    private final WKTReader wktReader;
    private final WKTWriter wktWriter;
    private final GeoJsonReader geoJsonReader;
    private final GeoJsonWriter geoJsonWriter;

    public GisService(SessionManager sessionManager, IdGenerator idGenerator) {
        this.sessionManager = sessionManager;
        this.idGenerator = idGenerator;
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
        this.wktReader = new WKTReader(geometryFactory);
        this.wktWriter = new WKTWriter();
        this.geoJsonReader = new GeoJsonReader(geometryFactory);
        this.geoJsonWriter = new GeoJsonWriter();
        this.geoJsonWriter.setEncodeCRS(false);
    }

    // ==================== Data Loading Tools ====================

    /**
     * Loads a geometry from WKT format.
     */
    public ToolResult<String> loadWktGeometry(String wkt) {
        return loadWktGeometry(wkt, "EPSG:4326");
    }

    /**
     * Loads a geometry from WKT format with specified CRS.
     */
    public ToolResult<String> loadWktGeometry(String wkt, String crs) {
        try {
            Geometry geometry = wktReader.read(wkt);
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(crs);
            metadata.setBounds(extractBounds(geometry));
            metadata.setFeatureCount(1);
            
            GisDataObject dataObject = new GisDataObject(id, metadata, geometry);
            sessionManager.store(dataObject);
            
            log.debug("Loaded WKT geometry: {} (type: {})", id, geometry.getGeometryType());
            return ToolResult.success(id, "Loaded geometry: " + geometry.getGeometryType());
            
        } catch (ParseException e) {
            log.error("Failed to parse WKT: {}", e.getMessage());
            return ToolResult.error("Invalid WKT format: " + e.getMessage(), "PARSE_ERROR");
        }
    }

    /**
     * Loads a geometry from GeoJSON format.
     */
    public ToolResult<String> loadGeoJson(String geoJson) {
        return loadGeoJson(geoJson, "EPSG:4326");
    }

    /**
     * Loads a geometry from GeoJSON format with specified CRS.
     */
    public ToolResult<String> loadGeoJson(String geoJson, String crs) {
        try {
            Geometry geometry = geoJsonReader.read(geoJson);
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(crs);
            metadata.setBounds(extractBounds(geometry));
            metadata.setFeatureCount(geometry.getNumGeometries());
            
            GisDataObject dataObject = new GisDataObject(id, metadata, geometry);
            sessionManager.store(dataObject);
            
            log.debug("Loaded GeoJSON geometry: {} (type: {})", id, geometry.getGeometryType());
            return ToolResult.success(id, "Loaded geometry: " + geometry.getGeometryType());
            
        } catch (ParseException e) {
            log.error("Failed to parse GeoJSON: {}", e.getMessage());
            return ToolResult.error("Invalid GeoJSON format: " + e.getMessage(), "PARSE_ERROR");
        }
    }

    // ==================== Spatial Analysis Tools ====================

    /**
     * Creates a buffer around a geometry.
     */
    public ToolResult<String> bufferGeometry(String geometryId, double distance) {
        return getGeometry(geometryId).map(geometry -> {
            Geometry buffered = geometry.buffer(distance);
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(getMetadata(geometryId).map(GisMetadata::getCrs).orElse("EPSG:4326"));
            metadata.setBounds(extractBounds(buffered));
            metadata.setFeatureCount(1);
            
            GisDataObject dataObject = new GisDataObject(id, metadata, buffered);
            sessionManager.store(dataObject);
            
            log.debug("Created buffer: {} (distance: {})", id, distance);
            return ToolResult.success(id, "Created buffer with distance " + distance);
            
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Computes the intersection of two geometries.
     */
    public ToolResult<String> intersect(String geometryId1, String geometryId2) {
        return getGeometry(geometryId1).flatMap(geom1 -> 
            getGeometry(geometryId2).map(geom2 -> {
                Geometry result = geom1.intersection(geom2);
                
                if (result.isEmpty()) {
                    return ToolResult.<String>success(null, "Geometries do not intersect");
                }
                
                String id = idGenerator.generateGeometryId();
                GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
                metadata.setCrs(getMetadata(geometryId1).map(GisMetadata::getCrs).orElse("EPSG:4326"));
                metadata.setBounds(extractBounds(result));
                metadata.setFeatureCount(result.getNumGeometries());
                
                GisDataObject dataObject = new GisDataObject(id, metadata, result);
                sessionManager.store(dataObject);
                
                log.debug("Created intersection: {}", id);
                return ToolResult.success(id, "Created intersection geometry");
            })
        ).orElse(ToolResult.error("One or both geometries not found", "NOT_FOUND"));
    }

    /**
     * Computes the union of two geometries.
     */
    public ToolResult<String> union(String geometryId1, String geometryId2) {
        return getGeometry(geometryId1).flatMap(geom1 -> 
            getGeometry(geometryId2).map(geom2 -> {
                Geometry result = geom1.union(geom2);
                
                String id = idGenerator.generateGeometryId();
                GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
                metadata.setCrs(getMetadata(geometryId1).map(GisMetadata::getCrs).orElse("EPSG:4326"));
                metadata.setBounds(extractBounds(result));
                metadata.setFeatureCount(result.getNumGeometries());
                
                GisDataObject dataObject = new GisDataObject(id, metadata, result);
                sessionManager.store(dataObject);
                
                log.debug("Created union: {}", id);
                return ToolResult.success(id, "Created union geometry");
            })
        ).orElse(ToolResult.error("One or both geometries not found", "NOT_FOUND"));
    }

    /**
     * Computes the difference of two geometries.
     */
    public ToolResult<String> difference(String geometryId1, String geometryId2) {
        return getGeometry(geometryId1).flatMap(geom1 -> 
            getGeometry(geometryId2).map(geom2 -> {
                Geometry result = geom1.difference(geom2);
                
                String id = idGenerator.generateGeometryId();
                GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
                metadata.setCrs(getMetadata(geometryId1).map(GisMetadata::getCrs).orElse("EPSG:4326"));
                metadata.setBounds(extractBounds(result));
                metadata.setFeatureCount(result.getNumGeometries());
                
                GisDataObject dataObject = new GisDataObject(id, metadata, result);
                sessionManager.store(dataObject);
                
                log.debug("Created difference: {}", id);
                return ToolResult.success(id, "Created difference geometry");
            })
        ).orElse(ToolResult.error("One or both geometries not found", "NOT_FOUND"));
    }

    /**
     * Computes the convex hull of a geometry.
     */
    public ToolResult<String> convexHull(String geometryId) {
        return getGeometry(geometryId).map(geometry -> {
            Geometry hull = geometry.convexHull();
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(getMetadata(geometryId).map(GisMetadata::getCrs).orElse("EPSG:4326"));
            metadata.setBounds(extractBounds(hull));
            metadata.setFeatureCount(1);
            
            GisDataObject dataObject = new GisDataObject(id, metadata, hull);
            sessionManager.store(dataObject);
            
            log.debug("Created convex hull: {}", id);
            return ToolResult.success(id, "Created convex hull");
            
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Computes the centroid of a geometry.
     */
    public ToolResult<String> centroid(String geometryId) {
        return getGeometry(geometryId).map(geometry -> {
            Point centroid = geometry.getCentroid();
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(getMetadata(geometryId).map(GisMetadata::getCrs).orElse("EPSG:4326"));
            metadata.setBounds(extractBounds(centroid));
            metadata.setFeatureCount(1);
            
            GisDataObject dataObject = new GisDataObject(id, metadata, centroid);
            sessionManager.store(dataObject);
            
            log.debug("Created centroid: {}", id);
            return ToolResult.success(id, "Created centroid: (" + 
                centroid.getX() + ", " + centroid.getY() + ")");
            
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    // ==================== Projection Tools ====================

    /**
     * Transforms geometry from one CRS to another.
     * Note: This is a simplified implementation that just updates metadata.
     * For proper CRS transformation, GeoTools or similar library would be needed.
     */
    public ToolResult<String> transformCrs(String geometryId, String targetCrs) {
        return sessionManager.get(geometryId).map(dataObject -> {
            Geometry geometry = dataObject.getAsGeometry();
            String sourceCrs = dataObject.getMetadata().getCrs();
            
            if (sourceCrs == null) {
                sourceCrs = "EPSG:4326";
            }
            
            // Note: This is a simplified implementation
            // Real CRS transformation would require a projection library
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(targetCrs);
            metadata.setBounds(extractBounds(geometry));
            metadata.setFeatureCount(geometry.getNumGeometries());
            
            // Clone the geometry (no actual transformation without GeoTools)
            Geometry cloned = geometry.copy();
            
            GisDataObject newDataObject = new GisDataObject(id, metadata, cloned);
            sessionManager.store(newDataObject);
            
            log.debug("Updated CRS metadata: {} -> {} (CRS: {} -> {})", 
                geometryId, id, sourceCrs, targetCrs);
            return ToolResult.success(id, "Updated CRS to " + targetCrs + 
                " (Note: Actual coordinate transformation requires additional libraries)");
            
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Simplifies geometry using Douglas-Peucker algorithm.
     */
    public ToolResult<String> simplifyGeometry(String geometryId, double tolerance) {
        return getGeometry(geometryId).map(geometry -> {
            Geometry simplified = DouglasPeuckerSimplifier.simplify(geometry, tolerance);
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(getMetadata(geometryId).map(GisMetadata::getCrs).orElse("EPSG:4326"));
            metadata.setBounds(extractBounds(simplified));
            metadata.setFeatureCount(simplified.getNumGeometries());
            
            GisDataObject dataObject = new GisDataObject(id, metadata, simplified);
            sessionManager.store(dataObject);
            
            int originalPoints = geometry.getNumPoints();
            int simplifiedPoints = simplified.getNumPoints();
            
            log.debug("Simplified geometry: {} -> {} (points: {} -> {})", 
                geometryId, id, originalPoints, simplifiedPoints);
            return ToolResult.success(id, "Simplified geometry (points: " + 
                originalPoints + " -> " + simplifiedPoints + ")");
            
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    // ==================== Query Tools ====================

    /**
     * Gets metadata for a data object.
     */
    public ToolResult<Map<String, Object>> getMetadataResult(String dataId) {
        return getMetadata(dataId).map(metadata -> {
            Map<String, Object> result = new HashMap<>();
            result.put("id", metadata.getId());
            result.put("type", metadata.getType().name());
            result.put("crs", metadata.getCrs());
            result.put("bounds", metadata.getBounds());
            result.put("featureCount", metadata.getFeatureCount());
            result.put("attributeNames", metadata.getAttributeNames());
            result.put("sizeInBytes", metadata.getSizeInBytes());
            result.put("createdAt", metadata.getCreatedAt().toString());
            result.put("expiresAt", metadata.getExpiresAt() != null ? 
                metadata.getExpiresAt().toString() : null);
            result.put("alias", metadata.getAlias());
            
            return ToolResult.success(result);
        }).orElse(ToolResult.error("Data object not found: " + dataId, "NOT_FOUND"));
    }

    /**
     * Queries geometries by bounding box.
     */
    public ToolResult<String> queryByBbox(String geometryId, double minX, double minY, 
                                          double maxX, double maxY) {
        return getGeometry(geometryId).map(geometry -> {
            Envelope bbox = new Envelope(minX, maxX, minY, maxY);
            Geometry bboxGeom = geometryFactory.toGeometry(bbox);
            
            if (!geometry.intersects(bboxGeom)) {
                return ToolResult.<String>success(null, "Geometry does not intersect with bbox");
            }
            
            Geometry clipped = geometry.intersection(bboxGeom);
            
            String id = idGenerator.generateGeometryId();
            GisMetadata metadata = new GisMetadata(id, DataType.GEOMETRY);
            metadata.setCrs(getMetadata(geometryId).map(GisMetadata::getCrs).orElse("EPSG:4326"));
            metadata.setBounds(extractBounds(clipped));
            metadata.setFeatureCount(clipped.getNumGeometries());
            
            GisDataObject dataObject = new GisDataObject(id, metadata, clipped);
            sessionManager.store(dataObject);
            
            log.debug("Created bbox query result: {}", id);
            return ToolResult.success(id, "Query returned " + clipped.getNumGeometries() + " geometries");
            
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Checks spatial relationship between geometries.
     */
    public ToolResult<Boolean> checkSpatialRelation(String geometryId1, String geometryId2, 
                                                    String relation) {
        return getGeometry(geometryId1).flatMap(geom1 -> 
            getGeometry(geometryId2).map(geom2 -> {
                boolean result = switch (relation.toLowerCase()) {
                    case "intersects" -> geom1.intersects(geom2);
                    case "contains" -> geom1.contains(geom2);
                    case "within" -> geom1.within(geom2);
                    case "touches" -> geom1.touches(geom2);
                    case "crosses" -> geom1.crosses(geom2);
                    case "overlaps" -> geom1.overlaps(geom2);
                    case "disjoint" -> geom1.disjoint(geom2);
                    case "equals" -> geom1.equals(geom2);
                    default -> throw new IllegalArgumentException("Unknown relation: " + relation);
                };
                
                return ToolResult.success(result, geometryId1 + " " + relation + " " + 
                    geometryId2 + ": " + result);
            })
        ).orElse(ToolResult.error("One or both geometries not found", "NOT_FOUND"));
    }

    /**
     * Calculates distance between two geometries.
     */
    public ToolResult<Double> calculateDistance(String geometryId1, String geometryId2) {
        return getGeometry(geometryId1).flatMap(geom1 -> 
            getGeometry(geometryId2).map(geom2 -> {
                double distance = geom1.distance(geom2);
                return ToolResult.success(distance, "Distance: " + distance);
            })
        ).orElse(ToolResult.error("One or both geometries not found", "NOT_FOUND"));
    }

    /**
     * Calculates area of a geometry.
     */
    public ToolResult<Double> calculateArea(String geometryId) {
        return getGeometry(geometryId).map(geometry -> {
            double area = geometry.getArea();
            return ToolResult.success(area, "Area: " + area);
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Calculates length of a geometry.
     */
    public ToolResult<Double> calculateLength(String geometryId) {
        return getGeometry(geometryId).map(geometry -> {
            double length = geometry.getLength();
            return ToolResult.success(length, "Length: " + length);
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    // ==================== Export Tools ====================

    /**
     * Exports geometry to GeoJSON.
     */
    public ToolResult<String> exportToGeoJson(String geometryId) {
        return getGeometry(geometryId).map(geometry -> {
            String geoJson = geoJsonWriter.write(geometry);
            return ToolResult.success(geoJson, "Exported to GeoJSON");
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Exports geometry to WKT.
     */
    public ToolResult<String> exportToWkt(String geometryId) {
        return getGeometry(geometryId).map(geometry -> {
            String wkt = wktWriter.write(geometry);
            return ToolResult.success(wkt, "Exported to WKT");
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    /**
     * Gets a summary of a geometry.
     */
    public ToolResult<Map<String, Object>> exportSummary(String geometryId) {
        return sessionManager.get(geometryId).map(dataObject -> {
            Geometry geometry = dataObject.getAsGeometry();
            GisMetadata metadata = dataObject.getMetadata();
            
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", geometryId);
            summary.put("type", geometry.getGeometryType());
            summary.put("crs", metadata.getCrs());
            summary.put("bounds", metadata.getBounds());
            summary.put("numPoints", geometry.getNumPoints());
            summary.put("numGeometries", geometry.getNumGeometries());
            summary.put("area", geometry.getArea());
            summary.put("length", geometry.getLength());
            summary.put("isValid", geometry.isValid());
            summary.put("isEmpty", geometry.isEmpty());
            
            Point centroid = geometry.getCentroid();
            summary.put("centroid", new double[]{centroid.getX(), centroid.getY()});
            
            return ToolResult.success(summary);
        }).orElse(ToolResult.error("Geometry not found: " + geometryId, "NOT_FOUND"));
    }

    // ==================== Helper Methods ====================

    private java.util.Optional<Geometry> getGeometry(String geometryId) {
        return sessionManager.get(geometryId).map(GisDataObject::getAsGeometry);
    }

    private java.util.Optional<GisMetadata> getMetadata(String geometryId) {
        return sessionManager.get(geometryId).map(GisDataObject::getMetadata);
    }

    private double[] extractBounds(Geometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        Envelope env = geometry.getEnvelopeInternal();
        return new double[]{env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY()};
    }
}
