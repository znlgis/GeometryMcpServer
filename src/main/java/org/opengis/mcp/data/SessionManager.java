package org.opengis.mcp.data;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.opengis.mcp.config.GisServerProperties;
import org.opengis.mcp.core.GisDataObject;
import org.opengis.mcp.core.GisMetadata;
import org.opengis.mcp.core.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Session manager for managing GIS data objects within sessions.
 * Implements LRU/TTL hybrid caching strategy.
 */
@Service
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private final GisServerProperties properties;
    private final IdGenerator idGenerator;
    
    // Main data store
    private final Cache<String, GisDataObject> dataCache;
    
    // Alias mapping
    private final Map<String, String> aliasToId = new ConcurrentHashMap<>();
    
    // Session tracking
    private final Map<String, Set<String>> sessionObjects = new ConcurrentHashMap<>();

    public SessionManager(GisServerProperties properties, IdGenerator idGenerator) {
        this.properties = properties;
        this.idGenerator = idGenerator;
        
        this.dataCache = Caffeine.newBuilder()
            .maximumSize(properties.getCache().getMaxSize())
            .expireAfterAccess(Duration.ofSeconds(properties.getCache().getTtl()))
            .evictionListener((key, value, cause) -> {
                if (key != null) {
                    log.debug("Evicted data object: {} (cause: {})", key, cause);
                    removeAliasForId(key.toString());
                }
            })
            .build();
    }

    /**
     * Stores a GIS data object and returns its ID.
     */
    public String store(GisDataObject dataObject) {
        return store(dataObject, null);
    }

    /**
     * Stores a GIS data object with session association.
     */
    public String store(GisDataObject dataObject, String sessionId) {
        validateStorageLimit();
        
        String id = dataObject.getId();
        dataCache.put(id, dataObject);
        
        if (sessionId != null) {
            sessionObjects.computeIfAbsent(sessionId, k -> ConcurrentHashMap.newKeySet())
                .add(id);
        }
        
        log.debug("Stored data object: {} (session: {})", id, sessionId);
        return id;
    }

    /**
     * Creates and stores a new data object.
     */
    public String create(GisMetadata.DataType type, Object data) {
        return create(type, data, null);
    }

    /**
     * Creates and stores a new data object with session association.
     */
    public String create(GisMetadata.DataType type, Object data, String sessionId) {
        String id = idGenerator.generateDataId();
        GisMetadata metadata = new GisMetadata(id, type);
        metadata.setExpiresAt(Instant.now().plusSeconds(properties.getData().getDefaultTtl()));
        
        GisDataObject dataObject = new GisDataObject(id, metadata, data);
        store(dataObject, sessionId);
        
        return id;
    }

    /**
     * Retrieves a data object by ID or alias.
     */
    public Optional<GisDataObject> get(String idOrAlias) {
        String id = resolveId(idOrAlias);
        GisDataObject obj = dataCache.getIfPresent(id);
        
        if (obj != null && obj.getMetadata().isExpired()) {
            delete(id);
            return Optional.empty();
        }
        
        return Optional.ofNullable(obj);
    }

    /**
     * Checks if an object exists.
     */
    public boolean exists(String idOrAlias) {
        return get(idOrAlias).isPresent();
    }

    /**
     * Deletes a data object.
     */
    public boolean delete(String idOrAlias) {
        String id = resolveId(idOrAlias);
        GisDataObject removed = dataCache.getIfPresent(id);
        
        if (removed != null) {
            dataCache.invalidate(id);
            removeAliasForId(id);
            
            // Remove from sessions
            sessionObjects.values().forEach(ids -> ids.remove(id));
            
            log.debug("Deleted data object: {}", id);
            return true;
        }
        
        return false;
    }

    /**
     * Sets an alias for a data object.
     */
    public void setAlias(String id, String alias) {
        GisDataObject obj = dataCache.getIfPresent(id);
        if (obj != null) {
            // Remove old alias if exists
            String oldAlias = obj.getMetadata().getAlias();
            if (oldAlias != null) {
                aliasToId.remove(oldAlias);
            }
            
            // Set new alias
            obj.getMetadata().setAlias(alias);
            aliasToId.put(alias, id);
            
            log.debug("Set alias '{}' for object: {}", alias, id);
        }
    }

    /**
     * Updates the TTL for a data object.
     */
    public boolean setTtl(String idOrAlias, int seconds) {
        String id = resolveId(idOrAlias);
        GisDataObject obj = dataCache.getIfPresent(id);
        
        if (obj != null) {
            obj.getMetadata().setExpiresAt(Instant.now().plusSeconds(seconds));
            log.debug("Updated TTL for object: {} (expires in {} seconds)", id, seconds);
            return true;
        }
        
        return false;
    }

    /**
     * Lists all data objects.
     */
    public List<GisMetadata> listAll() {
        return dataCache.asMap().values().stream()
            .map(GisDataObject::getMetadata)
            .filter(m -> !m.isExpired())
            .collect(Collectors.toList());
    }

    /**
     * Lists data objects for a specific session.
     */
    public List<GisMetadata> listBySession(String sessionId) {
        Set<String> ids = sessionObjects.get(sessionId);
        if (ids == null) {
            return Collections.emptyList();
        }
        
        return ids.stream()
            .map(dataCache::getIfPresent)
            .filter(Objects::nonNull)
            .map(GisDataObject::getMetadata)
            .filter(m -> !m.isExpired())
            .collect(Collectors.toList());
    }

    /**
     * Clears all data objects for a session.
     */
    public int clearSession(String sessionId) {
        Set<String> ids = sessionObjects.remove(sessionId);
        if (ids == null) {
            return 0;
        }
        
        ids.forEach(this::delete);
        log.debug("Cleared session: {} ({} objects)", sessionId, ids.size());
        
        return ids.size();
    }

    /**
     * Clears all expired data objects.
     */
    public int clearExpired() {
        int count = 0;
        for (Map.Entry<String, GisDataObject> entry : dataCache.asMap().entrySet()) {
            if (entry.getValue().getMetadata().isExpired()) {
                delete(entry.getKey());
                count++;
            }
        }
        
        if (count > 0) {
            log.debug("Cleared {} expired objects", count);
        }
        
        return count;
    }

    /**
     * Clears the entire cache.
     */
    public long clearAll() {
        long count = dataCache.estimatedSize();
        dataCache.invalidateAll();
        aliasToId.clear();
        sessionObjects.clear();
        
        log.info("Cleared entire cache ({} objects)", count);
        return count;
    }

    /**
     * Gets cache statistics.
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalObjects", dataCache.estimatedSize());
        stats.put("sessionCount", sessionObjects.size());
        stats.put("aliasCount", aliasToId.size());
        
        long totalMemory = dataCache.asMap().values().stream()
            .mapToLong(GisDataObject::estimateMemorySize)
            .sum();
        stats.put("estimatedMemoryBytes", totalMemory);
        
        return stats;
    }

    /**
     * Scheduled task to clean up expired objects and validate memory limits.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void cleanupExpired() {
        clearExpired();
        validateMemoryLimit();
    }

    private String resolveId(String idOrAlias) {
        return aliasToId.getOrDefault(idOrAlias, idOrAlias);
    }

    private void removeAliasForId(String id) {
        aliasToId.entrySet().removeIf(entry -> entry.getValue().equals(id));
    }

    /**
     * Validates that cache size limit is not exceeded.
     * Called on store operations for lightweight validation.
     */
    private void validateStorageLimit() {
        if (dataCache.estimatedSize() >= properties.getCache().getMaxSize()) {
            log.warn("Cache size limit reached, oldest entries will be evicted");
        }
    }

    /**
     * Validates memory limit periodically (called by scheduled task).
     * Separated from per-operation validation for performance.
     */
    private void validateMemoryLimit() {
        long totalMemory = dataCache.asMap().values().stream()
            .mapToLong(GisDataObject::estimateMemorySize)
            .sum();
        
        if (totalMemory > properties.getData().getMaxMemoryUsage()) {
            log.error("Memory limit exceeded: {} bytes (limit: {} bytes). Consider clearing cache.",
                totalMemory, properties.getData().getMaxMemoryUsage());
            // Trigger cache cleanup instead of throwing exception
            dataCache.cleanUp();
        }
    }
}
