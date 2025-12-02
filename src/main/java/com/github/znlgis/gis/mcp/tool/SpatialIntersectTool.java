package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for spatial intersection.
 */
@Component
public class SpatialIntersectTool implements McpTool {
    
    private final SpatialAnalysisService spatialAnalysisService;
    
    public SpatialIntersectTool(SpatialAnalysisService spatialAnalysisService) {
        this.spatialAnalysisService = spatialAnalysisService;
    }
    
    @Override
    public String getName() {
        return "spatial_intersect";
    }
    
    @Override
    public String getDescription() {
        return "Compute the intersection of two datasets. Returns features from dataset A that intersect with dataset B.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id_a", Map.of(
            "type", "string",
            "description", "First dataset ID"
        ));
        properties.put("data_id_b", Map.of(
            "type", "string",
            "description", "Second dataset ID"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_id_a", "data_id_b"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String dataIdA = (String) parameters.get("data_id_a");
            String dataIdB = (String) parameters.get("data_id_b");
            
            DataReference result = spatialAnalysisService.intersect(sessionId, dataIdA, dataIdB);
            
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
