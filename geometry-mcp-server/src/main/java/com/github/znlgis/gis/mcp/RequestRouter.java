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
 * MCP 请求路由器，负责验证和分发工具调用请求。
 * <p>
 * 该组件提供以下功能：
 * <ul>
 *   <li>会话验证：确保请求来自有效的活动会话</li>
 *   <li>工具查找：根据名称定位目标工具</li>
 *   <li>参数验证：验证请求参数是否符合工具输入模式</li>
 *   <li>同步/异步执行：支持同步和异步工具调用</li>
 *   <li>错误处理：捕获并报告工具执行异常</li>
 * </ul>
 * <p>
 * Request router that validates and dispatches MCP tool calls.
 * Provides session validation, tool lookup, parameter validation,
 * and both synchronous and asynchronous execution modes.
 *
 * @see ToolRegistry 工具注册表
 * @see McpTool MCP 工具接口
 */
@Component
public class RequestRouter {
    
    private static final Logger logger = LoggerFactory.getLogger(RequestRouter.class);
    
    private final ToolRegistry toolRegistry;
    private final SessionService sessionService;
    
    /** 用于异步工具执行的线程池 / Thread pool for async tool execution */
    private final ExecutorService executor;
    
    /**
     * 构造函数。
     * <p>
     * Constructor.
     *
     * @param toolRegistry 工具注册表 / Tool registry
     * @param sessionService 会话服务 / Session service
     */
    public RequestRouter(ToolRegistry toolRegistry, SessionService sessionService) {
        this.toolRegistry = toolRegistry;
        this.sessionService = sessionService;
        this.executor = Executors.newCachedThreadPool();
    }
    
    /**
     * 同步路由工具调用请求。
     * <p>
     * 验证会话和工具后执行工具并返回结果。
     * <p>
     * Routes a tool call synchronously after validating session and tool.
     *
     * @param sessionId 会话 ID / Session ID
     * @param toolName 工具名称 / Tool name
     * @param parameters 工具参数 / Tool parameters
     * @return 工具执行结果 / Tool execution result
     */
    public McpTool.ToolResult route(String sessionId, String toolName, Map<String, Object> parameters) {
        logger.info("Routing request: session={}, tool={}", sessionId, toolName);
        
        // 验证会话 / Validate session
        if (!sessionService.validateSession(sessionId)) {
            return McpTool.ToolResult.error("Invalid or expired session", 0);
        }
        
        // 查找工具 / Find tool
        Optional<McpTool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return McpTool.ToolResult.error("Tool not found: " + toolName, 0);
        }
        
        // 执行工具 / Execute tool
        McpTool tool = toolOpt.get();
        try {
            return tool.execute(sessionId, parameters);
        } catch (Exception e) {
            logger.error("Tool execution failed: {}", toolName, e);
            return McpTool.ToolResult.error("Execution failed: " + e.getMessage(), 0);
        }
    }
    
    /**
     * 异步路由工具调用请求。
     * <p>
     * 使用线程池异步执行工具调用，适用于耗时操作。
     * <p>
     * Routes a tool call asynchronously using the thread pool.
     * Suitable for long-running operations.
     *
     * @param sessionId 会话 ID / Session ID
     * @param toolName 工具名称 / Tool name
     * @param parameters 工具参数 / Tool parameters
     * @return 包含执行结果的 CompletableFuture / CompletableFuture containing the result
     */
    public CompletableFuture<McpTool.ToolResult> routeAsync(String sessionId, String toolName, Map<String, Object> parameters) {
        return CompletableFuture.supplyAsync(() -> route(sessionId, toolName, parameters), executor);
    }
    
    /**
     * 验证工具参数是否符合输入模式。
     * <p>
     * 检查必需参数是否存在。
     * <p>
     * Validates tool parameters against the input schema.
     * Checks for presence of required parameters.
     *
     * @param toolName 工具名称 / Tool name
     * @param parameters 待验证的参数 / Parameters to validate
     * @return true 如果参数有效 / if parameters are valid
     */
    public boolean validateParameters(String toolName, Map<String, Object> parameters) {
        Optional<McpTool> toolOpt = toolRegistry.getTool(toolName);
        if (toolOpt.isEmpty()) {
            return false;
        }
        
        McpTool tool = toolOpt.get();
        Map<String, Object> schema = tool.getInputSchema();
        
        // 检查必需参数 / Check required parameters
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
