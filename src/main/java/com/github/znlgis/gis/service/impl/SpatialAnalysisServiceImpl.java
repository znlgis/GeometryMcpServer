package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.SpatialPredicate;
import com.github.znlgis.gis.service.DataManagementService;
import com.github.znlgis.gis.service.SpatialAnalysisService;
import com.github.znlgis.gis.util.GeoJsonUtils;
import com.znlgis.ogu4j.geometry.GeometryUtil;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 空间分析服务实现类。
 * <p>
 * 基于 opengis-utils-for-java 和 JTS (Java Topology Suite) 实现各种空间分析操作。
 * 所有操作采用引用传递模式，即输入和输出都是数据 ID，
 * 实际数据通过 DataManagementService 进行读取和存储。
 * <p>
 * Implementation of SpatialAnalysisService using opengis-utils-for-java and JTS.
 * All operations use reference passing pattern for optimal performance
 * with large datasets.
 *
 * @see SpatialAnalysisService
 * @see com.znlgis.ogu4j.geometry.GeometryUtil
 */
@Service
@Transactional
public class SpatialAnalysisServiceImpl implements SpatialAnalysisService {
    
    private static final Logger logger = LoggerFactory.getLogger(SpatialAnalysisServiceImpl.class);
    
    private final DataManagementService dataManagementService;
    
    public SpatialAnalysisServiceImpl(DataManagementService dataManagementService) {
        this.dataManagementService = dataManagementService;
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：使用 GeometryUtil.buffer() 方法创建缓冲区。
     * 距离单位会自动转换为度（适用于 WGS84 坐标系）。
     */
    @Override
    public DataReference buffer(String sessionId, String sourceDataId, double distance, String units) {
        logger.info("Performing buffer analysis: source={}, distance={} {}", sourceDataId, distance, units);
        
        // 获取源数据 / Get source data
        String sourceGeoJson = dataManagementService.fetchResult(sessionId, sourceDataId, "full", Integer.MAX_VALUE);
        
        // 转换距离单位为度 / Convert distance to degrees
        double distanceInDegrees = convertToDegreesIfNeeded(distance, units);
        
        // 执行缓冲区操作 / Perform buffer operation
        List<Geometry> sourceGeometries = GeoJsonUtils.parseGeometries(sourceGeoJson);
        List<Geometry> bufferedGeometries = new ArrayList<>();
        
        for (Geometry geom : sourceGeometries) {
            Geometry buffered = GeometryUtil.buffer(geom, distanceInDegrees);
            bufferedGeometries.add(buffered);
        }
        
        // 转换回 GeoJSON 并保存 / Convert back to GeoJSON and save
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(bufferedGeometries);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, sourceDataId);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：计算两个数据集中所有几何对象对的交集。
     */
    @Override
    public DataReference intersect(String sessionId, String dataIdA, String dataIdB) {
        logger.info("Performing intersection: {} ∩ {}", dataIdA, dataIdB);
        
        String geoJsonA = dataManagementService.fetchResult(sessionId, dataIdA, "full", Integer.MAX_VALUE);
        String geoJsonB = dataManagementService.fetchResult(sessionId, dataIdB, "full", Integer.MAX_VALUE);
        
        List<Geometry> geometriesA = GeoJsonUtils.parseGeometries(geoJsonA);
        List<Geometry> geometriesB = GeoJsonUtils.parseGeometries(geoJsonB);
        
        List<Geometry> intersections = new ArrayList<>();
        
        // 计算所有几何对的交集 / Compute intersection for all geometry pairs
        for (Geometry geomA : geometriesA) {
            for (Geometry geomB : geometriesB) {
                if (GeometryUtil.intersects(geomA, geomB)) {
                    Geometry intersection = GeometryUtil.intersection(geomA, geomB);
                    if (!GeometryUtil.isEmpty(intersection)) {
                        intersections.add(intersection);
                    }
                }
            }
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(intersections);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataIdA);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：使用级联并集算法合并多个数据集的所有几何对象。
     */
    @Override
    public DataReference union(String sessionId, String... dataIds) {
        logger.info("Performing union of {} datasets", dataIds.length);
        
        List<Geometry> allGeometries = new ArrayList<>();
        
        // 收集所有几何对象 / Collect all geometries
        for (String dataId : dataIds) {
            String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
            allGeometries.addAll(GeoJsonUtils.parseGeometries(geoJson));
        }
        
        if (allGeometries.isEmpty()) {
            return dataManagementService.createDerivedData(sessionId, 
                GeoJsonUtils.emptyFeatureCollection(), dataIds.length > 0 ? dataIds[0] : null);
        }
        
        // 执行级联并集 / Perform cascaded union using GeometryUtil
        Geometry unionResult = GeometryUtil.union(allGeometries.toArray(new Geometry[0]));

        String resultGeoJson = GeoJsonUtils.toFeatureCollection(List.of(unionResult));
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, 
            dataIds.length > 0 ? dataIds[0] : null);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：根据 WKT 定义的查询几何和空间谓词筛选要素。
     */
    @Override
    public DataReference spatialQuery(String sessionId, String dataId, String wkt, SpatialPredicate predicate) {
        logger.info("Performing spatial query: dataId={}, predicate={}", dataId, predicate);
        
        // 解析查询几何 / Parse query geometry using GeometryUtil
        Geometry queryGeometry;
        try {
            queryGeometry = GeometryUtil.wkt2Geometry(wkt);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid WKT: " + wkt, e);
        }
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
        
        List<Geometry> matchedGeometries = new ArrayList<>();
        
        // 根据空间谓词筛选 / Filter by spatial predicate using GeometryUtil
        for (Geometry geom : geometries) {
            boolean matches = switch (predicate) {
                case INTERSECTS -> GeometryUtil.intersects(geom, queryGeometry);
                case WITHIN -> GeometryUtil.within(geom, queryGeometry);
                case CONTAINS -> GeometryUtil.contains(geom, queryGeometry);
                case CROSSES -> GeometryUtil.crosses(geom, queryGeometry);
                case TOUCHES -> GeometryUtil.touches(geom, queryGeometry);
                case OVERLAPS -> GeometryUtil.overlaps(geom, queryGeometry);
                case DISJOINT -> GeometryUtil.disjoint(geom, queryGeometry);
                case EQUALS -> geom.equals(queryGeometry); // Use JTS native equals
            };
            
            if (matches) {
                matchedGeometries.add(geom);
            }
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(matchedGeometries);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：使用简化的表达式解析器筛选要素属性。
     */
    @Override
    public DataReference attributeFilter(String sessionId, String dataId, String expression) {
        logger.info("Performing attribute filter: dataId={}, expression={}", dataId, expression);
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        
        // 应用属性筛选表达式 / Apply attribute filter expression
        String filteredGeoJson = GeoJsonUtils.filterByExpression(geoJson, expression);
        
        return dataManagementService.createDerivedData(sessionId, filteredGeoJson, dataId);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：计算 A 减去 B 的几何差集。
     */
    @Override
    public DataReference difference(String sessionId, String dataIdA, String dataIdB) {
        logger.info("Performing difference: {} - {}", dataIdA, dataIdB);
        
        String geoJsonA = dataManagementService.fetchResult(sessionId, dataIdA, "full", Integer.MAX_VALUE);
        String geoJsonB = dataManagementService.fetchResult(sessionId, dataIdB, "full", Integer.MAX_VALUE);
        
        List<Geometry> geometriesA = GeoJsonUtils.parseGeometries(geoJsonA);
        List<Geometry> geometriesB = GeoJsonUtils.parseGeometries(geoJsonB);
        
        // 创建 B 的并集 / Create union of B geometries
        Geometry unionB = null;
        for (Geometry geomB : geometriesB) {
            unionB = (unionB == null) ? geomB : unionB.union(geomB);
        }
        
        // 计算差集 / Compute difference
        List<Geometry> differences = new ArrayList<>();
        for (Geometry geomA : geometriesA) {
            Geometry diff = (unionB != null) ? geomA.difference(unionB) : geomA;
            if (!diff.isEmpty()) {
                differences.add(diff);
            }
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(differences);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataIdA);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：计算对称差集（A ∪ B - A ∩ B）。
     */
    @Override
    public DataReference symmetricDifference(String sessionId, String dataIdA, String dataIdB) {
        logger.info("Performing symmetric difference: {} △ {}", dataIdA, dataIdB);
        
        String geoJsonA = dataManagementService.fetchResult(sessionId, dataIdA, "full", Integer.MAX_VALUE);
        String geoJsonB = dataManagementService.fetchResult(sessionId, dataIdB, "full", Integer.MAX_VALUE);
        
        List<Geometry> geometriesA = GeoJsonUtils.parseGeometries(geoJsonA);
        List<Geometry> geometriesB = GeoJsonUtils.parseGeometries(geoJsonB);
        
        // 创建各自的并集 / Create unions
        Geometry unionA = null;
        for (Geometry geom : geometriesA) {
            unionA = (unionA == null) ? geom : unionA.union(geom);
        }
        
        Geometry unionB = null;
        for (Geometry geom : geometriesB) {
            unionB = (unionB == null) ? geom : unionB.union(geom);
        }
        
        // 计算对称差集 / Compute symmetric difference
        Geometry result;
        if (unionA == null && unionB == null) {
            result = null;
        } else if (unionA == null) {
            result = unionB;
        } else if (unionB == null) {
            result = unionA;
        } else {
            result = unionA.symDifference(unionB);
        }
        
        String resultGeoJson = result != null && !result.isEmpty() 
            ? GeoJsonUtils.toFeatureCollection(List.of(result))
            : GeoJsonUtils.emptyFeatureCollection();
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataIdA);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：计算每个几何对象的最小凸包多边形。
     */
    @Override
    public DataReference convexHull(String sessionId, String dataId) {
        logger.info("Computing convex hull for: {}", dataId);
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
        
        List<Geometry> hulls = new ArrayList<>();
        for (Geometry geom : geometries) {
            hulls.add(geom.convexHull());
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(hulls);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
    }
    
    /**
     * {@inheritDoc}
     * <p>
     * 实现说明：计算每个几何对象的几何中心点。
     */
    @Override
    public DataReference centroid(String sessionId, String dataId) {
        logger.info("Computing centroids for: {}", dataId);
        
        String geoJson = dataManagementService.fetchResult(sessionId, dataId, "full", Integer.MAX_VALUE);
        List<Geometry> geometries = GeoJsonUtils.parseGeometries(geoJson);
        
        List<Geometry> centroids = new ArrayList<>();
        for (Geometry geom : geometries) {
            centroids.add(geom.getCentroid());
        }
        
        String resultGeoJson = GeoJsonUtils.toFeatureCollection(centroids);
        
        return dataManagementService.createDerivedData(sessionId, resultGeoJson, dataId);
    }
    
    // ==================== 距离单位转换 / Distance Unit Conversion ====================
    
    /**
     * 距离转换常量（赤道处的近似值）。
     * 注意：这些是近似值，随纬度变化。对于精确转换，
     * 应使用基于实际位置的测地计算。
     * <p>
     * Distance conversion constants (approximate values at equator).
     * Note: These are approximations that vary by latitude.
     */
    private static final double METERS_PER_DEGREE = 111320.0;      // 1 degree ≈ 111.32 km at equator
    private static final double KILOMETERS_PER_DEGREE = 111.32;    // 1 degree ≈ 111.32 km at equator
    private static final double MILES_PER_DEGREE = 69.0;           // 1 degree ≈ 69 miles at equator
    private static final double FEET_PER_DEGREE = 364320.0;        // 1 degree ≈ 364,320 feet at equator
    
    /**
     * 将距离转换为度（适用于地理坐标系的近似转换）。
     * <p>
     * 注意：这是假设赤道距离的近似值。
     * 对于更高精度的结果，应使用适当的测地计算。
     * <p>
     * Converts distance to degrees (approximate for geographic coordinates).
     *
     * @param distance 距离值 / Distance value
     * @param units 距离单位 / Distance units
     * @return 度数 / Degrees
     */
    private double convertToDegreesIfNeeded(double distance, String units) {
        return switch (units.toLowerCase()) {
            case "meters", "m" -> distance / METERS_PER_DEGREE;
            case "kilometers", "km" -> distance / KILOMETERS_PER_DEGREE;
            case "miles", "mi" -> distance / MILES_PER_DEGREE;
            case "feet", "ft" -> distance / FEET_PER_DEGREE;
            case "degrees", "deg" -> distance;
            default -> distance; // 假设为度 / Assume degrees
        };
    }
}
