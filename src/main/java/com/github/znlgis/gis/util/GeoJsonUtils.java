package com.github.znlgis.gis.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.znlgis.gis.model.DataReference;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for GeoJSON parsing and manipulation.
 */
public final class GeoJsonUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(GeoJsonUtils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    private static final WKTWriter wktWriter = new WKTWriter();
    
    private GeoJsonUtils() {
        // Utility class
    }
    
    /**
     * Parses geometries from GeoJSON string.
     */
    public static List<Geometry> parseGeometries(String geoJson) {
        List<Geometry> geometries = new ArrayList<>();
        
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.has("type") ? root.get("type").asText() : "";
            
            if ("FeatureCollection".equals(type)) {
                JsonNode features = root.get("features");
                if (features != null && features.isArray()) {
                    for (JsonNode feature : features) {
                        JsonNode geometry = feature.get("geometry");
                        if (geometry != null) {
                            Geometry geom = parseGeometry(geometry);
                            if (geom != null) {
                                geometries.add(geom);
                            }
                        }
                    }
                }
            } else if ("Feature".equals(type)) {
                JsonNode geometry = root.get("geometry");
                if (geometry != null) {
                    Geometry geom = parseGeometry(geometry);
                    if (geom != null) {
                        geometries.add(geom);
                    }
                }
            } else {
                // Assume it's a geometry object
                Geometry geom = parseGeometry(root);
                if (geom != null) {
                    geometries.add(geom);
                }
            }
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse GeoJSON", e);
        }
        
        return geometries;
    }
    
    /**
     * Parses a single geometry from a JSON node.
     */
    public static Geometry parseGeometry(JsonNode geometryNode) {
        if (geometryNode == null || geometryNode.isNull()) {
            return null;
        }
        
        String type = geometryNode.has("type") ? geometryNode.get("type").asText() : "";
        JsonNode coordinates = geometryNode.get("coordinates");
        
        return switch (type) {
            case "Point" -> parsePoint(coordinates);
            case "MultiPoint" -> parseMultiPoint(coordinates);
            case "LineString" -> parseLineString(coordinates);
            case "MultiLineString" -> parseMultiLineString(coordinates);
            case "Polygon" -> parsePolygon(coordinates);
            case "MultiPolygon" -> parseMultiPolygon(coordinates);
            case "GeometryCollection" -> parseGeometryCollection(geometryNode.get("geometries"));
            default -> null;
        };
    }
    
    private static Point parsePoint(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray() || coordinates.size() < 2) {
            return null;
        }
        double x = coordinates.get(0).asDouble();
        double y = coordinates.get(1).asDouble();
        return geometryFactory.createPoint(new Coordinate(x, y));
    }
    
    private static MultiPoint parseMultiPoint(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray()) {
            return null;
        }
        List<Point> points = new ArrayList<>();
        for (JsonNode coord : coordinates) {
            Point point = parsePoint(coord);
            if (point != null) {
                points.add(point);
            }
        }
        return geometryFactory.createMultiPoint(points.toArray(new Point[0]));
    }
    
    private static LineString parseLineString(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray()) {
            return null;
        }
        List<Coordinate> coords = new ArrayList<>();
        for (JsonNode coord : coordinates) {
            if (coord.isArray() && coord.size() >= 2) {
                coords.add(new Coordinate(coord.get(0).asDouble(), coord.get(1).asDouble()));
            }
        }
        return geometryFactory.createLineString(coords.toArray(new Coordinate[0]));
    }
    
    private static MultiLineString parseMultiLineString(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray()) {
            return null;
        }
        List<LineString> lines = new ArrayList<>();
        for (JsonNode lineCoords : coordinates) {
            LineString line = parseLineString(lineCoords);
            if (line != null) {
                lines.add(line);
            }
        }
        return geometryFactory.createMultiLineString(lines.toArray(new LineString[0]));
    }
    
    private static Polygon parsePolygon(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray() || coordinates.size() == 0) {
            return null;
        }
        
        // Parse exterior ring
        LinearRing shell = parseLinearRing(coordinates.get(0));
        if (shell == null) {
            return null;
        }
        
        // Parse interior rings (holes)
        List<LinearRing> holes = new ArrayList<>();
        for (int i = 1; i < coordinates.size(); i++) {
            LinearRing hole = parseLinearRing(coordinates.get(i));
            if (hole != null) {
                holes.add(hole);
            }
        }
        
        return geometryFactory.createPolygon(shell, holes.toArray(new LinearRing[0]));
    }
    
    private static LinearRing parseLinearRing(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray()) {
            return null;
        }
        List<Coordinate> coords = new ArrayList<>();
        for (JsonNode coord : coordinates) {
            if (coord.isArray() && coord.size() >= 2) {
                coords.add(new Coordinate(coord.get(0).asDouble(), coord.get(1).asDouble()));
            }
        }
        // Ensure ring is closed
        if (!coords.isEmpty() && !coords.get(0).equals(coords.get(coords.size() - 1))) {
            coords.add(coords.get(0));
        }
        return geometryFactory.createLinearRing(coords.toArray(new Coordinate[0]));
    }
    
    private static MultiPolygon parseMultiPolygon(JsonNode coordinates) {
        if (coordinates == null || !coordinates.isArray()) {
            return null;
        }
        List<Polygon> polygons = new ArrayList<>();
        for (JsonNode polyCoords : coordinates) {
            Polygon polygon = parsePolygon(polyCoords);
            if (polygon != null) {
                polygons.add(polygon);
            }
        }
        return geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }
    
    private static GeometryCollection parseGeometryCollection(JsonNode geometries) {
        if (geometries == null || !geometries.isArray()) {
            return geometryFactory.createGeometryCollection();
        }
        List<Geometry> geomList = new ArrayList<>();
        for (JsonNode geomNode : geometries) {
            Geometry geom = parseGeometry(geomNode);
            if (geom != null) {
                geomList.add(geom);
            }
        }
        return geometryFactory.createGeometryCollection(geomList.toArray(new Geometry[0]));
    }
    
    /**
     * Converts a list of geometries to a GeoJSON FeatureCollection.
     */
    public static String toFeatureCollection(List<Geometry> geometries) {
        return toFeatureCollectionWithCrs(geometries, null);
    }
    
    /**
     * Converts a list of geometries to a GeoJSON FeatureCollection with CRS.
     */
    public static String toFeatureCollectionWithCrs(List<Geometry> geometries, String crs) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", "FeatureCollection");
        
        if (crs != null) {
            ObjectNode crsNode = objectMapper.createObjectNode();
            crsNode.put("type", "name");
            ObjectNode propsNode = objectMapper.createObjectNode();
            propsNode.put("name", crs);
            crsNode.set("properties", propsNode);
            root.set("crs", crsNode);
        }
        
        ArrayNode features = objectMapper.createArrayNode();
        
        for (int i = 0; i < geometries.size(); i++) {
            Geometry geom = geometries.get(i);
            ObjectNode feature = objectMapper.createObjectNode();
            feature.put("type", "Feature");
            feature.put("id", i);
            feature.set("properties", objectMapper.createObjectNode());
            feature.set("geometry", geometryToJson(geom));
            features.add(feature);
        }
        
        root.set("features", features);
        
        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize GeoJSON", e);
            return emptyFeatureCollection();
        }
    }
    
    /**
     * Converts a JTS Geometry to a GeoJSON geometry object.
     */
    public static ObjectNode geometryToJson(Geometry geometry) {
        ObjectNode geomNode = objectMapper.createObjectNode();
        
        if (geometry instanceof Point) {
            geomNode.put("type", "Point");
            geomNode.set("coordinates", pointCoordinates((Point) geometry));
        } else if (geometry instanceof MultiPoint) {
            geomNode.put("type", "MultiPoint");
            geomNode.set("coordinates", multiPointCoordinates((MultiPoint) geometry));
        } else if (geometry instanceof LineString) {
            geomNode.put("type", "LineString");
            geomNode.set("coordinates", lineStringCoordinates((LineString) geometry));
        } else if (geometry instanceof MultiLineString) {
            geomNode.put("type", "MultiLineString");
            geomNode.set("coordinates", multiLineStringCoordinates((MultiLineString) geometry));
        } else if (geometry instanceof Polygon) {
            geomNode.put("type", "Polygon");
            geomNode.set("coordinates", polygonCoordinates((Polygon) geometry));
        } else if (geometry instanceof MultiPolygon) {
            geomNode.put("type", "MultiPolygon");
            geomNode.set("coordinates", multiPolygonCoordinates((MultiPolygon) geometry));
        } else if (geometry instanceof GeometryCollection) {
            geomNode.put("type", "GeometryCollection");
            ArrayNode geoms = objectMapper.createArrayNode();
            for (int i = 0; i < geometry.getNumGeometries(); i++) {
                geoms.add(geometryToJson(geometry.getGeometryN(i)));
            }
            geomNode.set("geometries", geoms);
        }
        
        return geomNode;
    }
    
    private static ArrayNode pointCoordinates(Point point) {
        ArrayNode coords = objectMapper.createArrayNode();
        coords.add(point.getX());
        coords.add(point.getY());
        return coords;
    }
    
    private static ArrayNode multiPointCoordinates(MultiPoint multiPoint) {
        ArrayNode coords = objectMapper.createArrayNode();
        for (int i = 0; i < multiPoint.getNumGeometries(); i++) {
            coords.add(pointCoordinates((Point) multiPoint.getGeometryN(i)));
        }
        return coords;
    }
    
    private static ArrayNode lineStringCoordinates(LineString lineString) {
        ArrayNode coords = objectMapper.createArrayNode();
        for (Coordinate coord : lineString.getCoordinates()) {
            ArrayNode point = objectMapper.createArrayNode();
            point.add(coord.x);
            point.add(coord.y);
            coords.add(point);
        }
        return coords;
    }
    
    private static ArrayNode multiLineStringCoordinates(MultiLineString multiLineString) {
        ArrayNode coords = objectMapper.createArrayNode();
        for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
            coords.add(lineStringCoordinates((LineString) multiLineString.getGeometryN(i)));
        }
        return coords;
    }
    
    private static ArrayNode polygonCoordinates(Polygon polygon) {
        ArrayNode coords = objectMapper.createArrayNode();
        coords.add(linearRingCoordinates(polygon.getExteriorRing()));
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            coords.add(linearRingCoordinates(polygon.getInteriorRingN(i)));
        }
        return coords;
    }
    
    private static ArrayNode linearRingCoordinates(LineString ring) {
        return lineStringCoordinates(ring);
    }
    
    private static ArrayNode multiPolygonCoordinates(MultiPolygon multiPolygon) {
        ArrayNode coords = objectMapper.createArrayNode();
        for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
            coords.add(polygonCoordinates((Polygon) multiPolygon.getGeometryN(i)));
        }
        return coords;
    }
    
    /**
     * Returns an empty FeatureCollection.
     */
    public static String emptyFeatureCollection() {
        return "{\"type\":\"FeatureCollection\",\"features\":[]}";
    }
    
    /**
     * Extracts bounds from GeoJSON.
     */
    public static double[] extractBounds(String geoJson) {
        List<Geometry> geometries = parseGeometries(geoJson);
        if (geometries.isEmpty()) {
            return new double[]{0, 0, 0, 0};
        }
        
        Envelope envelope = new Envelope();
        for (Geometry geom : geometries) {
            envelope.expandToInclude(geom.getEnvelopeInternal());
        }
        
        return new double[]{envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()};
    }
    
    /**
     * Counts features in GeoJSON.
     */
    public static long countFeatures(String geoJson) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.has("type") ? root.get("type").asText() : "";
            
            if ("FeatureCollection".equals(type)) {
                JsonNode features = root.get("features");
                return features != null ? features.size() : 0;
            } else if ("Feature".equals(type)) {
                return 1;
            }
            return 1;
        } catch (JsonProcessingException e) {
            return 0;
        }
    }
    
    /**
     * Extracts CRS from GeoJSON.
     */
    public static String extractCrs(String geoJson) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            JsonNode crs = root.get("crs");
            if (crs != null) {
                JsonNode properties = crs.get("properties");
                if (properties != null && properties.has("name")) {
                    return properties.get("name").asText();
                }
            }
        } catch (JsonProcessingException e) {
            // Ignore
        }
        return null;
    }
    
    /**
     * Truncates features to a maximum count.
     */
    public static String truncateFeatures(String geoJson, int maxFeatures) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.has("type") ? root.get("type").asText() : "";
            
            if (!"FeatureCollection".equals(type)) {
                return geoJson;
            }
            
            JsonNode features = root.get("features");
            if (features == null || features.size() <= maxFeatures) {
                return geoJson;
            }
            
            ObjectNode result = (ObjectNode) root.deepCopy();
            ArrayNode truncatedFeatures = objectMapper.createArrayNode();
            
            for (int i = 0; i < Math.min(features.size(), maxFeatures); i++) {
                truncatedFeatures.add(features.get(i));
            }
            
            result.set("features", truncatedFeatures);
            result.put("truncated", true);
            result.put("total_count", features.size());
            
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return geoJson;
        }
    }
    
    /**
     * Generates a summary of GeoJSON data.
     */
    public static String generateSummary(String geoJson, DataReference ref) {
        ObjectNode summary = objectMapper.createObjectNode();
        
        summary.put("data_id", ref.getId());
        summary.put("type", ref.getType().toString());
        summary.put("format", ref.getFormat());
        summary.put("feature_count", ref.getFeatureCount() != null ? ref.getFeatureCount() : countFeatures(geoJson));
        summary.put("size_bytes", ref.getSize());
        summary.put("crs", ref.getCrs());
        
        double[] bounds = ref.getBounds();
        if (bounds != null && bounds.length >= 4) {
            ObjectNode boundsNode = objectMapper.createObjectNode();
            boundsNode.put("minX", bounds[0]);
            boundsNode.put("minY", bounds[1]);
            boundsNode.put("maxX", bounds[2]);
            boundsNode.put("maxY", bounds[3]);
            summary.set("bounds", boundsNode);
        }
        
        try {
            return objectMapper.writeValueAsString(summary);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
    
    /**
     * Filters features by a simple expression.
     * This is a simplified implementation supporting basic comparisons.
     */
    public static String filterByExpression(String geoJson, String expression) {
        try {
            JsonNode root = objectMapper.readTree(geoJson);
            String type = root.has("type") ? root.get("type").asText() : "";
            
            if (!"FeatureCollection".equals(type)) {
                return geoJson;
            }
            
            JsonNode features = root.get("features");
            if (features == null) {
                return geoJson;
            }
            
            ArrayNode filteredFeatures = objectMapper.createArrayNode();
            
            // Parse simple expression (e.g., "property = 'value'" or "property > 10")
            String[] parts = parseExpression(expression);
            if (parts == null) {
                return geoJson;
            }
            
            String property = parts[0];
            String operator = parts[1];
            String value = parts[2];
            
            for (JsonNode feature : features) {
                JsonNode properties = feature.get("properties");
                if (properties != null && properties.has(property)) {
                    JsonNode propValue = properties.get(property);
                    if (matchesExpression(propValue, operator, value)) {
                        filteredFeatures.add(feature);
                    }
                }
            }
            
            ObjectNode result = (ObjectNode) root.deepCopy();
            result.set("features", filteredFeatures);
            
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return geoJson;
        }
    }
    
    private static String[] parseExpression(String expression) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        
        String[] operators = {">=", "<=", "!=", "=", ">", "<", "LIKE"};
        for (String op : operators) {
            int index = expression.indexOf(op);
            if (index > 0) {
                String property = expression.substring(0, index).trim();
                String value = expression.substring(index + op.length()).trim();
                // Remove quotes if present
                if (value.startsWith("'") && value.endsWith("'")) {
                    value = value.substring(1, value.length() - 1);
                }
                return new String[]{property, op, value};
            }
        }
        return null;
    }
    
    private static boolean matchesExpression(JsonNode propValue, String operator, String value) {
        if (propValue.isNumber()) {
            double numValue = propValue.asDouble();
            double compareValue = Double.parseDouble(value);
            return switch (operator) {
                case "=" -> numValue == compareValue;
                case "!=" -> numValue != compareValue;
                case ">" -> numValue > compareValue;
                case "<" -> numValue < compareValue;
                case ">=" -> numValue >= compareValue;
                case "<=" -> numValue <= compareValue;
                default -> false;
            };
        } else if (propValue.isTextual()) {
            String textValue = propValue.asText();
            return switch (operator) {
                case "=" -> textValue.equals(value);
                case "!=" -> !textValue.equals(value);
                case "LIKE" -> textValue.contains(value.replace("%", ""));
                default -> false;
            };
        }
        return false;
    }
}
