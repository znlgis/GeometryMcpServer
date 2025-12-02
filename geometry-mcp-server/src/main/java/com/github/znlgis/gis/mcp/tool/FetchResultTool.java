package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.service.DataManagementService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for fetching data results with optional formatting.
 */
@Component
public class FetchResultTool implements McpTool {
    
    private final DataManagementService dataManagementService;
    
    public FetchResultTool(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public String getName() {
        return "fetch_result";
    }
    
    @Override
    public String getDescription() {
        return "Fetch actual data content with optional formatting. Supports preview (limited features), full (all data), or summary (statistics only).";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "The data reference ID"
        ));
        properties.put("format", Map.of(
            "type", "string",
            "description", "Output format: preview, full, or summary",
            "enum", List.of("preview", "full", "summary"),
            "default", "preview"
        ));
        properties.put("max_features", Map.of(
            "type", "integer",
            "description", "Maximum number of features to return (for preview mode)",
            "default", 100
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_id"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String dataId = (String) parameters.get("data_id");
            String format = (String) parameters.getOrDefault("format", "preview");
            int maxFeatures = parameters.containsKey("max_features") 
                ? ((Number) parameters.get("max_features")).intValue() 
                : 100;
            
            String result = dataManagementService.fetchResult(sessionId, dataId, format, maxFeatures);
            
            return ToolResult.success(result, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
