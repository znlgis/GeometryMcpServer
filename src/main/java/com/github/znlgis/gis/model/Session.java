package com.github.znlgis.gis.model;

import com.github.znlgis.gis.model.enums.SessionState;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Session model for managing user sessions and data isolation.
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
     * Updates activity timestamp.
     */
    public void touch() {
        this.lastActivityAt = Instant.now();
    }
    
    /**
     * Adds a data reference to this session.
     */
    public void addDataRef(String dataRefId, long dataSize) {
        this.dataRefs.add(dataRefId);
        this.currentDataCount++;
        this.currentDataSize += dataSize;
        touch();
    }
    
    /**
     * Removes a data reference from this session.
     */
    public void removeDataRef(String dataRefId, long dataSize) {
        if (this.dataRefs.remove(dataRefId)) {
            this.currentDataCount = Math.max(0, this.currentDataCount - 1);
            this.currentDataSize = Math.max(0, this.currentDataSize - dataSize);
        }
        touch();
    }
    
    /**
     * Checks if the session can accommodate additional data.
     */
    public boolean canAddData(long additionalSize) {
        return currentDataSize + additionalSize <= maxDataSize 
            && currentDataCount < maxDataCount;
    }
    
    /**
     * Gets the quota information for this session.
     */
    public SessionQuota getQuota() {
        return new SessionQuota(maxDataSize, maxDataCount, currentDataSize, currentDataCount);
    }
    
    /**
     * Checks if the session is active.
     */
    public boolean isActive() {
        return state == SessionState.ACTIVE;
    }
}
