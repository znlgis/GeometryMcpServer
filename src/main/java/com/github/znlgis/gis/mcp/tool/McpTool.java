package com.github.znlgis.gis.mcp.tool;

import java.util.Map;

/**
 * Base interface for all MCP tools.
 */
public interface McpTool {
    
    /**
     * Gets the tool name.
     */
    String getName();
    
    /**
     * Gets the tool description.
     */
    String getDescription();
    
    /**
     * Gets the input schema for the tool.
     */
    Map<String, Object> getInputSchema();
    
    /**
     * Executes the tool with the given parameters.
     *
     * @param sessionId Session identifier
     * @param parameters Tool parameters
     * @return Tool execution result
     */
    ToolResult execute(String sessionId, Map<String, Object> parameters);
    
    /**
     * Result of tool execution.
     */
    record ToolResult(
        boolean success,
        Object data,
        String error,
        long durationMs
    ) {
        public static ToolResult success(Object data, long durationMs) {
            return new ToolResult(true, data, null, durationMs);
        }
        
        public static ToolResult error(String error, long durationMs) {
            return new ToolResult(false, null, error, durationMs);
        }
    }
}
