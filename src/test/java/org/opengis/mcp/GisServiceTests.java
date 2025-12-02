package org.opengis.mcp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opengis.mcp.config.GisServerProperties;
import org.opengis.mcp.core.IdGenerator;
import org.opengis.mcp.core.ToolResult;
import org.opengis.mcp.data.SessionManager;
import org.opengis.mcp.service.GisService;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GisService.
 */
class GisServiceTests {

    private GisService gisService;

    @BeforeEach
    void setUp() {
        GisServerProperties properties = new GisServerProperties();
        IdGenerator idGenerator = new IdGenerator();
        SessionManager sessionManager = new SessionManager(properties, idGenerator);
        gisService = new GisService(sessionManager, idGenerator);
    }

    @Test
    void testLoadWktPoint() {
        ToolResult<String> result = gisService.loadWktGeometry("POINT(0 0)");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData().startsWith("geom_"));
    }

    @Test
    void testLoadWktPolygon() {
        ToolResult<String> result = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testLoadInvalidWkt() {
        ToolResult<String> result = gisService.loadWktGeometry("INVALID WKT");
        
        assertFalse(result.isSuccess());
        assertEquals("PARSE_ERROR", result.getErrorCode());
    }

    @Test
    void testLoadGeoJsonPoint() {
        String geoJson = "{\"type\":\"Point\",\"coordinates\":[0,0]}";
        ToolResult<String> result = gisService.loadGeoJson(geoJson);
        
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    void testBufferGeometry() {
        ToolResult<String> loadResult = gisService.loadWktGeometry("POINT(0 0)");
        assertTrue(loadResult.isSuccess());
        
        ToolResult<String> bufferResult = gisService.bufferGeometry(loadResult.getData(), 10.0);
        
        assertTrue(bufferResult.isSuccess());
        assertNotNull(bufferResult.getData());
        assertNotEquals(loadResult.getData(), bufferResult.getData());
    }

    @Test
    void testIntersection() {
        ToolResult<String> poly1 = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        ToolResult<String> poly2 = gisService.loadWktGeometry(
            "POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))");
        
        assertTrue(poly1.isSuccess());
        assertTrue(poly2.isSuccess());
        
        ToolResult<String> intersectResult = gisService.intersect(
            poly1.getData(), poly2.getData());
        
        assertTrue(intersectResult.isSuccess());
        assertNotNull(intersectResult.getData());
    }

    @Test
    void testUnion() {
        ToolResult<String> poly1 = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        ToolResult<String> poly2 = gisService.loadWktGeometry(
            "POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))");
        
        ToolResult<String> unionResult = gisService.union(
            poly1.getData(), poly2.getData());
        
        assertTrue(unionResult.isSuccess());
        assertNotNull(unionResult.getData());
    }

    @Test
    void testDifference() {
        ToolResult<String> poly1 = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        ToolResult<String> poly2 = gisService.loadWktGeometry(
            "POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))");
        
        ToolResult<String> diffResult = gisService.difference(
            poly1.getData(), poly2.getData());
        
        assertTrue(diffResult.isSuccess());
        assertNotNull(diffResult.getData());
    }

    @Test
    void testConvexHull() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "MULTIPOINT((0 0), (10 0), (5 10), (3 5))");
        
        ToolResult<String> hullResult = gisService.convexHull(loadResult.getData());
        
        assertTrue(hullResult.isSuccess());
        assertNotNull(hullResult.getData());
    }

    @Test
    void testCentroid() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        
        ToolResult<String> centroidResult = gisService.centroid(loadResult.getData());
        
        assertTrue(centroidResult.isSuccess());
        assertNotNull(centroidResult.getData());
        assertTrue(centroidResult.getMessage().contains("5.0, 5.0"));
    }

    @Test
    void testSimplifyGeometry() {
        // Create a complex polygon
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "POLYGON((0 0, 1 0.1, 2 0, 3 0.1, 4 0, 4 4, 0 4, 0 0))");
        
        ToolResult<String> simplifyResult = gisService.simplifyGeometry(
            loadResult.getData(), 0.5);
        
        assertTrue(simplifyResult.isSuccess());
        assertNotNull(simplifyResult.getData());
    }

    @Test
    void testExportToGeoJson() {
        ToolResult<String> loadResult = gisService.loadWktGeometry("POINT(1 2)");
        
        ToolResult<String> exportResult = gisService.exportToGeoJson(loadResult.getData());
        
        assertTrue(exportResult.isSuccess());
        assertNotNull(exportResult.getData());
        assertTrue(exportResult.getData().contains("Point"));
    }

    @Test
    void testExportToWkt() {
        ToolResult<String> loadResult = gisService.loadWktGeometry("POINT(1 2)");
        
        ToolResult<String> exportResult = gisService.exportToWkt(loadResult.getData());
        
        assertTrue(exportResult.isSuccess());
        assertNotNull(exportResult.getData());
        assertTrue(exportResult.getData().contains("POINT"));
    }

    @Test
    void testExportSummary() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        
        ToolResult<Map<String, Object>> summaryResult = gisService.exportSummary(
            loadResult.getData());
        
        assertTrue(summaryResult.isSuccess());
        Map<String, Object> summary = summaryResult.getData();
        
        assertEquals("Polygon", summary.get("type"));
        assertEquals(100.0, summary.get("area"));
        assertEquals(true, summary.get("isValid"));
    }

    @Test
    void testGetMetadata() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "POINT(0 0)", "EPSG:4326");
        
        ToolResult<Map<String, Object>> metadataResult = gisService.getMetadataResult(
            loadResult.getData());
        
        assertTrue(metadataResult.isSuccess());
        Map<String, Object> metadata = metadataResult.getData();
        
        assertEquals("EPSG:4326", metadata.get("crs"));
        assertEquals("GEOMETRY", metadata.get("type"));
    }

    @Test
    void testQueryByBbox() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "POLYGON((0 0, 20 0, 20 20, 0 20, 0 0))");
        
        ToolResult<String> queryResult = gisService.queryByBbox(
            loadResult.getData(), 5, 5, 15, 15);
        
        assertTrue(queryResult.isSuccess());
        assertNotNull(queryResult.getData());
    }

    @Test
    void testCheckSpatialRelationIntersects() {
        ToolResult<String> poly1 = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        ToolResult<String> poly2 = gisService.loadWktGeometry(
            "POLYGON((5 5, 15 5, 15 15, 5 15, 5 5))");
        
        ToolResult<Boolean> result = gisService.checkSpatialRelation(
            poly1.getData(), poly2.getData(), "intersects");
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
    }

    @Test
    void testCheckSpatialRelationDisjoint() {
        ToolResult<String> poly1 = gisService.loadWktGeometry(
            "POLYGON((0 0, 5 0, 5 5, 0 5, 0 0))");
        ToolResult<String> poly2 = gisService.loadWktGeometry(
            "POLYGON((10 10, 15 10, 15 15, 10 15, 10 10))");
        
        ToolResult<Boolean> result = gisService.checkSpatialRelation(
            poly1.getData(), poly2.getData(), "disjoint");
        
        assertTrue(result.isSuccess());
        assertTrue(result.getData());
    }

    @Test
    void testCalculateDistance() {
        ToolResult<String> point1 = gisService.loadWktGeometry("POINT(0 0)");
        ToolResult<String> point2 = gisService.loadWktGeometry("POINT(3 4)");
        
        ToolResult<Double> result = gisService.calculateDistance(
            point1.getData(), point2.getData());
        
        assertTrue(result.isSuccess());
        assertEquals(5.0, result.getData(), 0.001);
    }

    @Test
    void testCalculateArea() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        
        ToolResult<Double> result = gisService.calculateArea(loadResult.getData());
        
        assertTrue(result.isSuccess());
        assertEquals(100.0, result.getData(), 0.001);
    }

    @Test
    void testCalculateLength() {
        ToolResult<String> loadResult = gisService.loadWktGeometry(
            "LINESTRING(0 0, 10 0)");
        
        ToolResult<Double> result = gisService.calculateLength(loadResult.getData());
        
        assertTrue(result.isSuccess());
        assertEquals(10.0, result.getData(), 0.001);
    }

    @Test
    void testNotFoundError() {
        ToolResult<String> result = gisService.bufferGeometry("nonexistent_id", 10.0);
        
        assertFalse(result.isSuccess());
        assertEquals("NOT_FOUND", result.getErrorCode());
    }
}
