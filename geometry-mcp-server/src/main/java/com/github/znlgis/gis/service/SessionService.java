package com.github.znlgis.gis.service;

import com.github.znlgis.gis.model.Session;
import com.github.znlgis.gis.model.SessionQuota;

import java.util.Optional;

/**
 * Service for session lifecycle management.
 */
public interface SessionService {
    
    /**
     * Creates a new session.
     *
     * @param userId Optional user ID for authentication integration
     * @return Created session
     */
    Session createSession(String userId);
    
    /**
     * Gets an existing session.
     *
     * @param sessionId Session identifier
     * @return Session if exists and active
     */
    Optional<Session> getSession(String sessionId);
    
    /**
     * Validates and refreshes a session.
     *
     * @param sessionId Session identifier
     * @return true if session is valid and active
     */
    boolean validateSession(String sessionId);
    
    /**
     * Closes a session and marks all associated data for cleanup.
     *
     * @param sessionId Session identifier
     */
    void closeSession(String sessionId);
    
    /**
     * Gets the quota information for a session.
     *
     * @param sessionId Session identifier
     * @return Session quota
     */
    SessionQuota getQuota(String sessionId);
    
    /**
     * Updates session usage after data operations.
     *
     * @param sessionId Session identifier
     * @param dataSize Size of data added or removed (positive for add, negative for remove)
     * @param dataCount Count of data added or removed
     */
    void updateUsage(String sessionId, long dataSize, int dataCount);
    
    /**
     * Associates a data reference with a session.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @param dataSize Size of the data
     */
    void addDataToSession(String sessionId, String dataId, long dataSize);
    
    /**
     * Removes a data reference from a session.
     *
     * @param sessionId Session identifier
     * @param dataId Data reference ID
     * @param dataSize Size of the data
     */
    void removeDataFromSession(String sessionId, String dataId, long dataSize);
}
