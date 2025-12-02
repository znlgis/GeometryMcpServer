package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.CoordinateTransformService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for CRS transformation.
 */
@Component
public class TransformCrsTool implements McpTool {
    
    private final CoordinateTransformService coordinateTransformService;
    
    public TransformCrsTool(CoordinateTransformService coordinateTransformService) {
        this.coordinateTransformService = coordinateTransformService;
    }
    
    @Override
    public String getName() {
        return "transform_crs";
    }
    
    @Override
    public String getDescription() {
        return "Transform data from one coordinate reference system to another. Common CRS codes: EPSG:4326 (WGS84), EPSG:3857 (Web Mercator).";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "Dataset ID to transform"
        ));
        properties.put("source_crs", Map.of(
            "type", "string",
            "description", "Source coordinate reference system (e.g., 'EPSG:4326')"
        ));
        properties.put("target_crs", Map.of(
            "type", "string",
            "description", "Target coordinate reference system (e.g., 'EPSG:3857')"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_id", "source_crs", "target_crs"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String dataId = (String) parameters.get("data_id");
            String sourceCrs = (String) parameters.get("source_crs");
            String targetCrs = (String) parameters.get("target_crs");
            
            DataReference result = coordinateTransformService.transformCrs(sessionId, dataId, sourceCrs, targetCrs);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result_data_id", result.getId());
            response.put("source_crs", sourceCrs);
            response.put("target_crs", targetCrs);
            response.put("processing_time_ms", System.currentTimeMillis() - startTime);
            
            return ToolResult.success(response, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
