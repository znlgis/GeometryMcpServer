package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.DataManagementService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tool for describing data metadata.
 */
@Component
public class DescribeDataTool implements McpTool {
    
    private final DataManagementService dataManagementService;
    
    public DescribeDataTool(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public String getName() {
        return "describe_data";
    }
    
    @Override
    public String getDescription() {
        return "Get detailed metadata about a specific data reference including bounds, CRS, feature count, and attributes.";
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
        
        schema.put("properties", properties);
        schema.put("required", List.of("data_id"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String dataId = (String) parameters.get("data_id");
            
            Optional<DataReference> refOpt = dataManagementService.describeData(sessionId, dataId);
            
            if (refOpt.isEmpty()) {
                return ToolResult.error("Data not found: " + dataId, System.currentTimeMillis() - startTime);
            }
            
            DataReference ref = refOpt.get();
            
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("data_id", ref.getId());
            metadata.put("type", ref.getType().toString());
            metadata.put("format", ref.getFormat());
            metadata.put("crs", ref.getCrs());
            metadata.put("bounds", Map.of(
                "minX", ref.getBounds()[0],
                "minY", ref.getBounds()[1],
                "maxX", ref.getBounds()[2],
                "maxY", ref.getBounds()[3]
            ));
            metadata.put("feature_count", ref.getFeatureCount());
            metadata.put("size", ref.getSize());
            metadata.put("state", ref.getState().toString());
            metadata.put("created_at", ref.getCreatedAt().toString());
            metadata.put("last_accessed_at", ref.getLastAccessedAt().toString());
            metadata.put("expires_at", ref.getExpiresAt().toString());
            metadata.put("reference_count", ref.getReferenceCount());
            metadata.put("tags", ref.getTags());
            
            return ToolResult.success(metadata, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
