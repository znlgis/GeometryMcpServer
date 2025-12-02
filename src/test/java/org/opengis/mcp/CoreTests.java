package org.opengis.mcp;

import org.junit.jupiter.api.Test;
import org.opengis.mcp.core.GisDataObject;
import org.opengis.mcp.core.GisMetadata;
import org.opengis.mcp.core.IdGenerator;
import org.opengis.mcp.core.ToolResult;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for core classes.
 */
class CoreTests {

    @Test
    void testIdGenerator() {
        IdGenerator generator = new IdGenerator();
        
        String id1 = generator.generateId();
        String id2 = generator.generateId();
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertNotEquals(id1, id2);
        assertTrue(id1.startsWith("gis_"));
    }

    @Test
    void testIdGeneratorPrefixes() {
        IdGenerator generator = new IdGenerator();
        generator.reset();
        
        String geomId = generator.generateGeometryId();
        String featId = generator.generateFeatureId();
        String layerId = generator.generateLayerId();
        String dataId = generator.generateDataId();
        
        assertTrue(geomId.startsWith("geom_"));
        assertTrue(featId.startsWith("feat_"));
        assertTrue(layerId.startsWith("layer_"));
        assertTrue(dataId.startsWith("data_"));
    }

    @Test
    void testGisMetadata() {
        GisMetadata metadata = new GisMetadata("test_001", GisMetadata.DataType.GEOMETRY);
        
        assertEquals("test_001", metadata.getId());
        assertEquals(GisMetadata.DataType.GEOMETRY, metadata.getType());
        assertNotNull(metadata.getCreatedAt());
        assertFalse(metadata.isExpired());
    }

    @Test
    void testGisMetadataExpiration() {
        GisMetadata metadata = new GisMetadata("test_001", GisMetadata.DataType.GEOMETRY);
        metadata.setExpiresAt(java.time.Instant.now().minusSeconds(60));
        
        assertTrue(metadata.isExpired());
    }

    @Test
    void testGisDataObject() {
        GisMetadata metadata = new GisMetadata("test_001", GisMetadata.DataType.GEOMETRY);
        String testData = "test geometry data";
        
        GisDataObject dataObject = new GisDataObject("test_001", metadata, testData);
        
        assertEquals("test_001", dataObject.getId());
        assertTrue(dataObject.isLoaded());
        assertEquals(testData, dataObject.getData());
    }

    @Test
    void testGisDataObjectLazyLoading() {
        GisMetadata metadata = new GisMetadata("test_001", GisMetadata.DataType.GEOMETRY);
        String[] loadCount = {""};
        
        GisDataObject dataObject = new GisDataObject("test_001", metadata, () -> {
            loadCount[0] += "loaded";
            return "lazy data";
        });
        
        assertFalse(dataObject.isLoaded());
        assertEquals("", loadCount[0]);
        
        Object data = dataObject.getData();
        
        assertTrue(dataObject.isLoaded());
        assertEquals("lazy data", data);
        assertEquals("loaded", loadCount[0]);
        
        // Second call should not reload
        dataObject.getData();
        assertEquals("loaded", loadCount[0]);
    }

    @Test
    void testToolResultSuccess() {
        ToolResult<String> result = ToolResult.success("test_data", "Operation completed");
        
        assertTrue(result.isSuccess());
        assertEquals("test_data", result.getData());
        assertEquals("Operation completed", result.getMessage());
        assertNull(result.getErrorCode());
    }

    @Test
    void testToolResultError() {
        ToolResult<String> result = ToolResult.error("Something went wrong", "ERR_001");
        
        assertFalse(result.isSuccess());
        assertNull(result.getData());
        assertEquals("Something went wrong", result.getMessage());
        assertEquals("ERR_001", result.getErrorCode());
    }
}
