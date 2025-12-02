package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.DataType;
import com.github.znlgis.gis.service.DataManagementService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for uploading GIS data.
 */
@Component
public class UploadDataTool implements McpTool {
    
    private final DataManagementService dataManagementService;
    
    public UploadDataTool(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public String getName() {
        return "upload_data";
    }
    
    @Override
    public String getDescription() {
        return "Upload GIS data (GeoJSON, Shapefile, etc.) to the server. Returns a data_id for subsequent operations.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data", Map.of(
            "type", "string",
            "description", "GeoJSON data or URL to fetch data from"
        ));
        properties.put("format", Map.of(
            "type", "string",
            "description", "Data format (GeoJSON, Shapefile, etc.)",
            "default", "GeoJSON"
        ));
        properties.put("type", Map.of(
            "type", "string",
            "enum", List.of("VECTOR", "RASTER", "FEATURE_COLLECTION"),
            "default", "FEATURE_COLLECTION"
        ));
        
        schema.put("properties", properties);
        schema.put("required", List.of("data"));
        
        return schema;
    }
    
    @Override
    public ToolResult execute(String sessionId, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        
        try {
            String data = (String) parameters.get("data");
            String format = (String) parameters.getOrDefault("format", "GeoJSON");
            String typeStr = (String) parameters.getOrDefault("type", "FEATURE_COLLECTION");
            DataType type = DataType.valueOf(typeStr);
            
            DataReference ref;
            
            // Check if data is a URL
            if (data.startsWith("http://") || data.startsWith("https://")) {
                ref = dataManagementService.uploadDataFromUrl(sessionId, data, format, type);
            } else {
                // Assume it's direct GeoJSON data
                ref = dataManagementService.uploadData(
                    sessionId,
                    new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)),
                    "data." + format.toLowerCase(),
                    format,
                    type
                );
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("data_id", ref.getId());
            result.put("type", ref.getType().toString());
            result.put("format", ref.getFormat());
            result.put("feature_count", ref.getFeatureCount());
            result.put("bounds", ref.getBounds());
            result.put("size", ref.getSize());
            
            return ToolResult.success(result, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
