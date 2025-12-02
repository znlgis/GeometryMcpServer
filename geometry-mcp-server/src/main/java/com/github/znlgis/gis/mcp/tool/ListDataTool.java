package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.DataManagementService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool for listing data in a session.
 */
@Component
public class ListDataTool implements McpTool {
    
    private final DataManagementService dataManagementService;
    
    public ListDataTool(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public String getName() {
        return "list_data";
    }
    
    @Override
    public String getDescription() {
        return "List all data references in the current session. Returns data IDs, types, sizes, and creation timestamps.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("type_filter", Map.of(
            "type", "string",
            "description", "Filter by data type (VECTOR, RASTER, FEATURE_COLLECTION)"
        ));
        properties.put("format_filter", Map.of(
            "type", "string",
            "description", "Filter by format (GeoJSON, Shapefile, etc.)"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of());
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            Map<String, String> filters = new HashMap<>();
            if (parameters.containsKey("type_filter")) {
                filters.put("type", (String) parameters.get("type_filter"));
            }
            if (parameters.containsKey("format_filter")) {
                filters.put("format", (String) parameters.get("format_filter"));
            }
            
            List<DataReference> refs = dataManagementService.listData(sessionId, filters);
            
            List<Map<String, Object>> result = refs.stream()
                .map(ref -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("data_id", ref.getId());
                    item.put("type", ref.getType().toString());
                    item.put("format", ref.getFormat());
                    item.put("size", ref.getSize());
                    item.put("feature_count", ref.getFeatureCount());
                    item.put("created_at", ref.getCreatedAt().toString());
                    return item;
                })
                .collect(Collectors.toList());
            
            return ToolResult.success(Map.of("data", result, "count", result.size()), 
                System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
