package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for spatial union.
 */
@Component
public class SpatialUnionTool implements McpTool {
    
    private final SpatialAnalysisService spatialAnalysisService;
    
    public SpatialUnionTool(SpatialAnalysisService spatialAnalysisService) {
        this.spatialAnalysisService = spatialAnalysisService;
    }
    
    @Override
    public String getName() {
        return "spatial_union";
    }
    
    @Override
    public String getDescription() {
        return "Compute the union of multiple datasets. Combines all geometries into a single result.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_ids", Map.of(
            "type", "array",
            "items", Map.of("type", "string"),
            "description", "Array of dataset IDs to union"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_ids"));
        
        return schema;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            List<String> dataIds = (List<String>) parameters.get("data_ids");
            
            DataReference result = spatialAnalysisService.union(sessionId, dataIds.toArray(new String[0]));
            
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
