package com.github.znlgis.gis.mcp.tool;

import com.github.znlgis.gis.service.DataExportService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tool for exporting data.
 */
@Component
public class ExportDataTool implements McpTool {
    
    private final DataExportService dataExportService;
    
    public ExportDataTool(DataExportService dataExportService) {
        this.dataExportService = dataExportService;
    }
    
    @Override
    public String getName() {
        return "export_data";
    }
    
    @Override
    public String getDescription() {
        return "Export data to a specific format. Returns a download URL that expires after a configurable time.";
    }
    
    @Override
    public Map<String, Object> getInputSchema() {
        Map<String, Object> schema = new HashMap<>();
        schema.put("type", "object");
        
        Map<String, Object> properties = new HashMap<>();
        properties.put("data_id", Map.of(
            "type", "string",
            "description", "Dataset ID to export"
        ));
        properties.put("format", Map.of(
            "type", "string",
            "enum", List.of("GeoJSON", "KML", "WKT", "Shapefile"),
            "description", "Export format",
            "default", "GeoJSON"
        ));
        properties.put("compression", Map.of(
            "type", "boolean",
            "description", "Whether to compress the output as ZIP",
            "default", false
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
            String format = (String) parameters.getOrDefault("format", "GeoJSON");
            boolean compression = Boolean.TRUE.equals(parameters.get("compression"));
            
            DataExportService.ExportResult result = dataExportService.exportData(sessionId, dataId, format, compression);
            
            Map<String, Object> response = new HashMap<>();
            response.put("download_url", result.downloadUrl());
            response.put("expires_in", result.expiresIn());
            response.put("format", result.format());
            response.put("size", result.size());
            
            return ToolResult.success(response, System.currentTimeMillis() - startTime);
            
        } catch (Exception e) {
            return ToolResult.error(e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
