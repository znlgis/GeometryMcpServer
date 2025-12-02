package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.service.DataManagementService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for deleting data.
 */
@Component
public class DeleteDataTool implements McpTool {
    
    private final DataManagementService dataManagementService;
    
    public DeleteDataTool(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public String getName() {
        return "delete_data";
    }
    
    @Override
    public String getDescription() {
        return "Delete a data reference and its associated storage. Cannot delete data that is referenced by other data.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "The data reference ID to delete"
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
            
            boolean deleted = dataManagementService.deleteData(sessionId, dataId);
            
            if (deleted) {
                return ToolResult.success(Map.of("status", "deleted", "data_id", dataId), 
                    System.currentTimeMillis() - startTime);
            } else {
                return ToolResult.error("Failed to delete data: " + dataId, 
                    System.currentTimeMillis() - startTime);
            }
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
