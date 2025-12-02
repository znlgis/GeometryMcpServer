package com.github.znlgis.gis.model;

import com.github.znlgis.gis.model.enums.SessionState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Session model.
 */
class SessionTest {
    
    @Test
    void testDefaultConstructor() {
        Session session = new Session();
        
        assertNotNull(session.getSessionId());
        assertNotNull(session.getCreatedAt());
        assertNotNull(session.getLastActivityAt());
        assertEquals(SessionState.ACTIVE, session.getState());
        assertTrue(session.getDataRefs().isEmpty());
        assertEquals(0, session.getCurrentDataCount());
        assertEquals(0, session.getCurrentDataSize());
    }
    
    @Test
    void testUserIdConstructor() {
        Session session = new Session("user-123");
        
        assertEquals("user-123", session.getUserId());
    }
    
    @Test
    void testAddDataRef() {
        Session session = new Session();
        
        session.addDataRef("data-1", 1024);
        
        assertTrue(session.getDataRefs().contains("data-1"));
        assertEquals(1, session.getCurrentDataCount());
        assertEquals(1024, session.getCurrentDataSize());
    }
    
    @Test
    void testRemoveDataRef() {
        Session session = new Session();
        session.addDataRef("data-1", 1024);
        
        session.removeDataRef("data-1", 1024);
        
        assertFalse(session.getDataRefs().contains("data-1"));
        assertEquals(0, session.getCurrentDataCount());
        assertEquals(0, session.getCurrentDataSize());
    }
    
    @Test
    void testCanAddData() {
        Session session = new Session();
        
        assertTrue(session.canAddData(1024));
        
        // Simulate quota exceeded
        session.setMaxDataSize(100L);
        assertFalse(session.canAddData(200));
    }
    
    @Test
    void testGetQuota() {
        Session session = new Session();
        session.addDataRef("data-1", 1024);
        
        SessionQuota quota = session.getQuota();
        
        assertNotNull(quota);
        assertEquals(1024, quota.currentDataSize());
        assertEquals(1, quota.currentDataCount());
    }
    
    @Test
    void testIsActive() {
        Session session = new Session();
        assertTrue(session.isActive());
        
        session.setState(SessionState.EXPIRED);
        assertFalse(session.isActive());
    }
}
