package com.github.znlgis.gis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for the Geometry MCP Server.
 * 
 * This server provides GIS capabilities through the Model Context Protocol (MCP),
 * enabling AI agents to perform spatial analysis, data management, and 
 * coordinate transformations.
 */
@SpringBootApplication
public class GeometryMcpServerApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(GeometryMcpServerApplication.class, args);
    }
}
