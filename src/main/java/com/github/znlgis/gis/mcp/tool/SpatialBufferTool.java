package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for buffer analysis.
 */
@Component
public class SpatialBufferTool implements McpTool {
    
    private final SpatialAnalysisService spatialAnalysisService;
    
    public SpatialBufferTool(SpatialAnalysisService spatialAnalysisService) {
        this.spatialAnalysisService = spatialAnalysisService;
    }
    
    @Override
    public String getName() {
        return "spatial_buffer";
    }
    
    @Override
    public String getDescription() {
        return "Create a buffer zone around features in the source data. Returns a new data_id containing the buffered geometries.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("source_data_id", Map.of(
            "type", "string",
            "description", "The source data reference ID"
        ));
        properties.put("distance", Map.of(
            "type", "number",
            "description", "Buffer distance"
        ));
        properties.put("units", Map.of(
            "type", "string",
            "description", "Distance units",
            "enum", List.of("meters", "kilometers", "miles", "feet", "degrees"),
            "default", "meters"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("source_data_id", "distance"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String sourceDataId = (String) parameters.get("source_data_id");
            double distance = ((Number) parameters.get("distance")).doubleValue();
            String units = (String) parameters.getOrDefault("units", "meters");
            
            DataReference result = spatialAnalysisService.buffer(sessionId, sourceDataId, distance, units);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result_data_id", result.getId());
            response.put("feature_count", result.getFeatureCount());
            response.put("processing_time_ms", System.currentTimeMillis() - startTime);
            
            return ToolResult.success(response, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
