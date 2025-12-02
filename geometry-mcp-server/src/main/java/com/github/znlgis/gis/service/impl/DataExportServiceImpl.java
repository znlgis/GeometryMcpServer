package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.DataExportService;
import com.github.znlgis.gis.service.DataManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Implementation of DataExportService for data export and visualization.
 */
@Service
@Transactional
public class DataExportServiceImpl implements DataExportService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataExportServiceImpl.class);
    private static final long EXPORT_EXPIRY_SECONDS = 3600; // 1 hour
    
    private final DataManagementService dataManagementService;
    private final Path exportBasePath;
    private final String baseUrl;
    
    public DataExportServiceImpl(
            DataManagementService dataManagementService,
            @Value("${gis.export.path:./exports}") String exportPath,
            @Value("${gis.server.baseUrl:http://localhost:8080}") String baseUrl) {
        this.dataManagementService = dataManagementService;
        this.exportBasePath = Path.of(exportPath);
        this.baseUrl = baseUrl;
        
        try {
            Files.createDirectories(exportBasePath);
        } catch (IOException e) {
            logger.error("Failed to create export directory", e);
        }
    }
    
    @Override
    public String generateThumbnail(String sessionId, String dataId, int width, int height, String style) {
        logger.info("Generating thumbnail for: {} ({}x{})", dataId, width, height);
        
        // For now, return a simple SVG representation
        // Full implementation would require rendering with GeoTools
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "preview", 100);
        
        String svg = generateSimpleSvg(geoJson, width, height, style);
        return Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public ExportResult exportData(String sessionId, String dataId, String format, boolean compression) {
        logger.info("Exporting data: {} as {} (compression={})", dataId, format, compression);
        
        String content = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        
        try {
            String exportId = UUID.randomUUID().toString();
            String fileName = exportId + getExtension(format);
            Path exportPath;
            
            if (compression) {
                fileName = exportId + ".zip";
                exportPath = exportBasePath.resolve(fileName);
                
                try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(exportPath))) {
                    ZipEntry entry = new ZipEntry("data" + getExtension(format));
                    zos.putNextEntry(entry);
                    zos.write(convertFormat(content, format).getBytes(StandardCharsets.UTF_8));
                    zos.closeEntry();
                }
            } else {
                exportPath = exportBasePath.resolve(fileName);
                String exportContent = convertFormat(content, format);
                Files.writeString(exportPath, exportContent, StandardCharsets.UTF_8);
            }
            
            long size = Files.size(exportPath);
            String downloadUrl = baseUrl + "/api/exports/" + fileName;
            
            return new ExportResult(downloadUrl, EXPORT_EXPIRY_SECONDS, format, size);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to export data", e);
        }
    }
    
    private String convertFormat(String geoJson, String format) {
        return switch (format.toLowerCase()) {
            case "geojson" -> geoJson;
            case "kml" -> convertToKml(geoJson);
            case "wkt" -> convertToWkt(geoJson);
            default -> geoJson;
        };
    }
    
    private String convertToKml(String geoJson) {
        // Simplified KML conversion
        StringBuilder kml = new StringBuilder();
        kml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        kml.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n");
        kml.append("<Document>\n");
        kml.append("<name>Exported Data</name>\n");
        
        // Parse GeoJSON and convert to KML placemarks
        // This is a simplified implementation
        kml.append("<Placemark><name>Feature</name><description>GeoJSON Export</description></Placemark>\n");
        
        kml.append("</Document>\n");
        kml.append("</kml>");
        return kml.toString();
    }
    
    private String convertToWkt(String geoJson) {
        // WKT conversion would require proper GeoJSON to WKT transformation
        // This is a placeholder - full implementation would use JTS WKTWriter
        throw new UnsupportedOperationException("WKT export not yet implemented");
    }
    
    private String generateSimpleSvg(String geoJson, int width, int height, String style) {
        // Generate a simple SVG placeholder
        String fillColor = style != null && style.contains("fill:") 
            ? extractStyleValue(style, "fill") 
            : "#3388ff";
        String strokeColor = style != null && style.contains("stroke:")
            ? extractStyleValue(style, "stroke")
            : "#0000ff";
        
        return String.format("""
            <svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d">
              <rect width="100%%" height="100%%" fill="#f0f0f0"/>
              <text x="50%%" y="50%%" text-anchor="middle" fill="#666">GIS Data Preview</text>
            </svg>
            """, width, height);
    }
    
    private String extractStyleValue(String style, String property) {
        String[] parts = style.split(";");
        for (String part : parts) {
            if (part.trim().startsWith(property + ":")) {
                return part.substring(part.indexOf(":") + 1).trim();
            }
        }
        return null;
    }
    
    private String getExtension(String format) {
        return switch (format.toLowerCase()) {
            case "geojson" -> ".geojson";
            case "kml" -> ".kml";
            case "wkt" -> ".wkt";
            case "shapefile" -> ".shp";
            default -> ".dat";
        };
    }
}
