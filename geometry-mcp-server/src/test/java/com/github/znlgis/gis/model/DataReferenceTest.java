package com.github.znlgis.gis.model;

import com.github.znlgis.gis.model.enums.DataState;
import com.github.znlgis.gis.model.enums.DataType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DataReference model.
 */
class DataReferenceTest {
    
    @Test
    void testDefaultConstructor() {
        DataReference ref = new DataReference();
        
        assertNotNull(ref.getId());
        assertNotNull(ref.getCreatedAt());
        assertNotNull(ref.getLastAccessedAt());
        assertNotNull(ref.getExpiresAt());
        assertEquals(DataState.CREATED, ref.getState());
        assertEquals(0, ref.getReferenceCount());
    }
    
    @Test
    void testParameterizedConstructor() {
        DataReference ref = new DataReference("session-1", DataType.VECTOR, "GeoJSON", "/path/to/file");
        
        assertEquals("session-1", ref.getSessionId());
        assertEquals(DataType.VECTOR, ref.getType());
        assertEquals("GeoJSON", ref.getFormat());
        assertEquals("/path/to/file", ref.getStorageLocation());
    }
    
    @Test
    void testTouch() {
        DataReference ref = new DataReference();
        java.time.Instant originalExpiry = ref.getExpiresAt();
        
        // Wait a moment
        try { 
            Thread.sleep(10); 
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        ref.touch();
        
        assertTrue(ref.getExpiresAt().isAfter(originalExpiry));
    }
    
    @Test
    void testReferenceCount() {
        DataReference ref = new DataReference();
        assertEquals(0, ref.getReferenceCount());
        
        ref.incrementReferenceCount();
        assertEquals(1, ref.getReferenceCount());
        
        ref.incrementReferenceCount();
        assertEquals(2, ref.getReferenceCount());
        
        ref.decrementReferenceCount();
        assertEquals(1, ref.getReferenceCount());
        
        ref.decrementReferenceCount();
        assertEquals(0, ref.getReferenceCount());
        
        // Should not go negative
        ref.decrementReferenceCount();
        assertEquals(0, ref.getReferenceCount());
    }
    
    @Test
    void testBounds() {
        DataReference ref = new DataReference();
        
        double[] bounds = new double[]{-180, -90, 180, 90};
        ref.setBounds(bounds);
        
        double[] retrieved = ref.getBounds();
        assertArrayEquals(bounds, retrieved, 0.001);
    }
    
    @Test
    void testIsExpired() {
        DataReference ref = new DataReference();
        assertFalse(ref.isExpired());
        
        // Set expiry in the past
        ref.setExpiresAt(java.time.Instant.now().minusSeconds(1));
        assertTrue(ref.isExpired());
    }
}
