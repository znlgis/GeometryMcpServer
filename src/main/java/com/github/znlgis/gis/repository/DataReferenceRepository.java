package com.github.znlgis.gis.repository;

import com.github.znlgis.gis.model.DataReference;
import com.github.znlgis.gis.model.enums.DataState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository for DataReference entities.
 */
@Repository
public interface DataReferenceRepository extends JpaRepository<DataReference, String> {
    
    /**
     * Finds all data references for a given session.
     */
    List<DataReference> findBySessionId(String sessionId);
    
    /**
     * Finds data references by session and state.
     */
    List<DataReference> findBySessionIdAndState(String sessionId, DataState state);
    
    /**
     * Finds a data reference by ID and session ID for session isolation.
     */
    Optional<DataReference> findByIdAndSessionId(String id, String sessionId);
    
    /**
     * Finds expired data references with no active references.
     */
    @Query("SELECT d FROM DataReference d WHERE d.state = :state AND d.referenceCount = 0 AND d.expiresAt < :now")
    List<DataReference> findExpiredWithNoRefs(@Param("state") DataState state, @Param("now") Instant now);
    
    /**
     * Finds data references marked for deletion before a given time.
     */
    @Query("SELECT d FROM DataReference d WHERE d.state = 'MARKED_FOR_DELETION' AND d.lastAccessedAt < :before")
    List<DataReference> findMarkedForDeletionBefore(@Param("before") Instant before);
    
    /**
     * Finds data references by state.
     */
    List<DataReference> findByState(DataState state);
    
    /**
     * Counts data references by session.
     */
    long countBySessionId(String sessionId);
    
    /**
     * Calculates total size of data for a session.
     */
    @Query("SELECT COALESCE(SUM(d.size), 0) FROM DataReference d WHERE d.sessionId = :sessionId")
    long sumSizeBySessionId(@Param("sessionId") String sessionId);
}
