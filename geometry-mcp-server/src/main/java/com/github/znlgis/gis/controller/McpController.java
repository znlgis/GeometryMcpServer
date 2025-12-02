package com.github.znlgis.gis.controller;

import com.github.znlgis.gis.mcp.protocol.McpProtocolHandler;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * MCP HTTP 传输层 REST 控制器。
 * <p>
 * 提供基于 HTTP 的 MCP 协议端点，使 AI 代理能够通过 HTTP 请求
 * 与 GIS 服务进行交互。
 * <p>
 * 端点：
 * <ul>
 *   <li>POST /api/mcp - 处理 MCP JSON-RPC 请求</li>
 *   <li>GET /api/mcp/health - 健康检查端点</li>
 * </ul>
 * <p>
 * REST controller for MCP HTTP transport.
 * Provides HTTP-based MCP protocol endpoints for AI agents to interact
 * with GIS services through HTTP requests.
 *
 * @see McpProtocolHandler MCP 协议处理器
 */
@RestController
@RequestMapping("/api/mcp")
public class McpController {
    
    private final McpProtocolHandler protocolHandler;
    
    /**
     * 构造函数。
     * Constructor.
     *
     * @param protocolHandler MCP 协议处理器 / MCP protocol handler
     */
    public McpController(McpProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
    
    /**
     * 处理 MCP JSON-RPC 请求。
     * <p>
     * 接收 JSON-RPC 2.0 格式的请求，路由到相应的处理方法，并返回 JSON-RPC 响应。
     * <p>
     * Handles MCP JSON-RPC requests over HTTP.
     * Accepts JSON-RPC 2.0 format requests and returns JSON-RPC responses.
     *
     * @param request JSON-RPC 请求体 / JSON-RPC request body
     * @return JSON-RPC 响应 / JSON-RPC response
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleRequest(@RequestBody String request) {
        String response = protocolHandler.handleRequest(request);
        return ResponseEntity.ok(response);
    }
    
    /**
     * 健康检查端点。
     * <p>
     * 返回服务器状态信息，用于监控和负载均衡器健康检查。
     * <p>
     * Health check endpoint for monitoring and load balancer health checks.
     *
     * @return 健康状态 JSON / Health status JSON
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("{\"status\":\"ok\",\"server\":\"geometry-mcp-server\"}");
    }
}
