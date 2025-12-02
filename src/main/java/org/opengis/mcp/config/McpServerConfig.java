package org.opengis.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import org.opengis.mcp.tools.GisToolsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the MCP Server.
 * Sets up the MCP server with STDIO transport and registers all GIS tools.
 */
@Configuration
public class McpServerConfig {

    private static final Logger log = LoggerFactory.getLogger(McpServerConfig.class);

    @Bean
    public McpSyncServer mcpServer(GisServerProperties properties, 
                                   GisToolsProvider toolsProvider,
                                   ObjectMapper objectMapper) {
        GisServerProperties.McpConfig.ServerConfig serverConfig = properties.getMcp().getServer();
        
        // Create transport provider for STDIO communication
        StdioServerTransportProvider transportProvider = new StdioServerTransportProvider(objectMapper);
        
        // Build server capabilities
        McpSchema.ServerCapabilities capabilities = McpSchema.ServerCapabilities.builder()
            .tools(true)
            .build();
        
        // Build the server with tool specifications
        var serverBuilder = McpServer.sync(transportProvider)
            .serverInfo(serverConfig.getName(), serverConfig.getVersion())
            .capabilities(capabilities);
        
        // Register all tools with their handlers
        for (GisToolsProvider.ToolDefinition toolDef : toolsProvider.getToolDefinitions()) {
            serverBuilder = serverBuilder.tool(toolDef.tool(), toolDef.handler());
        }
        
        McpSyncServer server = serverBuilder.build();

        log.info("MCP Server configured: {} v{}", serverConfig.getName(), serverConfig.getVersion());
        return server;
    }

    @Bean
    public CommandLineRunner startMcpServer(McpSyncServer server, GisServerProperties properties) {
        return args -> {
            log.info("Starting GIS MCP Server: {} v{}",
                properties.getMcp().getServer().getName(),
                properties.getMcp().getServer().getVersion());
        };
    }
}
