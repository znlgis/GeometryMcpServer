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
 * MCP 协议处理器，负责处理 JSON-RPC 消息。
 * <p>
 * 该处理器实现 MCP 规范定义的核心协议方法：
 * <ul>
 *   <li>initialize: 初始化会话并返回服务器能力</li>
 *   <li>tools/list: 列出所有可用工具</li>
 *   <li>tools/call: 调用指定工具</li>
 *   <li>ping: 连接健康检查</li>
 * </ul>
 * <p>
 * 所有消息都遵循 JSON-RPC 2.0 规范。
 * <p>
 * MCP Protocol handler for processing JSON-RPC messages.
 * Implements core MCP protocol methods (initialize, tools/list, tools/call, ping)
 * following the JSON-RPC 2.0 specification.
 *
 * @see <a href="https://spec.modelcontextprotocol.io">MCP Specification</a>
 */
@Component
public class McpProtocolHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(McpProtocolHandler.class);
    
    /** JSON-RPC 协议版本 / JSON-RPC protocol version */
    private static final String JSONRPC_VERSION = "2.0";
    
    /** MCP 协议版本 / MCP protocol version */
    private static final String MCP_VERSION = "2024-11-05";
    
    private final ObjectMapper objectMapper;
    private final ToolRegistry toolRegistry;
    private final RequestRouter requestRouter;
    private final SessionService sessionService;
    
    /**
     * 构造函数。
     * <p>
     * Constructor.
     *
     * @param toolRegistry 工具注册表 / Tool registry
     * @param requestRouter 请求路由器 / Request router
     * @param sessionService 会话服务 / Session service
     */
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
     * 处理 MCP 请求消息。
     * <p>
     * 解析 JSON-RPC 请求，根据方法名分发到相应的处理函数，
     * 并返回 JSON-RPC 响应。
     * <p>
     * Handles an MCP request message. Parses the JSON-RPC request,
     * dispatches to appropriate handler, and returns JSON-RPC response.
     *
     * @param requestJson JSON-RPC 请求字符串 / JSON-RPC request string
     * @return JSON-RPC 响应字符串 / JSON-RPC response string
     */
    @SuppressWarnings("unchecked")
    public String handleRequest(String requestJson) {
        try {
            Map<String, Object> request = objectMapper.readValue(requestJson, Map.class);
            
            String method = (String) request.get("method");
            Object id = request.get("id");
            Map<String, Object> params = (Map<String, Object>) request.getOrDefault("params", Map.of());
            
            // 根据方法名分发处理 / Dispatch based on method name
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
     * 处理 initialize 方法。
     * <p>
     * 创建新会话并返回服务器信息和能力声明。
     * <p>
     * Handles the initialize method.
     * Creates a new session and returns server info and capabilities.
     *
     * @param params 请求参数 / Request parameters
     * @return 初始化结果 / Initialization result
     */
    private Map<String, Object> handleInitialize(Map<String, Object> params) {
        // 创建新会话 / Create a new session
        String userId = (String) params.get("userId");
        Session session = sessionService.createSession(userId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("protocolVersion", MCP_VERSION);
        result.put("sessionId", session.getSessionId());
        
        // 服务器能力声明 / Server capabilities
        Map<String, Object> capabilities = new HashMap<>();
        capabilities.put("tools", Map.of("listChanged", false));
        result.put("capabilities", capabilities);
        
        // 服务器信息 / Server info
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("name", "geometry-mcp-server");
        serverInfo.put("version", "1.0.0");
        result.put("serverInfo", serverInfo);
        
        return result;
    }
    
    /**
     * 处理 tools/list 方法。
     * <p>
     * 返回所有可用工具的目录。
     * <p>
     * Handles the tools/list method.
     * Returns catalog of all available tools.
     *
     * @return 工具列表结果 / Tool list result
     */
    private Map<String, Object> handleToolsList() {
        Map<String, Object> result = new HashMap<>();
        result.put("tools", toolRegistry.getToolCatalog());
        return result;
    }
    
    /**
     * 处理 tools/call 方法。
     * <p>
     * 验证参数后调用指定工具并返回结果。
     * <p>
     * Handles the tools/call method.
     * Validates parameters and invokes the specified tool.
     *
     * @param params 请求参数 / Request parameters
     * @return 工具调用结果 / Tool call result
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> handleToolsCall(Map<String, Object> params) {
        String sessionId = (String) params.get("sessionId");
        String name = (String) params.get("name");
        Map<String, Object> arguments = (Map<String, Object>) params.getOrDefault("arguments", Map.of());
        
        // 验证必需参数 / Validate required parameters
        if (sessionId == null) {
            return createError(-32602, "Missing sessionId");
        }
        if (name == null) {
            return createError(-32602, "Missing tool name");
        }
        
        // 路由并执行工具 / Route and execute tool
        McpTool.ToolResult toolResult = requestRouter.route(sessionId, name, arguments);
        
        // 构建 MCP 格式的响应 / Build MCP-format response
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
     * 处理 ping 方法。
     * <p>
     * 返回空对象表示连接正常。
     * <p>
     * Handles the ping method.
     * Returns empty object indicating connection is alive.
     *
     * @return 空结果 / Empty result
     */
    private Map<String, Object> handlePing() {
        return Map.of();
    }
    
    /**
     * 创建 JSON-RPC 错误对象。
     * Creates a JSON-RPC error object.
     */
    private Map<String, Object> createError(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("error", Map.of("code", code, "message", message));
        return error;
    }
    
    /**
     * 创建成功的 JSON-RPC 响应。
     * Creates a successful JSON-RPC response.
     */
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
    
    /**
     * 创建错误的 JSON-RPC 响应。
     * Creates an error JSON-RPC response.
     */
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
    
    /**
     * 序列化工具结果为字符串。
     * Serializes tool result to string.
     */
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
