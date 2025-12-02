package com.github.znlgis.gis.model;

import java.time.Instant;

/**
 * Record for tracking operation history within a session.
 */
public record OperationLog(
    Instant timestamp,
    String tool,
    String inputs,
    String outputDataId,
    long durationMs
) {
    public static OperationLog create(String tool, String inputs, String outputDataId, long durationMs) {
        return new OperationLog(Instant.now(), tool, inputs, outputDataId, durationMs);
    }
}
