package com.github.znlgis.gis.controller;

import com.github.znlgis.gis.mcp.protocol.McpProtocolHandler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for MCP HTTP transport.
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {
    
    private final McpProtocolHandler protocolHandler;
    
    public McpController(McpProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
    
    /**
     * Handles MCP JSON-RPC requests over HTTP.
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleRequest(@RequestBody String request) {
        String response = protocolHandler.handleRequest(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\",\"server\":\"geometry-mcp-server\"}");
    }
}
