package com.github.znlgis.gis.mcp;

import com.github.znlgis.gis.mcp.tool.McpTool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 工具注册表，负责工具的发现、注册和管理。
 * <p>
 * 该组件提供以下功能：
 * <ul>
 *   <li>自动发现和注册所有实现 {@link McpTool} 接口的 Spring Bean</li>
 *   <li>通过工具名称查询和获取工具实例</li>
 *   <li>生成工具目录供 MCP 客户端发现可用工具</li>
 *   <li>线程安全的并发访问支持</li>
 * </ul>
 * <p>
 * Tool registry for MCP tools. Manages tool discovery, registration, and lookup.
 * Provides thread-safe concurrent access and auto-discovery of McpTool beans.
 *
 * @see McpTool MCP 工具接口
 */
@Component
public class ToolRegistry {
    
    private static final Logger logger = LoggerFactory.getLogger(ToolRegistry.class);
    
    /** 工具存储映射，键为工具名称 / Tool storage map, keyed by tool name */
    private final Map<String, McpTool> tools = new ConcurrentHashMap<>();
    
    /**
     * 构造函数，自动注册所有 MCP 工具 Bean。
     * <p>
     * Spring 容器启动时，自动注入所有实现了 McpTool 接口的 Bean。
     * <p>
     * Constructor that auto-registers all McpTool beans discovered by Spring.
     *
     * @param mcpTools Spring 容器中所有 McpTool 实现 / All McpTool implementations from Spring container
     */
    public ToolRegistry(List<McpTool> mcpTools) {
        for (McpTool tool : mcpTools) {
            registerTool(tool);
        }
        logger.info("Registered {} MCP tools", tools.size());
    }
    
    /**
     * 注册单个工具到注册表。
     * <p>
     * Registers a single tool to the registry.
     *
     * @param tool 要注册的 MCP 工具 / MCP tool to register
     */
    public void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
        logger.debug("Registered tool: {}", tool.getName());
    }
    
    /**
     * 根据名称获取工具。
     * <p>
     * Gets a tool by its name.
     *
     * @param name 工具名称 / Tool name
     * @return 包含工具的 Optional，如果不存在则为空 / Optional containing the tool, or empty if not found
     */
    public Optional<McpTool> getTool(String name) {
        return Optional.ofNullable(tools.get(name));
    }
    
    /**
     * 获取所有已注册工具的列表。
     * <p>
     * Lists all registered tools.
     *
     * @return 工具列表副本 / Copy of the tools list
     */
    public List<McpTool> listTools() {
        return new ArrayList<>(tools.values());
    }
    
    /**
     * 获取 MCP 发现协议所需的工具目录。
     * <p>
     * 返回符合 MCP 规范的工具描述列表，包含名称、描述和输入模式。
     * 用于响应 MCP 客户端的 tools/list 请求。
     * <p>
     * Gets the tool catalog for MCP discovery protocol.
     * Returns a list of tool descriptions conforming to MCP specification.
     *
     * @return 工具目录列表 / Tool catalog list
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
     * 检查指定名称的工具是否存在。
     * <p>
     * Checks if a tool with the given name exists.
     *
     * @param name 工具名称 / Tool name
     * @return true 如果工具存在 / if tool exists
     */
    public boolean hasTool(String name) {
        return tools.containsKey(name);
    }
}
