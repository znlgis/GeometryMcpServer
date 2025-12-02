package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.SpatialPredicate;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for spatial query.
 */
@Component
public class SpatialQueryTool implements McpTool {
    
    private final SpatialAnalysisService spatialAnalysisService;
    
    public SpatialQueryTool(SpatialAnalysisService spatialAnalysisService) {
        this.spatialAnalysisService = spatialAnalysisService;
    }
    
    @Override
    public String getName() {
        return "spatial_query";
    }
    
    @Override
    public String getDescription() {
        return "Query features using a geometry and spatial predicate. Returns features that match the spatial relationship.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "Dataset ID to query"
        ));
        properties.put("geometry", Map.of(
            "type", "string",
            "description", "Query geometry in WKT format"
        ));
        properties.put("predicate", Map.of(
            "type", "string",
            "enum", List.of("INTERSECTS", "WITHIN", "CONTAINS", "CROSSES", "TOUCHES", "OVERLAPS", "DISJOINT", "EQUALS"),
            "description", "Spatial predicate to use",
            "default", "INTERSECTS"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_id", "geometry"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String dataId = (String) parameters.get("data_id");
            String wkt = (String) parameters.get("geometry");
            String predicateStr = (String) parameters.getOrDefault("predicate", "INTERSECTS");
            SpatialPredicate predicate = SpatialPredicate.valueOf(predicateStr);
            
            DataReference result = spatialAnalysisService.spatialQuery(sessionId, dataId, wkt, predicate);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result_data_id", result.getId());
            response.put("matched_count", result.getFeatureCount());
            response.put("processing_time_ms", System.currentTimeMillis() - startTime);
            
            return ToolResult.success(response, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
