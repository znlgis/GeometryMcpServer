package org.opengis.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.mcp.config.GisServerProperties;
import org.opengis.mcp.core.GisDataObject;
import org.opengis.mcp.core.GisMetadata;
import org.opengis.mcp.core.IdGenerator;
import org.opengis.mcp.data.SessionManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SessionManager.
 */
class SessionManagerTests {

    private SessionManager sessionManager;
    private IdGenerator idGenerator;

    @BeforeEach
    void setUp() {
        GisServerProperties properties = new GisServerProperties();
        idGenerator = new IdGenerator();
        sessionManager = new SessionManager(properties, idGenerator);
    }

    @Test
    void testStoreAndRetrieve() {
        GisMetadata metadata = new GisMetadata("test_001", GisMetadata.DataType.GEOMETRY);
        GisDataObject dataObject = new GisDataObject("test_001", metadata, "test data");
        
        String id = sessionManager.store(dataObject);
        
        assertEquals("test_001", id);
        
        Optional<GisDataObject> retrieved = sessionManager.get("test_001");
        
        assertTrue(retrieved.isPresent());
        assertEquals("test data", retrieved.get().getData());
    }

    @Test
    void testCreate() {
        String id = sessionManager.create(GisMetadata.DataType.GEOMETRY, "auto created");
        
        assertNotNull(id);
        assertTrue(id.startsWith("data_"));
        
        Optional<GisDataObject> retrieved = sessionManager.get(id);
        assertTrue(retrieved.isPresent());
        assertEquals("auto created", retrieved.get().getData());
    }

    @Test
    void testDelete() {
        String id = sessionManager.create(GisMetadata.DataType.GEOMETRY, "to delete");
        
        assertTrue(sessionManager.exists(id));
        
        boolean deleted = sessionManager.delete(id);
        
        assertTrue(deleted);
        assertFalse(sessionManager.exists(id));
    }

    @Test
    void testAlias() {
        String id = sessionManager.create(GisMetadata.DataType.GEOMETRY, "aliased data");
        
        sessionManager.setAlias(id, "myData");
        
        // Should be retrievable by alias
        Optional<GisDataObject> byAlias = sessionManager.get("myData");
        assertTrue(byAlias.isPresent());
        assertEquals("aliased data", byAlias.get().getData());
        
        // Should also be retrievable by ID
        Optional<GisDataObject> byId = sessionManager.get(id);
        assertTrue(byId.isPresent());
    }

    @Test
    void testSetTtl() {
        String id = sessionManager.create(GisMetadata.DataType.GEOMETRY, "ttl test");
        
        boolean updated = sessionManager.setTtl(id, 7200);
        
        assertTrue(updated);
        
        Optional<GisDataObject> retrieved = sessionManager.get(id);
        assertTrue(retrieved.isPresent());
        assertNotNull(retrieved.get().getMetadata().getExpiresAt());
    }

    @Test
    void testListAll() {
        sessionManager.clearAll();
        
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "data1");
        sessionManager.create(GisMetadata.DataType.FEATURE, "data2");
        sessionManager.create(GisMetadata.DataType.LAYER, "data3");
        
        List<GisMetadata> all = sessionManager.listAll();
        
        assertEquals(3, all.size());
    }

    @Test
    void testClearAll() {
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "data1");
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "data2");
        
        long cleared = sessionManager.clearAll();
        
        assertEquals(2, cleared);
        assertEquals(0, sessionManager.listAll().size());
    }

    @Test
    void testGetStatistics() {
        sessionManager.clearAll();
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "data1");
        
        Map<String, Object> stats = sessionManager.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.containsKey("totalObjects"));
        assertTrue(stats.containsKey("estimatedMemoryBytes"));
    }

    @Test
    void testSessionTracking() {
        sessionManager.clearAll();
        
        String id1 = sessionManager.create(GisMetadata.DataType.GEOMETRY, "session1 data1", "session1");
        String id2 = sessionManager.create(GisMetadata.DataType.GEOMETRY, "session1 data2", "session1");
        String id3 = sessionManager.create(GisMetadata.DataType.GEOMETRY, "session2 data", "session2");
        
        List<GisMetadata> session1Objects = sessionManager.listBySession("session1");
        List<GisMetadata> session2Objects = sessionManager.listBySession("session2");
        
        assertEquals(2, session1Objects.size());
        assertEquals(1, session2Objects.size());
    }

    @Test
    void testClearSession() {
        sessionManager.clearAll();
        
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "session1 data1", "session1");
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "session1 data2", "session1");
        sessionManager.create(GisMetadata.DataType.GEOMETRY, "session2 data", "session2");
        
        int cleared = sessionManager.clearSession("session1");
        
        assertEquals(2, cleared);
        assertEquals(0, sessionManager.listBySession("session1").size());
        assertEquals(1, sessionManager.listBySession("session2").size());
    }
}
