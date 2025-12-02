package com.github.znlgis.gis.model.enums;

/**
 * 数据引用生命周期状态枚举。
 * <p>
 * 状态转换规则：
 * <pre>
 * CREATED → ACTIVE：首次被工具成功使用
 * ACTIVE → REFERENCED：被其他数据的派生关系引用
 * ACTIVE → EXPIRED：超过 TTL 且无引用
 * REFERENCED → EXPIRED：所有引用者被删除后等待宽限期
 * EXPIRED → MARKED_FOR_DELETION：后台清理任务扫描到
 * MARKED_FOR_DELETION → DELETED：确认无活跃连接后物理删除
 * 任意状态 → ARCHIVED：用户主动归档（长期存储，只读）
 * </pre>
 * <p>
 * Lifecycle states for data references with defined transition rules.
 *
 * @see com.github.znlgis.gis.model.DataReference
 */
public enum DataState {
    /** 已创建，尚未使用 / Created but not yet used */
    CREATED,
    /** 活跃状态，可正常使用 / Active state, can be used */
    ACTIVE,
    /** 被其他数据引用 / Referenced by other data */
    REFERENCED,
    /** 已过期，等待清理 / Expired, waiting for cleanup */
    EXPIRED,
    /** 已标记删除，等待物理删除 / Marked for deletion, waiting for physical removal */
    MARKED_FOR_DELETION,
    /** 已删除 / Deleted */
    DELETED,
    /** 已归档，只读长期存储 / Archived, read-only long-term storage */
    ARCHIVED
}
