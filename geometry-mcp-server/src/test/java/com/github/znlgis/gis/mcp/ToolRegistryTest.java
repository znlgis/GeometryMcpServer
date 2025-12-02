package com.github.znlgis.gis.mcp;

import com.github.znlgis.gis.mcp.tool.McpTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ToolRegistry.
 */
class ToolRegistryTest {
    
    private ToolRegistry toolRegistry;
    
    @BeforeEach
    void setUp() {
        // Create a simple test tool
        McpTool testTool = new McpTool() {
            @Override
            public String getName() {
                return "test_tool";
            }
            
            @Override
            public String getDescription() {
                return "A test tool for unit testing";
            }
            
            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of(
                    "type", "object",
                    "properties", Map.of(
                        "input", Map.of("type", "string")
                    ),
                    "required", List.of("input")
                );
            }
            
            @Override
            public ToolResult execute(String sessionId, Map<String, Object> parameters) {
                return ToolResult.success(Map.of("result", "test"), 100);
            }
        };
        
        toolRegistry = new ToolRegistry(List.of(testTool));
    }
    
    @Test
    void testGetTool() {
        Optional<McpTool> tool = toolRegistry.getTool("test_tool");
        
        assertTrue(tool.isPresent());
        assertEquals("test_tool", tool.get().getName());
    }
    
    @Test
    void testGetNonExistentTool() {
        Optional<McpTool> tool = toolRegistry.getTool("non_existent");
        
        assertTrue(tool.isEmpty());
    }
    
    @Test
    void testHasTool() {
        assertTrue(toolRegistry.hasTool("test_tool"));
        assertFalse(toolRegistry.hasTool("non_existent"));
    }
    
    @Test
    void testListTools() {
        List<McpTool> tools = toolRegistry.listTools();
        
        assertFalse(tools.isEmpty());
        assertEquals(1, tools.size());
    }
    
    @Test
    void testGetToolCatalog() {
        List<Map<String, Object>> catalog = toolRegistry.getToolCatalog();
        
        assertFalse(catalog.isEmpty());
        
        Map<String, Object> entry = catalog.get(0);
        assertEquals("test_tool", entry.get("name"));
        assertEquals("A test tool for unit testing", entry.get("description"));
        assertNotNull(entry.get("inputSchema"));
    }
    
    @Test
    void testRegisterTool() {
        McpTool newTool = new McpTool() {
            @Override
            public String getName() {
                return "new_tool";
            }
            
            @Override
            public String getDescription() {
                return "A newly registered tool";
            }
            
            @Override
            public Map<String, Object> getInputSchema() {
                return Map.of("type", "object");
            }
            
            @Override
            public ToolResult execute(String sessionId, Map<String, Object> parameters) {
                return ToolResult.success("done", 0);
            }
        };
        
        toolRegistry.registerTool(newTool);
        
        assertTrue(toolRegistry.hasTool("new_tool"));
        assertEquals(2, toolRegistry.listTools().size());
    }
}
