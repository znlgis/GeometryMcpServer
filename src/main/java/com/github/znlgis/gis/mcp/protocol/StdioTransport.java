package com.github.znlgis.gis.mcp.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * STDIO transport for MCP protocol.
 * Enabled when running in stdio mode.
 */
@Component
@ConditionalOnProperty(name = "mcp.transport", havingValue = "stdio", matchIfMissing = false)
public class StdioTransport implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(StdioTransport.class);
    
    private final McpProtocolHandler protocolHandler;
    
    public StdioTransport(McpProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting MCP server in STDIO mode");
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
             PrintWriter writer = new PrintWriter(System.out, true, StandardCharsets.UTF_8)) {
            
            String line;
            StringBuilder messageBuilder = new StringBuilder();
            int contentLength = -1;
            
            while ((line = reader.readLine()) != null) {
                // Handle LSP-style header
                if (line.startsWith("Content-Length:")) {
                    contentLength = Integer.parseInt(line.substring(15).trim());
                } else if (line.isEmpty() && contentLength > 0) {
                    // Read the content
                    char[] content = new char[contentLength];
                    int read = reader.read(content, 0, contentLength);
                    if (read == contentLength) {
                        String request = new String(content);
                        String response = protocolHandler.handleRequest(request);
                        
                        // Write response with header
                        writer.println("Content-Length: " + response.getBytes(StandardCharsets.UTF_8).length);
                        writer.println();
                        writer.print(response);
                        writer.flush();
                    }
                    contentLength = -1;
                } else if (contentLength == -1 && !line.isEmpty()) {
                    // Simple JSON-RPC without headers
                    String response = protocolHandler.handleRequest(line);
                    writer.println(response);
                    writer.flush();
                }
            }
        }
        
        logger.info("MCP STDIO server stopped");
    }
}
