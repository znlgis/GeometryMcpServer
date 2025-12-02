package com.github.znlgis.gis.mcp.protocol;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.znlgis.gis.mcp.RequestRouter;
import com.github.znlgis.gis.mcp.ToolRegistry;
import com.github.znlgis.gis.mcp.tool.McpTool;
import com.github.znlgis.gis.model.Session;
import com.github.znlgis.gis.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP Protocol handler for processing JSON-RPC messages.
 */
@Component
public class McpProtocolHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(McpProtocolHandler.class);
    private static final String JSONRPC_VERSION = "2.0";
    private static final String MCP_VERSION = "2024-11-05";
    
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final RequestRouter requestRouter;
    private final SessionService sessionService;
    
    public McpProtocolHandler(
            ToolRegistry toolRegistry,
            RequestRouter requestRouter,
            SessionService sessionService) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        this.toolRegistry = toolRegistry;
        this.requestRouter = requestRouter;
        this.sessionService = sessionService;
    }
    
    /**
     * Handles an MCP request message.
     */
    @SuppressWarnings("unchecked")
    public String handleRequest(String requestJson) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            
            String method = (String) request.get("method");
            Object id = request.get("id");
            Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());
            
            Object result = switch (method) {
                case "initialize" -> handleInitialize(params);
                case "tools/list" -> handleToolsList();
                case "tools/call" -> handleToolsCall(params);
                case "ping" -> handlePing();
                default -> createError(-32601, "Method not found: " + method);
            };
            
            if (result instanceof Map && ((Map<?, ?>) result).containsKey("error")) {
                return createErrorResponse(id, (Map<String, Object>) result);
            }
            
            return createSuccessResponse(id, result);
            
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse request", e);
            return createErrorResponse(null, createError(-32700, "Parse error"));
        } catch (Exception e) {
            logger.error("Internal error", e);
            return createErrorResponse(null, createError(-32603, "Internal error: " + e.getMessage()));
        }
    }
    
    /**
     * Handles the initialize method.
     */
    private Map<String, Object> handleInitialize(Map<String, Object> params) {
        // Create a new session
        String userId = (String) params.get("userId");
        Session session = sessionService.createSession(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", MCP_VERSION);
        result.put("sessionId", session.getSessionId());
        
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        result.put("capabilities", capabilities);
        
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "geometry-mcp-server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        
        return result;
    }
    
    /**
     * Handles the tools/list method.
     */
    private Map<String, Object> handleToolsList() {
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolRegistry.getToolCatalog());
        return result;
    }
    
    /**
     * Handles the tools/call method.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolsCall(Map<String, Object> params) {
        String sessionId = (String) params.get("sessionId");
        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
        
        if (sessionId == null) {
            return createError(-32602, "Missing sessionId");
        }
        if (name == null) {
            return createError(-32602, "Missing tool name");
        }
        
        McpTool.ToolResult toolResult = requestRouter.route(sessionId, name, arguments);
        
        Map<String, Object> result = new HashMap<>();
        
        if (toolResult.success()) {
            List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", serializeResult(toolResult.data()))
            );
            result.put("content", content);
        } else {
            result.put("isError", true);
            List<Map<String, Object>> content = List.of(
                Map.of("type", "text", "text", toolResult.error())
            );
            result.put("content", content);
        }
        
        return result;
    }
    
    /**
     * Handles the ping method.
     */
    private Map<String, Object> handlePing() {
        return Map.of();
    }
    
    private Map<String, Object> createError(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of("code", code, "message", message));
        return error;
    }
    
    private String createSuccessResponse(Object id, Object result) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("result", result);
        
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Serialization error\"}}";
        }
    }
    
    private String createErrorResponse(Object id, Map<String, Object> error) {
        Map<String, Object> response = new HashMap<>();
        response.put("jsonrpc", JSONRPC_VERSION);
        response.put("id", id);
        response.put("error", error.get("error"));
        
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            return "{\"jsonrpc\":\"2.0\",\"error\":{\"code\":-32603,\"message\":\"Serialization error\"}}";
        }
    }
    
    private String serializeResult(Object result) {
        if (result instanceof String) {
            return (String) result;
        }
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException e) {
            return result.toString();
        }
    }
}
