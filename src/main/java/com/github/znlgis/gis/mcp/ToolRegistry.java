package com.github.znlgis.gis.mcp;

import com.github.znlgis.gis.mcp.tool.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for MCP tools. Manages tool discovery and registration.
 */
@Component
public class ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    
    public ToolRegistry(List<McpTool> mcpTools) {
        for (McpTool tool : mcpTools) {
            registerTool(tool);
        }
        logger.info("Registered {} MCP tools", tools.size());
    }
    
    /**
     * Registers a tool in the registry.
     */
    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
        logger.debug("Registered tool: {}", tool.getName());
    }
    
    /**
     * Gets a tool by name.
     */
    public Optional<McpTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    /**
     * Lists all registered tools.
     */
    public List<McpTool> listTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * Gets the tool catalog for MCP discovery.
     */
    public List<Map<String, Object>> getToolCatalog() {
        List<Map<String, Object>> catalog = new ArrayList<>();
        
        for (McpTool tool : tools.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", tool.getName());
            entry.put("description", tool.getDescription());
            entry.put("inputSchema", tool.getInputSchema());
            catalog.add(entry);
        }
        
        return catalog;
    }
    
    /**
     * Checks if a tool exists.
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
