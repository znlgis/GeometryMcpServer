package com.github.znlgis.gis.model.enums;

/**
 * 支持的 GIS 数据类型枚举。
 * <p>
 * Enumeration of supported GIS data types.
 */
public enum DataType {
    /** 矢量数据 / Vector data (points, lines, polygons) */
    VECTOR,
    /** 栅格数据 / Raster data (images, grids) */
    RASTER,
    /** 要素集合 / Feature collection (GeoJSON FeatureCollection) */
    FEATURE_COLLECTION
}
