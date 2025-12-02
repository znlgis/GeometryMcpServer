package com.github.znlgis.gis.mcp;

import com.github.znlgis.gis.mcp.tool.McpTool;
import com.github.znlgis.gis.service.SessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Routes and validates MCP requests to appropriate tools.
 */
@Component
public class RequestRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestRouter.class);
    
    private final ToolRegistry toolRegistry;
    private final SessionService sessionService;
    private final ExecutorService executor;
    
    public RequestRouter(ToolRegistry toolRegistry, SessionService sessionService) {
        this.toolRegistry = toolRegistry;
        this.sessionService = sessionService;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * Routes a tool call to the appropriate tool.
     */
    public McpTool.ToolResult route(String sessionId, String toolName, Map<String, Object> parameters) {
        logger.info("Routing request: session={}, tool={}", sessionId, toolName);
        
        // Validate session
        if (!sessionService.validateSession(sessionId)) {
            return McpTool.ToolResult.error("Invalid or expired session", 0);
        }
        
        // Find tool
        Optional<McpTool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return McpTool.ToolResult.error("Tool not found: " + toolName, 0);
        }
        
        // Execute tool
        McpTool tool = toolOpt.get();
        try {
            return tool.execute(sessionId, parameters);
        } catch (Exception e) {
            logger.error("Tool execution failed: {}", toolName, e);
            return McpTool.ToolResult.error("Execution failed: " + e.getMessage(), 0);
        }
    }
    
    /**
     * Routes a tool call asynchronously.
     */
    public CompletableFuture<McpTool.ToolResult> routeAsync(String sessionId, String toolName, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> route(sessionId, toolName, parameters), executor);
    }
    
    /**
     * Validates tool parameters against schema.
     */
    public boolean validateParameters(String toolName, Map<String, Object> parameters) {
        Optional<McpTool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return false;
        }
        
        McpTool tool = toolOpt.get();
        Map<String, Object> schema = tool.getInputSchema();
        
        // Check required parameters
        @SuppressWarnings("unchecked")
        java.util.List<String> required = (java.util.List<String>) schema.get("required");
        if (required != null) {
            for (String param : required) {
                if (!parameters.containsKey(param)) {
                    logger.warn("Missing required parameter: {}", param);
                    return false;
                }
            }
        }
        
        return true;
    }
}
