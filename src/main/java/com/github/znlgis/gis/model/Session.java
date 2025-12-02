package com.github.znlgis.gis.model;

import com.github.znlgis.gis.model.enums.SessionState;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 会话实体，用于管理用户会话和数据隔离。
 * <p>
 * 每个会话包含：
 * <ul>
 *   <li>唯一会话标识符和可选的用户关联</li>
 *   <li>该会话创建的所有数据引用集合</li>
 *   <li>存储配额管理（大小限制和数量限制）</li>
 *   <li>活动时间跟踪和状态管理</li>
 * </ul>
 * <p>
 * 会话提供数据隔离，确保不同会话的数据相互不可见（除非显式共享）。
 * <p>
 * Session entity for managing user sessions and data isolation.
 * Each session maintains its own data references, quota limits, and activity tracking.
 * Sessions provide data isolation, ensuring data from different sessions is not visible
 * to each other unless explicitly shared.
 *
 * @see SessionState 会话状态枚举
 * @see SessionQuota 会话配额记录
 */
@Entity
@Table(name = "sessions")
public class Session {
    
    @Id
    private String sessionId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SessionState state = SessionState.ACTIVE;
    
    @ElementCollection
    @CollectionTable(name = "session_data_refs", joinColumns = @JoinColumn(name = "session_id"))
    @Column(name = "data_ref_id")
    private Set<String> dataRefs = new HashSet<>();
    
    @Column(name = "max_data_size")
    private Long maxDataSize = SessionQuota.DEFAULT_MAX_DATA_SIZE;
    
    @Column(name = "max_data_count")
    private Integer maxDataCount = SessionQuota.DEFAULT_MAX_DATA_COUNT;
    
    @Column(name = "current_data_size")
    private Long currentDataSize = 0L;
    
    @Column(name = "current_data_count")
    private Integer currentDataCount = 0;
    
    public Session() {
        this.sessionId = UUID.randomUUID().toString();
        this.createdAt = Instant.now();
        this.lastActivityAt = Instant.now();
    }
    
    public Session(String userId) {
        this();
        this.userId = userId;
    }
    
    // Getters and setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    public Instant getLastActivityAt() {
        return lastActivityAt;
    }
    
    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }
    
    public SessionState getState() {
        return state;
    }
    
    public void setState(SessionState state) {
        this.state = state;
    }
    
    public Set<String> getDataRefs() {
        return dataRefs;
    }
    
    public void setDataRefs(Set<String> dataRefs) {
        this.dataRefs = dataRefs;
    }
    
    public Long getMaxDataSize() {
        return maxDataSize;
    }
    
    public void setMaxDataSize(Long maxDataSize) {
        this.maxDataSize = maxDataSize;
    }
    
    public Integer getMaxDataCount() {
        return maxDataCount;
    }
    
    public void setMaxDataCount(Integer maxDataCount) {
        this.maxDataCount = maxDataCount;
    }
    
    public Long getCurrentDataSize() {
        return currentDataSize;
    }
    
    public void setCurrentDataSize(Long currentDataSize) {
        this.currentDataSize = currentDataSize;
    }
    
    public Integer getCurrentDataCount() {
        return currentDataCount;
    }
    
    public void setCurrentDataCount(Integer currentDataCount) {
        this.currentDataCount = currentDataCount;
    }
    
    /**
     * 更新会话活动时间戳。
     * <p>
     * 每次会话操作时调用，用于跟踪会话活跃度和实现会话超时。
     * <p>
     * Updates the activity timestamp. Called on each session operation
     * to track activity and implement session timeout.
     */
    public void touch() {
        this.lastActivityAt = Instant.now();
    }
    
    /**
     * 向会话添加数据引用。
     * <p>
     * 同时更新配额使用量（数据计数和大小）。
     * <p>
     * Adds a data reference to this session and updates quota usage.
     *
     * @param dataRefId 数据引用 ID / Data reference ID
     * @param dataSize 数据大小（字节）/ Data size in bytes
     */
    public void addDataRef(String dataRefId, long dataSize) {
        this.dataRefs.add(dataRefId);
        this.currentDataCount++;
        this.currentDataSize += dataSize;
        touch();
    }
    
    /**
     * 从会话移除数据引用。
     * <p>
     * 同时更新配额使用量。如果数据引用不存在，则不执行任何操作。
     * <p>
     * Removes a data reference from this session and updates quota usage.
     * No operation if the data reference does not exist.
     *
     * @param dataRefId 数据引用 ID / Data reference ID
     * @param dataSize 数据大小（字节）/ Data size in bytes
     */
    public void removeDataRef(String dataRefId, long dataSize) {
        if (this.dataRefs.remove(dataRefId)) {
            this.currentDataCount = Math.max(0, this.currentDataCount - 1);
            this.currentDataSize = Math.max(0, this.currentDataSize - dataSize);
        }
        touch();
    }
    
    /**
     * 检查会话是否可以容纳额外的数据。
     * <p>
     * 验证添加指定大小的数据后，是否会超出大小限制或数量限制。
     * <p>
     * Checks if the session can accommodate additional data without
     * exceeding size or count limits.
     *
     * @param additionalSize 要添加的数据大小（字节）/ Additional data size in bytes
     * @return true 如果配额允许添加 / if quota allows adding the data
     */
    public boolean canAddData(long additionalSize) {
        return currentDataSize + additionalSize <= maxDataSize 
            && currentDataCount < maxDataCount;
    }
    
    /**
     * 获取会话的配额信息。
     * <p>
     * 返回包含最大限制和当前使用量的配额快照。
     * <p>
     * Gets the quota information for this session as a snapshot
     * containing max limits and current usage.
     *
     * @return 会话配额记录 / Session quota record
     */
    public SessionQuota getQuota() {
        return new SessionQuota(maxDataSize, maxDataCount, currentDataSize, currentDataCount);
    }
    
    /**
     * 检查会话是否处于活动状态。
     * <p>
     * 只有活动状态的会话才能执行数据操作。
     * <p>
     * Checks if the session is in active state.
     * Only active sessions can perform data operations.
     *
     * @return true 如果会话处于活动状态 / if session is active
     */
    public boolean isActive() {
        return state == SessionState.ACTIVE;
    }
}
