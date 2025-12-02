package com.github.znlgis.gis.model.enums;

/**
 * 会话生命周期状态枚举。
 * <p>
 * Session lifecycle states.
 */
public enum SessionState {
    /** 活跃状态，可执行操作 / Active state, can perform operations */
    ACTIVE,
    /** 已过期，只读模式 / Expired, read-only mode */
    EXPIRED,
    /** 已关闭，等待清理 / Closed, waiting for cleanup */
    CLOSED
}
