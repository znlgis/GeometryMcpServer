package com.github.znlgis.gis.service.impl;

import com.github.znlgis.gis.model.Session;
import com.github.znlgis.gis.model.SessionQuota;
import com.github.znlgis.gis.model.enums.SessionState;
import com.github.znlgis.gis.repository.SessionRepository;
import com.github.znlgis.gis.service.SessionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of SessionService for session lifecycle management.
 */
@Service
@Transactional
public class SessionServiceImpl implements SessionService {
    
    private static final long SESSION_TIMEOUT_HOURS = 24;
    
    private final SessionRepository sessionRepository;
    
    public SessionServiceImpl(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }
    
    @Override
    public Session createSession(String userId) {
        Session session = new Session(userId);
        return sessionRepository.save(session);
    }
    
    @Override
    @Transactional(readOnly = true)
    public Optional<Session> getSession(String sessionId) {
        return sessionRepository.findById(sessionId)
            .filter(s -> s.getState() == SessionState.ACTIVE);
    }
    
    @Override
    public boolean validateSession(String sessionId) {
        Optional<Session> sessionOpt = sessionRepository.findById(sessionId);
        if (sessionOpt.isEmpty()) {
            return false;
        }
        
        Session session = sessionOpt.get();
        if (session.getState() != SessionState.ACTIVE) {
            return false;
        }
        
        // Check if session has expired due to inactivity
        if (session.getLastActivityAt().plus(SESSION_TIMEOUT_HOURS, ChronoUnit.HOURS).isBefore(Instant.now())) {
            session.setState(SessionState.EXPIRED);
            sessionRepository.save(session);
            return false;
        }
        
        // Refresh session
        session.touch();
        sessionRepository.save(session);
        return true;
    }
    
    @Override
    public void closeSession(String sessionId) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setState(SessionState.CLOSED);
            sessionRepository.save(session);
        });
    }
    
    @Override
    @Transactional(readOnly = true)
    public SessionQuota getQuota(String sessionId) {
        return sessionRepository.findById(sessionId)
            .map(Session::getQuota)
            .orElse(SessionQuota.createDefault());
    }
    
    @Override
    public void updateUsage(String sessionId, long dataSize, int dataCount) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.setCurrentDataSize(session.getCurrentDataSize() + dataSize);
            session.setCurrentDataCount(session.getCurrentDataCount() + dataCount);
            session.touch();
            sessionRepository.save(session);
        });
    }
    
    @Override
    public void addDataToSession(String sessionId, String dataId, long dataSize) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.addDataRef(dataId, dataSize);
            sessionRepository.save(session);
        });
    }
    
    @Override
    public void removeDataFromSession(String sessionId, String dataId, long dataSize) {
        sessionRepository.findById(sessionId).ifPresent(session -> {
            session.removeDataRef(dataId, dataSize);
            sessionRepository.save(session);
        });
    }
    
    /**
     * Scheduled task to expire inactive sessions.
     */
    @Scheduled(fixedRate = 3600000) // Every hour
    public void expireInactiveSessions() {
        Instant threshold = Instant.now().minus(SESSION_TIMEOUT_HOURS, ChronoUnit.HOURS);
        List<Session> inactiveSessions = sessionRepository.findInactiveSince(SessionState.ACTIVE, threshold);
        
        for (Session session : inactiveSessions) {
            session.setState(SessionState.EXPIRED);
            sessionRepository.save(session);
        }
    }
}
