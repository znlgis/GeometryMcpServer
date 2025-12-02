package org.opengis.mcp.core;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Generates unique IDs for GIS data objects.
 * Format: gis_{yyyyMMdd}_{sequence}
 */
@Component
public class IdGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final AtomicLong sequence = new AtomicLong(0);
    private String currentDatePrefix;
    private final Object lock = new Object();

    public IdGenerator() {
        this.currentDatePrefix = LocalDate.now().format(DATE_FORMATTER);
    }

    /**
     * Generates a new unique ID for a data object.
     */
    public String generateId() {
        return generateId("gis");
    }

    /**
     * Generates a new unique ID with a custom prefix.
     */
    public String generateId(String prefix) {
        synchronized (lock) {
            String today = LocalDate.now().format(DATE_FORMATTER);
            if (!today.equals(currentDatePrefix)) {
                currentDatePrefix = today;
                sequence.set(0);
            }
            long seq = sequence.incrementAndGet();
            return String.format("%s_%s_%03d", prefix, currentDatePrefix, seq);
        }
    }

    /**
     * Generates an ID for a specific type of object.
     */
    public String generateGeometryId() {
        return generateId("geom");
    }

    public String generateFeatureId() {
        return generateId("feat");
    }

    public String generateLayerId() {
        return generateId("layer");
    }

    public String generateDataId() {
        return generateId("data");
    }

    /**
     * Resets the sequence counter (mainly for testing).
     */
    public void reset() {
        synchronized (lock) {
            sequence.set(0);
            currentDatePrefix = LocalDate.now().format(DATE_FORMATTER);
        }
    }
}
