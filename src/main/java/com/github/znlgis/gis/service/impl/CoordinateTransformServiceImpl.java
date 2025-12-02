package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.CoordinateTransformService;
import com.github.znlgis.gis.service.DataManagementService;
import com.github.znlgis.gis.util.GeoJsonUtils;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of CoordinateTransformService for CRS transformations using Proj4J.
 */
@Service
@Transactional
public class CoordinateTransformServiceImpl implements CoordinateTransformService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoordinateTransformServiceImpl.class);
    
    private final DataManagementService dataManagementService;
    private final CRSFactory crsFactory;
    private final CoordinateTransformFactory ctFactory;
    private final GeometryFactory geometryFactory;
    
    public CoordinateTransformServiceImpl(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
        this.crsFactory = new CRSFactory();
        this.ctFactory = new CoordinateTransformFactory();
        this.geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    }
    
    @Override
    public DataReference transformCrs(String sessionId, String dataId, String sourceCrs, String targetCrs) {
        logger.info("Transforming CRS: {} from {} to {}", dataId, sourceCrs, targetCrs);
        
        try {
            // Parse CRS
            CoordinateReferenceSystem source = crsFactory.createFromName(sourceCrs);
            CoordinateReferenceSystem target = crsFactory.createFromName(targetCrs);
            
            // Get transform
            CoordinateTransform transform = ctFactory.createTransform(source, target);
            
            // Get source data
            String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
            List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
            
            // Transform geometries
            List<Geometry> transformedGeometries = new ArrayList<>();
            for (Geometry geom : geometries) {
                Geometry transformed = transformGeometry(geom, transform);
                transformedGeometries.add(transformed);
            }
            
            // Create result GeoJSON with new CRS
            String resultGeoJson = GeoJsonUtils.toFeatureCollectionWithCrs(transformedGeometries, targetCrs);
            
            DataReference result = dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
            result.setCrs(targetCrs);
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to transform CRS: " + e.getMessage(), e);
        }
    }
    
    private Geometry transformGeometry(Geometry geom, CoordinateTransform transform) {
        Coordinate[] coords = geom.getCoordinates();
        Coordinate[] transformed = new Coordinate[coords.length];
        
        for (int i = 0; i < coords.length; i++) {
            ProjCoordinate src = new ProjCoordinate(coords[i].x, coords[i].y);
            ProjCoordinate dst = new ProjCoordinate();
            transform.transform(src, dst);
            transformed[i] = new Coordinate(dst.x, dst.y);
        }
        
        // Preserve geometry type by reconstructing with transformed coordinates
        if (geom instanceof org.locationtech.jts.geom.Point) {
            return geometryFactory.createPoint(transformed[0]);
        } else if (geom instanceof org.locationtech.jts.geom.LineString) {
            return geometryFactory.createLineString(transformed);
        } else if (geom instanceof org.locationtech.jts.geom.Polygon polygon) {
            // Reconstruct polygon with exterior ring and holes
            org.locationtech.jts.geom.LinearRing shell = geometryFactory.createLinearRing(
                transformRingCoordinates(polygon.getExteriorRing().getCoordinates(), transform)
            );
            org.locationtech.jts.geom.LinearRing[] holes = new org.locationtech.jts.geom.LinearRing[polygon.getNumInteriorRing()];
            for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
                holes[i] = geometryFactory.createLinearRing(
                    transformRingCoordinates(polygon.getInteriorRingN(i).getCoordinates(), transform)
                );
            }
            return geometryFactory.createPolygon(shell, holes);
        } else if (geom instanceof org.locationtech.jts.geom.MultiPoint) {
            org.locationtech.jts.geom.Point[] points = new org.locationtech.jts.geom.Point[geom.getNumGeometries()];
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                points[i] = (org.locationtech.jts.geom.Point) transformGeometry(geom.getGeometryN(i), transform);
            }
            return geometryFactory.createMultiPoint(points);
        } else if (geom instanceof org.locationtech.jts.geom.MultiLineString) {
            org.locationtech.jts.geom.LineString[] lines = new org.locationtech.jts.geom.LineString[geom.getNumGeometries()];
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                lines[i] = (org.locationtech.jts.geom.LineString) transformGeometry(geom.getGeometryN(i), transform);
            }
            return geometryFactory.createMultiLineString(lines);
        } else if (geom instanceof org.locationtech.jts.geom.MultiPolygon) {
            org.locationtech.jts.geom.Polygon[] polygons = new org.locationtech.jts.geom.Polygon[geom.getNumGeometries()];
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                polygons[i] = (org.locationtech.jts.geom.Polygon) transformGeometry(geom.getGeometryN(i), transform);
            }
            return geometryFactory.createMultiPolygon(polygons);
        } else if (geom instanceof org.locationtech.jts.geom.GeometryCollection) {
            Geometry[] geometries = new Geometry[geom.getNumGeometries()];
            for (int i = 0; i < geom.getNumGeometries(); i++) {
                geometries[i] = transformGeometry(geom.getGeometryN(i), transform);
            }
            return geometryFactory.createGeometryCollection(geometries);
        }
        
        // Default fallback - create new geometry from transformed coordinates
        return geometryFactory.createLineString(transformed);
    }
    
    private Coordinate[] transformRingCoordinates(Coordinate[] coords, CoordinateTransform transform) {
        Coordinate[] transformed = new Coordinate[coords.length];
        for (int i = 0; i < coords.length; i++) {
            ProjCoordinate src = new ProjCoordinate(coords[i].x, coords[i].y);
            ProjCoordinate dst = new ProjCoordinate();
            transform.transform(src, dst);
            transformed[i] = new Coordinate(dst.x, dst.y);
        }
        return transformed;
    }
    
    @Override
    public DataReference reprojectRaster(String sessionId, String rasterId, String targetCrs, String resamplingMethod) {
        logger.info("Reprojecting raster: {} to {} using {}", rasterId, targetCrs, resamplingMethod);
        
        // Raster reprojection is complex and would require full raster processing library
        // For now, this is a placeholder implementation
        throw new UnsupportedOperationException("Raster reprojection not yet implemented");
    }
    
    @Override
    @Transactional(readOnly = true)
    public String getCrs(String sessionId, String dataId) {
        Optional<DataReference> refOpt = dataManagementService.describeData(sessionId, dataId);
        return refOpt.map(DataReference::getCrs).orElse("EPSG:4326");
    }
    
    @Override
    public boolean isValidCrs(String crsCode) {
        try {
            crsFactory.createFromName(crsCode);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
