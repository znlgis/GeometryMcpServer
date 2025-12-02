package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.service.CoordinateTransformService;
import com.github.znlgis.gis.service.DataManagementService;
import com.github.znlgis.gis.util.GeoJsonUtils;
import org.locationtech.jts.geom.Geometry;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of CoordinateTransformService for CRS transformations.
 * <p>
 * 使用 GeoTools 和 opengis-utils-for-java 进行坐标系转换。
 * Uses GeoTools and opengis-utils-for-java for coordinate reference system transformations.
 */
@Service
@Transactional
public class CoordinateTransformServiceImpl implements CoordinateTransformService {
    
    private static final Logger logger = LoggerFactory.getLogger(CoordinateTransformServiceImpl.class);
    
    private final DataManagementService dataManagementService;

    public CoordinateTransformServiceImpl(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    @Override
    public DataReference transformCrs(String sessionId, String dataId, String sourceCrs, String targetCrs) {
        logger.info("Transforming CRS: {} from {} to {}", dataId, sourceCrs, targetCrs);
        
        try {
            // Parse CRS using GeoTools
            CoordinateReferenceSystem source = CRS.decode(sourceCrs, true);
            CoordinateReferenceSystem target = CRS.decode(targetCrs, true);

            // Get transform
            MathTransform transform = CRS.findMathTransform(source, target, false);

            // Get source data
            String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
            List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
            
            // Transform geometries using GeoTools JTS utilities
            List<Geometry> transformedGeometries = new ArrayList<>();
            for (Geometry geom : geometries) {
                Geometry transformed = JTS.transform(geom, transform);
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
        DataReference ref = dataManagementService.describeData(sessionId, dataId)
            .orElse(null);
        return ref != null ? ref.getCrs() : "EPSG:4326";
    }
    
    @Override
    public boolean isValidCrs(String crsCode) {
        try {
            CRS.decode(crsCode, true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
