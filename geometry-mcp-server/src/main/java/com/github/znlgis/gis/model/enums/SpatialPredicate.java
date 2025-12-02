package com.github.znlgis.gis.model.enums;

/**
 * 空间查询谓词枚举。
 * <p>
 * 定义几何对象之间的空间关系，用于空间查询操作。
 * 基于 OGC 简单要素规范 (Simple Features Specification) 定义的空间关系。
 * <p>
 * Spatial predicates for spatial queries based on OGC Simple Features.
 */
public enum SpatialPredicate {
    /** 相交：几何对象有公共部分 / Geometries share any portion of space */
    INTERSECTS,
    /** 在内：第一个几何完全在第二个内部 / First geometry is entirely within the second */
    WITHIN,
    /** 包含：第一个几何完全包含第二个 / First geometry completely contains the second */
    CONTAINS,
    /** 交叉：几何对象的内部相交但不完全包含 / Geometries cross each other's interior */
    CROSSES,
    /** 接触：几何对象仅在边界相交 / Geometries touch at their boundaries only */
    TOUCHES,
    /** 重叠：几何对象有公共内部但不完全包含 / Geometries share some but not all interior */
    OVERLAPS,
    /** 不相交：几何对象完全分离 / Geometries are completely separate */
    DISJOINT,
    /** 相等：几何对象完全相同 / Geometries are exactly equal */
    EQUALS
}
