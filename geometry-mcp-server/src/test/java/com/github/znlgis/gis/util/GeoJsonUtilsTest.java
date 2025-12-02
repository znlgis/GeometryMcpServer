package com.github.znlgis.gis.util;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Geometry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for GeoJsonUtils.
 */
class GeoJsonUtilsTest {
    
    private static final String SIMPLE_POINT = """
        {
            "type": "Point",
            "coordinates": [100.0, 0.0]
        }
        """;
    
    private static final String FEATURE_COLLECTION = """
        {
            "type": "FeatureCollection",
            "features": [
                {
                    "type": "Feature",
                    "geometry": {
                        "type": "Point",
                        "coordinates": [102.0, 0.5]
                    },
                    "properties": {
                        "name": "Point 1"
                    }
                },
                {
                    "type": "Feature",
                    "geometry": {
                        "type": "Polygon",
                        "coordinates": [
                            [[100.0, 0.0], [101.0, 0.0], [101.0, 1.0], [100.0, 1.0], [100.0, 0.0]]
                        ]
                    },
                    "properties": {
                        "name": "Polygon 1"
                    }
                }
            ]
        }
        """;
    
    @Test
    void testParsePoint() {
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(SIMPLE_POINT);
        
        assertEquals(1, geometries.size());
        assertTrue(geometries.get(0) instanceof org.locationtech.jts.geom.Point);
        
        org.locationtech.jts.geom.Point point = (org.locationtech.jts.geom.Point) geometries.get(0);
        assertEquals(100.0, point.getX(), 0.001);
        assertEquals(0.0, point.getY(), 0.001);
    }
    
    @Test
    void testParseFeatureCollection() {
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(FEATURE_COLLECTION);
        
        assertEquals(2, geometries.size());
        assertTrue(geometries.get(0) instanceof org.locationtech.jts.geom.Point);
        assertTrue(geometries.get(1) instanceof org.locationtech.jts.geom.Polygon);
    }
    
    @Test
    void testCountFeatures() {
        long count = GeoJsonUtils.countFeatures(FEATURE_COLLECTION);
        assertEquals(2, count);
    }
    
    @Test
    void testExtractBounds() {
        double[] bounds = GeoJsonUtils.extractBounds(FEATURE_COLLECTION);
        
        assertNotNull(bounds);
        assertEquals(4, bounds.length);
        assertEquals(100.0, bounds[0], 0.001); // minX
        assertEquals(0.0, bounds[1], 0.001);   // minY
        assertEquals(102.0, bounds[2], 0.001); // maxX
        assertEquals(1.0, bounds[3], 0.001);   // maxY
    }
    
    @Test
    void testToFeatureCollection() {
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(SIMPLE_POINT);
        String result = GeoJsonUtils.toFeatureCollection(geometries);
        
        assertNotNull(result);
        assertTrue(result.contains("FeatureCollection"));
        assertTrue(result.contains("Point"));
    }
    
    @Test
    void testTruncateFeatures() {
        String truncated = GeoJsonUtils.truncateFeatures(FEATURE_COLLECTION, 1);
        
        assertNotNull(truncated);
        assertTrue(truncated.contains("truncated"));
        long count = GeoJsonUtils.countFeatures(truncated);
        assertEquals(1, count);
    }
    
    @Test
    void testEmptyFeatureCollection() {
        String empty = GeoJsonUtils.emptyFeatureCollection();
        
        assertNotNull(empty);
        assertTrue(empty.contains("FeatureCollection"));
        assertEquals(0, GeoJsonUtils.countFeatures(empty));
    }
}
