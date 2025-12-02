package com.github.znlgis.gis.mcp.tool;

import java.util.Map;

/**
 * MCP 工具基础接口。
 * <p>
 * 所有 MCP 工具都必须实现此接口。接口定义了工具的基本结构：
 * <ul>
 *   <li>工具名称：用于工具发现和调用的唯一标识符</li>
 *   <li>工具描述：供 AI 代理理解工具功能的自然语言描述</li>
 *   <li>输入模式：定义工具参数的 JSON Schema</li>
 *   <li>执行方法：工具的实际逻辑实现</li>
 * </ul>
 * <p>
 * Base interface for all MCP tools.
 * Every MCP tool must implement this interface, providing tool metadata
 * (name, description, input schema) and execution logic.
 *
 * @see ToolResult 工具执行结果记录
 */
public interface McpTool {
    
    /**
     * 获取工具名称。
     * <p>
     * 名称用于工具发现和调用，应该是唯一的、描述性的标识符。
     * 例如：upload_data, spatial_buffer, transform_crs
     * <p>
     * Gets the tool name used for discovery and invocation.
     *
     * @return 工具名称 / Tool name
     */
    String getName();
    
    /**
     * 获取工具描述。
     * <p>
     * 描述应该清晰地说明工具的功能，供 AI 代理理解何时以及如何使用该工具。
     * <p>
     * Gets the tool description that explains the tool's functionality
     * to AI agents.
     *
     * @return 工具描述 / Tool description
     */
    String getDescription();
    
    /**
     * 获取工具的输入模式 (JSON Schema)。
     * <p>
     * 定义工具接受的参数及其类型、约束和描述。
     * 模式应符合 JSON Schema 规范。
     * <p>
     * Gets the input schema (JSON Schema) defining accepted parameters,
     * their types, constraints, and descriptions.
     *
     * @return 输入模式 Map / Input schema map
     */
    Map<String, Object> getInputSchema();
    
    /**
     * 执行工具逻辑。
     * <p>
     * 根据提供的会话 ID 和参数执行工具操作。
     * 实现应处理所有异常并返回适当的 ToolResult。
     * <p>
     * Executes the tool logic with the given session and parameters.
     * Implementations should handle exceptions and return appropriate ToolResult.
     *
     * @param sessionId 会话标识符 / Session identifier
     * @param parameters 工具参数 / Tool parameters
     * @return 工具执行结果 / Tool execution result
     */
    ToolResult execute(String sessionId, Map<String, Object> parameters);
    
    /**
     * 工具执行结果记录。
     * <p>
     * 封装工具执行的结果，包括：
     * <ul>
     *   <li>success: 执行是否成功</li>
     *   <li>data: 成功时的返回数据</li>
     *   <li>error: 失败时的错误信息</li>
     *   <li>durationMs: 执行耗时（毫秒）</li>
     * </ul>
     * <p>
     * Result record encapsulating tool execution outcome including
     * success status, return data, error message, and duration.
     */
    record ToolResult(
        boolean success,
        Object data,
        String error,
        long durationMs
    ) {
        /**
         * 创建成功结果。
         * Creates a success result.
         *
         * @param data 返回数据 / Return data
         * @param durationMs 执行耗时（毫秒）/ Execution duration in ms
         * @return 成功的 ToolResult / Successful ToolResult
         */
        public static ToolResult success(Object data, long durationMs) {
            return new ToolResult(true, data, null, durationMs);
        }
        
        /**
         * 创建失败结果。
         * Creates an error result.
         *
         * @param error 错误信息 / Error message
         * @param durationMs 执行耗时（毫秒）/ Execution duration in ms
         * @return 失败的 ToolResult / Error ToolResult
         */
        public static ToolResult error(String error, long durationMs) {
            return new ToolResult(false, null, error, durationMs);
        }
    }
}
