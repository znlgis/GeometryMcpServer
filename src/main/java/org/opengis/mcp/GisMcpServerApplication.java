package org.opengis.mcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for the GIS MCP Server.
 * This server provides GIS capabilities through the Model Context Protocol (MCP).
 */
@SpringBootApplication
@EnableScheduling
public class GisMcpServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GisMcpServerApplication.class, args);
    }
}
