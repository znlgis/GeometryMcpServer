package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for attribute filtering.
 */
@Component
public class AttributeFilterTool implements McpTool {
    
    private final SpatialAnalysisService spatialAnalysisService;
    
    public AttributeFilterTool(SpatialAnalysisService spatialAnalysisService) {
        this.spatialAnalysisService = spatialAnalysisService;
    }
    
    @Override
    public String getName() {
        return "attribute_filter";
    }
    
    @Override
    public String getDescription() {
        return "Filter features by attribute expression. Supports simple comparisons like 'property = value' or 'property > 10'.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "Dataset ID to filter"
        ));
        properties.put("expression", Map.of(
            "type", "string",
            "description", "Filter expression (e.g., 'population > 1000', 'name = 'New York'')"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_id", "expression"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String dataId = (String) parameters.get("data_id");
            String expression = (String) parameters.get("expression");
            
            DataReference result = spatialAnalysisService.attributeFilter(sessionId, dataId, expression);
            
            Map<String, Object> response = new HashMap<>();
            response.put("result_data_id", result.getId());
            response.put("filtered_count", result.getFeatureCount());
            response.put("processing_time_ms", System.currentTimeMillis() - startTime);
            
            return ToolResult.success(response, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
