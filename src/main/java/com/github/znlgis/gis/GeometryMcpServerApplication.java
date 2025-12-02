package com.github.znlgis.gis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Geometry MCP Server 主应用程序入口。
 * <p>
 * 本服务器通过模型上下文协议 (Model Context Protocol, MCP) 提供完整的 GIS 能力，
 * 使 AI 代理能够执行以下操作：
 * <ul>
 *   <li>空间分析：缓冲区、交集、并集、空间查询等</li>
 *   <li>数据管理：上传、存储、检索、删除 GIS 数据</li>
 *   <li>坐标转换：支持多种坐标参考系统 (CRS) 之间的转换</li>
 *   <li>数据导出：支持多种格式导出和缩略图生成</li>
 * </ul>
 * <p>
 * Main application entry point for the Geometry MCP Server.
 * This server provides comprehensive GIS capabilities through the Model Context Protocol (MCP),
 * enabling AI agents to perform spatial analysis, data management, coordinate transformations,
 * and data export operations.
 *
 * @author znlgis
 * @version 1.0.0
 * @since 2024
 */
@SpringBootApplication
public class GeometryMcpServerApplication {
    
    /**
     * 应用程序主入口点。
     * Application main entry point.
     *
     * @param args 命令行参数 / Command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(GeometryMcpServerApplication.class, args);
    }
}
