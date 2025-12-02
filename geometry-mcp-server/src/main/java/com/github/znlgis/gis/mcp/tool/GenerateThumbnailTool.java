package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.service.DataExportService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for generating thumbnails.
 */
@Component
public class GenerateThumbnailTool implements McpTool {
    
    private final DataExportService dataExportService;
    
    public GenerateThumbnailTool(DataExportService dataExportService) {
        this.dataExportService = dataExportService;
    }
    
    @Override
    public String getName() {
        return "generate_thumbnail";
    }
    
    @Override
    public String getDescription() {
        return "Generate a thumbnail image of the data. Returns base64 encoded image data.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "Dataset ID to visualize"
        ));
        properties.put("width", Map.of(
            "type", "integer",
            "description", "Image width in pixels",
            "default", 256
        ));
        properties.put("height", Map.of(
            "type", "integer",
            "description", "Image height in pixels",
            "default", 256
        ));
        properties.put("style", Map.of(
            "type", "string",
            "description", "Style configuration (e.g., 'fill:#3388ff;stroke:#0000ff')"
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
            int width = parameters.containsKey("width") ? ((Number) parameters.get("width")).intValue() : 256;
            int height = parameters.containsKey("height") ? ((Number) parameters.get("height")).intValue() : 256;
            String style = (String) parameters.get("style");
            
            String base64Image = dataExportService.generateThumbnail(sessionId, dataId, width, height, style);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data_id", dataId);
            response.put("width", width);
            response.put("height", height);
            response.put("base64", base64Image);
            response.put("content_type", "image/svg+xml");
            
            return ToolResult.success(response, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
