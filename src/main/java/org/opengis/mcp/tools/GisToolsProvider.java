package org.opengis.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.annotation.PostConstruct;
import org.opengis.mcp.config.GisServerProperties;
import org.opengis.mcp.core.GisMetadata;
import org.opengis.mcp.core.ToolResult;
import org.opengis.mcp.data.SessionManager;
import org.opengis.mcp.service.GisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * MCP Tools for GIS operations.
 * Implements the MCP protocol layer for AI integration.
 */
@Service
public class GisToolsProvider {

    private static final Logger log = LoggerFactory.getLogger(GisToolsProvider.class);

    private final GisService gisService;
    private final SessionManager sessionManager;
    private final GisServerProperties properties;
    private final ObjectMapper objectMapper;

    public GisToolsProvider(GisService gisService, 
                           SessionManager sessionManager,
                           GisServerProperties properties,
                           ObjectMapper objectMapper) {
        this.gisService = gisService;
        this.sessionManager = sessionManager;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        log.info("GIS Tools Provider initialized with {} tools", getToolDefinitions().size());
    }

    /**
     * Record to hold tool and its handler together.
     */
    public record ToolDefinition(
        McpSchema.Tool tool,
        BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler
    ) {}

    /**
     * Returns all tool definitions for MCP server registration.
     */
    public List<ToolDefinition> getToolDefinitions() {
        List<ToolDefinition> defs = new ArrayList<>();
        
        // Data Loading Tools
        defs.add(createToolDef("load_wkt_geometry", 
            "Load a geometry from WKT (Well-Known Text) format. Returns geometry ID.",
            Map.of(
                "wkt", Map.of("type", "string", "description", "WKT representation of the geometry"),
                "crs", Map.of("type", "string", "description", "Coordinate Reference System (default: EPSG:4326)")
            ),
            List.of("wkt"),
            (exchange, args) -> {
                String wkt = (String) args.get("wkt");
                String crs = (String) args.getOrDefault("crs", "EPSG:4326");
                return formatResult(gisService.loadWktGeometry(wkt, crs));
            }));
        
        defs.add(createToolDef("load_geojson",
            "Load a geometry from GeoJSON format. Returns geometry ID.",
            Map.of(
                "geojson", Map.of("type", "string", "description", "GeoJSON representation of the geometry"),
                "crs", Map.of("type", "string", "description", "Coordinate Reference System (default: EPSG:4326)")
            ),
            List.of("geojson"),
            (exchange, args) -> {
                String geojson = (String) args.get("geojson");
                String crs = (String) args.getOrDefault("crs", "EPSG:4326");
                return formatResult(gisService.loadGeoJson(geojson, crs));
            }));
        
        // Spatial Analysis Tools
        defs.add(createToolDef("buffer_geometry",
            "Create a buffer around a geometry. Returns new geometry ID.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the source geometry"),
                "distance", Map.of("type", "number", "description", "Buffer distance in CRS units")
            ),
            List.of("geometry_id", "distance"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                double distance = ((Number) args.get("distance")).doubleValue();
                return formatResult(gisService.bufferGeometry(geometryId, distance));
            }));
        
        defs.add(createToolDef("intersect",
            "Compute the intersection of two geometries. Returns new geometry ID.",
            Map.of(
                "geometry_id1", Map.of("type", "string", "description", "ID of the first geometry"),
                "geometry_id2", Map.of("type", "string", "description", "ID of the second geometry")
            ),
            List.of("geometry_id1", "geometry_id2"),
            (exchange, args) -> {
                String geomId1 = (String) args.get("geometry_id1");
                String geomId2 = (String) args.get("geometry_id2");
                return formatResult(gisService.intersect(geomId1, geomId2));
            }));
        
        defs.add(createToolDef("union",
            "Compute the union of two geometries. Returns new geometry ID.",
            Map.of(
                "geometry_id1", Map.of("type", "string", "description", "ID of the first geometry"),
                "geometry_id2", Map.of("type", "string", "description", "ID of the second geometry")
            ),
            List.of("geometry_id1", "geometry_id2"),
            (exchange, args) -> {
                String geomId1 = (String) args.get("geometry_id1");
                String geomId2 = (String) args.get("geometry_id2");
                return formatResult(gisService.union(geomId1, geomId2));
            }));
        
        defs.add(createToolDef("difference",
            "Compute the difference of two geometries. Returns new geometry ID.",
            Map.of(
                "geometry_id1", Map.of("type", "string", "description", "ID of the first geometry"),
                "geometry_id2", Map.of("type", "string", "description", "ID of the second geometry")
            ),
            List.of("geometry_id1", "geometry_id2"),
            (exchange, args) -> {
                String geomId1 = (String) args.get("geometry_id1");
                String geomId2 = (String) args.get("geometry_id2");
                return formatResult(gisService.difference(geomId1, geomId2));
            }));
        
        defs.add(createToolDef("convex_hull",
            "Compute the convex hull of a geometry. Returns new geometry ID.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the source geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.convexHull(geometryId));
            }));
        
        defs.add(createToolDef("centroid",
            "Compute the centroid of a geometry. Returns new geometry ID.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the source geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.centroid(geometryId));
            }));
        
        // Query Tools
        defs.add(createToolDef("get_metadata",
            "Get metadata for a data object including bounds, CRS, and feature count.",
            Map.of(
                "data_id", Map.of("type", "string", "description", "ID of the data object")
            ),
            List.of("data_id"),
            (exchange, args) -> {
                String dataId = (String) args.get("data_id");
                return formatResult(gisService.getMetadataResult(dataId));
            }));
        
        defs.add(createToolDef("query_by_bbox",
            "Query geometries by bounding box. Returns new geometry ID with clipped result.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the source geometry"),
                "min_x", Map.of("type", "number", "description", "Minimum X coordinate"),
                "min_y", Map.of("type", "number", "description", "Minimum Y coordinate"),
                "max_x", Map.of("type", "number", "description", "Maximum X coordinate"),
                "max_y", Map.of("type", "number", "description", "Maximum Y coordinate")
            ),
            List.of("geometry_id", "min_x", "min_y", "max_x", "max_y"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                double minX = ((Number) args.get("min_x")).doubleValue();
                double minY = ((Number) args.get("min_y")).doubleValue();
                double maxX = ((Number) args.get("max_x")).doubleValue();
                double maxY = ((Number) args.get("max_y")).doubleValue();
                return formatResult(gisService.queryByBbox(geometryId, minX, minY, maxX, maxY));
            }));
        
        defs.add(createToolDef("check_spatial_relation",
            "Check spatial relationship between two geometries.",
            Map.of(
                "geometry_id1", Map.of("type", "string", "description", "ID of the first geometry"),
                "geometry_id2", Map.of("type", "string", "description", "ID of the second geometry"),
                "relation", Map.of("type", "string", "description", 
                    "Relation type: intersects, contains, within, touches, crosses, overlaps, disjoint, equals")
            ),
            List.of("geometry_id1", "geometry_id2", "relation"),
            (exchange, args) -> {
                String geomId1 = (String) args.get("geometry_id1");
                String geomId2 = (String) args.get("geometry_id2");
                String relation = (String) args.get("relation");
                return formatResult(gisService.checkSpatialRelation(geomId1, geomId2, relation));
            }));
        
        defs.add(createToolDef("calculate_distance",
            "Calculate distance between two geometries.",
            Map.of(
                "geometry_id1", Map.of("type", "string", "description", "ID of the first geometry"),
                "geometry_id2", Map.of("type", "string", "description", "ID of the second geometry")
            ),
            List.of("geometry_id1", "geometry_id2"),
            (exchange, args) -> {
                String geomId1 = (String) args.get("geometry_id1");
                String geomId2 = (String) args.get("geometry_id2");
                return formatResult(gisService.calculateDistance(geomId1, geomId2));
            }));
        
        defs.add(createToolDef("calculate_area",
            "Calculate area of a geometry.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.calculateArea(geometryId));
            }));
        
        defs.add(createToolDef("calculate_length",
            "Calculate length/perimeter of a geometry.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.calculateLength(geometryId));
            }));
        
        // Projection & Transform Tools
        defs.add(createToolDef("transform_crs",
            "Update CRS metadata for a geometry. Note: This updates metadata only; actual coordinate transformation requires additional libraries. Returns new geometry ID.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the source geometry"),
                "target_crs", Map.of("type", "string", "description", "Target CRS (e.g., EPSG:3857)")
            ),
            List.of("geometry_id", "target_crs"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                String targetCrs = (String) args.get("target_crs");
                return formatResult(gisService.transformCrs(geometryId, targetCrs));
            }));
        
        defs.add(createToolDef("simplify_geometry",
            "Simplify geometry using Douglas-Peucker algorithm. Returns new geometry ID.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the source geometry"),
                "tolerance", Map.of("type", "number", "description", "Simplification tolerance")
            ),
            List.of("geometry_id", "tolerance"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                double tolerance = ((Number) args.get("tolerance")).doubleValue();
                return formatResult(gisService.simplifyGeometry(geometryId, tolerance));
            }));
        
        // Export Tools
        defs.add(createToolDef("export_to_geojson",
            "Export geometry to GeoJSON format.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.exportToGeoJson(geometryId));
            }));
        
        defs.add(createToolDef("export_to_wkt",
            "Export geometry to WKT format.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.exportToWkt(geometryId));
            }));
        
        defs.add(createToolDef("export_summary",
            "Get a summary of geometry including type, bounds, area, length, and centroid.",
            Map.of(
                "geometry_id", Map.of("type", "string", "description", "ID of the geometry")
            ),
            List.of("geometry_id"),
            (exchange, args) -> {
                String geometryId = (String) args.get("geometry_id");
                return formatResult(gisService.exportSummary(geometryId));
            }));
        
        // Session Management Tools
        defs.add(createToolDef("list_data_objects",
            "List all data objects in the current session.",
            Map.of(),
            List.of(),
            (exchange, args) -> {
                List<GisMetadata> objects = sessionManager.listAll();
                List<Map<String, Object>> resultList = objects.stream()
                    .map(m -> {
                        Map<String, Object> obj = new HashMap<>();
                        obj.put("id", m.getId());
                        obj.put("type", m.getType().name());
                        obj.put("crs", m.getCrs());
                        obj.put("alias", m.getAlias());
                        obj.put("createdAt", m.getCreatedAt().toString());
                        return obj;
                    })
                    .collect(Collectors.toList());
                return formatResult(ToolResult.success(resultList, "Found " + resultList.size() + " data objects"));
            }));
        
        defs.add(createToolDef("delete_data_object",
            "Delete a data object from the session.",
            Map.of(
                "data_id", Map.of("type", "string", "description", "ID of the data object to delete")
            ),
            List.of("data_id"),
            (exchange, args) -> {
                String dataId = (String) args.get("data_id");
                boolean deleted = sessionManager.delete(dataId);
                return formatResult(deleted ? 
                    ToolResult.success(true, "Deleted: " + dataId) : 
                    ToolResult.error("Not found: " + dataId, "NOT_FOUND"));
            }));
        
        defs.add(createToolDef("set_alias",
            "Set an alias for a data object for easier reference.",
            Map.of(
                "data_id", Map.of("type", "string", "description", "ID of the data object"),
                "alias", Map.of("type", "string", "description", "Alias name")
            ),
            List.of("data_id", "alias"),
            (exchange, args) -> {
                String dataId = (String) args.get("data_id");
                String alias = (String) args.get("alias");
                sessionManager.setAlias(dataId, alias);
                return formatResult(ToolResult.success(true, "Alias set: " + alias + " -> " + dataId));
            }));
        
        defs.add(createToolDef("set_ttl",
            "Update the time-to-live for a data object.",
            Map.of(
                "data_id", Map.of("type", "string", "description", "ID of the data object"),
                "seconds", Map.of("type", "integer", "description", "TTL in seconds")
            ),
            List.of("data_id", "seconds"),
            (exchange, args) -> {
                String dataId = (String) args.get("data_id");
                int seconds = ((Number) args.get("seconds")).intValue();
                boolean updated = sessionManager.setTtl(dataId, seconds);
                return formatResult(updated ?
                    ToolResult.success(true, "TTL updated: " + seconds + " seconds") :
                    ToolResult.error("Not found: " + dataId, "NOT_FOUND"));
            }));
        
        defs.add(createToolDef("clear_cache",
            "Clear all cached data objects.",
            Map.of(),
            List.of(),
            (exchange, args) -> {
                long cleared = sessionManager.clearAll();
                return formatResult(ToolResult.success(cleared, "Cleared " + cleared + " objects"));
            }));
        
        defs.add(createToolDef("get_cache_statistics",
            "Get cache statistics including object count and memory usage.",
            Map.of(),
            List.of(),
            (exchange, args) -> {
                Map<String, Object> stats = sessionManager.getStatistics();
                return formatResult(ToolResult.success(stats));
            }));
        
        return defs;
    }

    private ToolDefinition createToolDef(
            String name, 
            String description,
            Map<String, Object> properties,
            List<String> required,
            BiFunction<McpSyncServerExchange, Map<String, Object>, McpSchema.CallToolResult> handler) {
        
        McpSchema.Tool tool = new McpSchema.Tool(name, description, createJsonSchema(properties, required));
        
        return new ToolDefinition(tool, (exchange, args) -> {
            try {
                return handler.apply(exchange, args);
            } catch (Exception e) {
                log.error("Tool execution error: {} - {}", name, e.getMessage(), e);
                return new McpSchema.CallToolResult(
                    List.of(new McpSchema.TextContent("Error: " + e.getMessage())),
                    true
                );
            }
        });
    }

    private String createJsonSchema(Map<String, Object> properties, List<String> required) {
        try {
            Map<String, Object> schema = new HashMap<>();
            schema.put("type", "object");
            schema.put("properties", properties);
            schema.put("required", required);
            return objectMapper.writeValueAsString(schema);
        } catch (Exception e) {
            log.error("Failed to create JSON schema", e);
            return "{}";
        }
    }

    private McpSchema.CallToolResult formatResult(Object result) {
        try {
            String content;
            boolean isError = false;
            
            if (result instanceof ToolResult<?> toolResult) {
                isError = !toolResult.isSuccess();
                Object data = toolResult.getData();
                
                if (data == null) {
                    content = toolResult.getMessage() != null ? toolResult.getMessage() : 
                        (isError ? "Error" : "Success");
                } else if (data instanceof String) {
                    content = toolResult.getMessage() != null ? 
                        toolResult.getMessage() + "\nData: " + data : (String) data;
                } else {
                    String jsonData = objectMapper.writerWithDefaultPrettyPrinter()
                        .writeValueAsString(data);
                    content = toolResult.getMessage() != null ?
                        toolResult.getMessage() + "\n" + jsonData : jsonData;
                }
            } else {
                content = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(result);
            }
            
            return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent(content)),
                isError
            );
            
        } catch (Exception e) {
            return new McpSchema.CallToolResult(
                List.of(new McpSchema.TextContent("Error formatting result: " + e.getMessage())),
                true
            );
        }
    }
}
