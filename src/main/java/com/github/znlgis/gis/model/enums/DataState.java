package com.github.znlgis.gis.model.enums;

/**
 * Lifecycle states for data references.
 * 
 * State transitions:
 * CREATED → ACTIVE: First successful tool usage
 * ACTIVE → REFERENCED: Referenced by other data's derived relationship
 * ACTIVE → EXPIRED: TTL exceeded with no references
 * REFERENCED → EXPIRED: All references deleted after grace period
 * EXPIRED → MARKED_FOR_DELETION: Background cleanup task scans
 * MARKED_FOR_DELETION → DELETED: Confirmed no active connections, physical deletion
 * Any state → ARCHIVED: User-initiated archival (long-term storage, read-only)
 */
public enum DataState {
    CREATED,
    ACTIVE,
    REFERENCED,
    EXPIRED,
    MARKED_FOR_DELETION,
    DELETED,
    ARCHIVED
}
