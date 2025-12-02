package com.github.znlgis.gis.model;

import com.github.znlgis.gis.model.enums.DataState;
import com.github.znlgis.gis.model.enums.DataType;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 数据引用实体，用于管理 GIS 数据的元数据和生命周期。
 * <p>
 * 该实体存储对 GIS 数据的引用，包含：
 * <ul>
 *   <li>唯一标识符 (UUID) 和会话关联</li>
 *   <li>数据类型 (矢量/栅格/要素集合) 和格式信息</li>
 *   <li>空间元数据：边界范围、坐标系、要素数量</li>
 *   <li>生命周期管理：创建时间、访问时间、过期时间、引用计数</li>
 *   <li>状态跟踪：支持完整的数据生命周期状态转换</li>
 * </ul>
 * <p>
 * Data reference entity for managing GIS data metadata and lifecycle.
 * This entity stores references to GIS data with associated metadata,
 * session information, spatial properties, and lifecycle management properties.
 *
 * @see DataState 数据生命周期状态枚举
 * @see DataType 数据类型枚举
 */
@Entity
@Table(name = "data_references")
public class DataReference {
    
    @Id
    private String id;
    
    @Column(name = "session_id", nullable = false)
    private String sessionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataType type;
    
    @Column(nullable = false)
    private String format;
    
    @Column(name = "storage_location", nullable = false)
    private String storageLocation;
    
    @Column(name = "bounds_min_x")
    private Double boundsMinX;
    
    @Column(name = "bounds_min_y")
    private Double boundsMinY;
    
    @Column(name = "bounds_max_x")
    private Double boundsMaxX;
    
    @Column(name = "bounds_max_y")
    private Double boundsMaxY;
    
    @Column(nullable = false)
    private String crs = "EPSG:4326";
    
    @Column(name = "feature_count")
    private Long featureCount;
    
    @Column(name = "data_size")
    private Long size;
    
    @Column(name = "attributes_json", length = 4000)
    private String attributesJson;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "last_accessed_at")
    private Instant lastAccessedAt;
    
    @Column(name = "expires_at")
    private Instant expiresAt;
    
    @Column(name = "reference_count")
    private Integer referenceCount = 0;
    
    @ElementCollection
    @CollectionTable(name = "data_reference_tags", joinColumns = @JoinColumn(name = "data_reference_id"))
    @Column(name = "tag")
    private List<String> tags = new ArrayList<>();
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DataState state = DataState.CREATED;
    
    public DataReference() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.lastAccessedAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60); // 24h default TTL
    }
    
    public DataReference(String sessionId, DataType type, String format, String storageLocation) {
        this();
        this.sessionId = sessionId;
        this.type = type;
        this.format = format;
        this.storageLocation = storageLocation;
    }
    
    // Getters and setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public DataType getType() {
        return type;
    }
    
    public void setType(DataType type) {
        this.type = type;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public String getStorageLocation() {
        return storageLocation;
    }
    
    public void setStorageLocation(String storageLocation) {
        this.storageLocation = storageLocation;
    }
    
    public double[] getBounds() {
        if (boundsMinX == null || boundsMinY == null || boundsMaxX == null || boundsMaxY == null) {
            return new double[]{0, 0, 0, 0};
        }
        return new double[]{boundsMinX, boundsMinY, boundsMaxX, boundsMaxY};
    }
    
    public void setBounds(double[] bounds) {
        if (bounds != null && bounds.length >= 4) {
            this.boundsMinX = bounds[0];
            this.boundsMinY = bounds[1];
            this.boundsMaxX = bounds[2];
            this.boundsMaxY = bounds[3];
        }
    }
    
    public String getCrs() {
        return crs;
    }
    
    public void setCrs(String crs) {
        this.crs = crs;
    }
    
    public Long getFeatureCount() {
        return featureCount;
    }
    
    public void setFeatureCount(Long featureCount) {
        this.featureCount = featureCount;
    }
    
    public Long getSize() {
        return size;
    }
    
    public void setSize(Long size) {
        this.size = size;
    }
    
    public String getAttributesJson() {
        return attributesJson;
    }
    
    public void setAttributesJson(String attributesJson) {
        this.attributesJson = attributesJson;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }
    
    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }
    
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public Integer getReferenceCount() {
        return referenceCount;
    }
    
    public void setReferenceCount(Integer referenceCount) {
        this.referenceCount = referenceCount;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public DataState getState() {
        return state;
    }
    
    public void setState(DataState state) {
        this.state = state;
    }
    
    /**
     * 更新最后访问时间戳并延长 TTL (生存时间)。
     * <p>
     * 每次数据被访问时调用此方法，将过期时间延长 24 小时。
     * 这是实现 LRU (最近最少使用) 淘汰策略的基础。
     * <p>
     * Updates the last accessed timestamp and extends the TTL (Time To Live).
     * Called whenever the data is accessed, extending expiration by 24 hours.
     */
    public void touch() {
        this.lastAccessedAt = Instant.now();
        this.expiresAt = Instant.now().plusSeconds(24 * 60 * 60);
    }
    
    /**
     * 增加引用计数。
     * <p>
     * 当其他数据通过派生关系引用此数据时调用。
     * 引用计数大于 0 的数据将受到保护，不会被自动清理。
     * <p>
     * Increments the reference count when this data is referenced by derived data.
     * Data with reference count > 0 is protected from automatic cleanup.
     */
    public void incrementReferenceCount() {
        this.referenceCount++;
    }
    
    /**
     * 减少引用计数。
     * <p>
     * 当引用此数据的派生数据被删除时调用。
     * 引用计数不会减少到 0 以下。
     * <p>
     * Decrements the reference count when referencing derived data is deleted.
     * Reference count will not go below 0.
     */
    public void decrementReferenceCount() {
        if (this.referenceCount > 0) {
            this.referenceCount--;
        }
    }
    
    /**
     * 检查数据是否已过期。
     * <p>
     * 过期的数据将在下一次清理任务中被标记为待删除。
     * <p>
     * Checks if the data has expired based on its TTL.
     * Expired data will be marked for deletion in the next cleanup cycle.
     *
     * @return true 如果当前时间已超过过期时间 / if current time is after expiration time
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
