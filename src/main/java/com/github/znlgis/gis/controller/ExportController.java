package com.github.znlgis.gis.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Controller for serving exported files.
 */
@RestController
@RequestMapping("/api/exports")
public class ExportController {
    
    private final Path exportPath;
    
    public ExportController(@Value("${gis.export.path:./exports}") String exportPath) {
        this.exportPath = Paths.get(exportPath);
    }
    
    @GetMapping("/{fileName}")
    public ResponseEntity<Resource> downloadExport(@PathVariable String fileName) {
        try {
            Path filePath = exportPath.resolve(fileName).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists()) {
                String contentType = determineContentType(fileName);
                
                return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    private String determineContentType(String fileName) {
        if (fileName.endsWith(".geojson") || fileName.endsWith(".json")) {
            return "application/geo+json";
        } else if (fileName.endsWith(".kml")) {
            return "application/vnd.google-earth.kml+xml";
        } else if (fileName.endsWith(".zip")) {
            return "application/zip";
        } else if (fileName.endsWith(".wkt")) {
            return "text/plain";
        }
        return "application/octet-stream";
    }
}
