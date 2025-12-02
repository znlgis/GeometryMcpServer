package org.opengis.mcp.core;

import org.locationtech.jts.geom.Geometry;

import java.util.function.Supplier;

/**
 * Represents a GIS data object with lazy loading capability.
 * Follows the ID-driven design principle for efficient AI interactions.
 */
public class GisDataObject {

    private final String id;
    private final GisMetadata metadata;
    private Supplier<Object> dataLoader;
    private Object cachedData;
    private boolean loaded = false;

    public GisDataObject(String id, GisMetadata metadata) {
        this.id = id;
        this.metadata = metadata;
    }

    public GisDataObject(String id, GisMetadata metadata, Object data) {
        this.id = id;
        this.metadata = metadata;
        this.cachedData = data;
        this.loaded = true;
    }

    public GisDataObject(String id, GisMetadata metadata, Supplier<Object> dataLoader) {
        this.id = id;
        this.metadata = metadata;
        this.dataLoader = dataLoader;
    }

    public String getId() {
        return id;
    }

    public GisMetadata getMetadata() {
        return metadata;
    }

    /**
     * Gets the data, loading it lazily if necessary.
     */
    public synchronized Object getData() {
        if (!loaded && dataLoader != null) {
            cachedData = dataLoader.get();
            loaded = true;
        }
        return cachedData;
    }

    /**
     * Sets the data directly.
     */
    public synchronized void setData(Object data) {
        this.cachedData = data;
        this.loaded = true;
    }

    /**
     * Returns the data as a Geometry if applicable.
     */
    public Geometry getAsGeometry() {
        Object data = getData();
        if (data instanceof Geometry) {
            return (Geometry) data;
        }
        throw new IllegalStateException("Data is not a Geometry: " + 
            (data != null ? data.getClass().getName() : "null"));
    }

    /**
     * Returns whether the data is currently loaded.
     */
    public boolean isLoaded() {
        return loaded;
    }

    /**
     * Unloads the cached data to free memory.
     */
    public synchronized void unload() {
        if (dataLoader != null) {
            cachedData = null;
            loaded = false;
        }
    }

    /**
     * Estimates the memory footprint of this object.
     */
    public long estimateMemorySize() {
        if (!loaded || cachedData == null) {
            return 0;
        }
        // Rough estimate based on data type
        if (cachedData instanceof Geometry) {
            return ((Geometry) cachedData).getNumPoints() * 16L; // ~16 bytes per coordinate
        }
        return metadata.getSizeInBytes();
    }
}
