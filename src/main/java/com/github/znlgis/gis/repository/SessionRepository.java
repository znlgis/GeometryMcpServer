package com.github.znlgis.gis.repository;

import com.github.znlgis.gis.model.Session;
import com.github.znlgis.gis.model.enums.SessionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Session entities.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, String> {
    
    /**
     * Finds a session by user ID.
     */
    Optional<Session> findByUserId(String userId);
    
    /**
     * Finds active sessions for a user.
     */
    List<Session> findByUserIdAndState(String userId, SessionState state);
    
    /**
     * Finds sessions by state.
     */
    List<Session> findByState(SessionState state);
    
    /**
     * Finds sessions that haven't been active since a given time.
     */
    @Query("SELECT s FROM Session s WHERE s.state = :state AND s.lastActivityAt < :since")
    List<Session> findInactiveSince(@Param("state") SessionState state, @Param("since") Instant since);
    
    /**
     * Checks if a session exists and is active.
     */
    boolean existsBySessionIdAndState(String sessionId, SessionState state);
}
