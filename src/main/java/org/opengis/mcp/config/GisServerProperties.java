package org.opengis.mcp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for the GIS MCP Server.
 */
@Component
@ConfigurationProperties(prefix = "gis")
public class GisServerProperties {

    private McpConfig mcp = new McpConfig();
    private DataConfig data = new DataConfig();
    private StorageConfig storage = new StorageConfig();
    private CacheConfig cache = new CacheConfig();
    private SecurityConfig security = new SecurityConfig();

    public McpConfig getMcp() {
        return mcp;
    }

    public void setMcp(McpConfig mcp) {
        this.mcp = mcp;
    }

    public DataConfig getData() {
        return data;
    }

    public void setData(DataConfig data) {
        this.data = data;
    }

    public StorageConfig getStorage() {
        return storage;
    }

    public void setStorage(StorageConfig storage) {
        this.storage = storage;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    public SecurityConfig getSecurity() {
        return security;
    }

    public void setSecurity(SecurityConfig security) {
        this.security = security;
    }

    public static class McpConfig {
        private ServerConfig server = new ServerConfig();

        public ServerConfig getServer() {
            return server;
        }

        public void setServer(ServerConfig server) {
            this.server = server;
        }

        public static class ServerConfig {
            private String name = "gis-mcp-server";
            private String version = "1.0.0";
            private String description = "GIS MCP Server for AI-powered spatial analysis";

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getVersion() {
                return version;
            }

            public void setVersion(String version) {
                this.version = version;
            }

            public String getDescription() {
                return description;
            }

            public void setDescription(String description) {
                this.description = description;
            }
        }
    }

    public static class DataConfig {
        private long maxObjectSize = 104857600L; // 100MB
        private int maxObjectsPerSession = 50;
        private int defaultTtl = 3600; // 1 hour
        private long maxMemoryUsage = 2147483648L; // 2GB

        public long getMaxObjectSize() {
            return maxObjectSize;
        }

        public void setMaxObjectSize(long maxObjectSize) {
            this.maxObjectSize = maxObjectSize;
        }

        public int getMaxObjectsPerSession() {
            return maxObjectsPerSession;
        }

        public void setMaxObjectsPerSession(int maxObjectsPerSession) {
            this.maxObjectsPerSession = maxObjectsPerSession;
        }

        public int getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(int defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public long getMaxMemoryUsage() {
            return maxMemoryUsage;
        }

        public void setMaxMemoryUsage(long maxMemoryUsage) {
            this.maxMemoryUsage = maxMemoryUsage;
        }
    }

    public static class StorageConfig {
        private String baseDirectory = System.getProperty("java.io.tmpdir") + "/gis-mcp-server";
        private List<String> allowedPaths = List.of(
            System.getProperty("java.io.tmpdir"),
            System.getProperty("user.home") + "/gis-data"
        );

        public String getBaseDirectory() {
            return baseDirectory;
        }

        public void setBaseDirectory(String baseDirectory) {
            this.baseDirectory = baseDirectory;
        }

        public List<String> getAllowedPaths() {
            return allowedPaths;
        }

        public void setAllowedPaths(List<String> allowedPaths) {
            this.allowedPaths = allowedPaths;
        }
    }

    public static class CacheConfig {
        private int maxSize = 1000;
        private int ttl = 3600;

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getTtl() {
            return ttl;
        }

        public void setTtl(int ttl) {
            this.ttl = ttl;
        }
    }

    public static class SecurityConfig {
        private int operationTimeout = 60;
        private boolean enableAudit = true;

        public int getOperationTimeout() {
            return operationTimeout;
        }

        public void setOperationTimeout(int operationTimeout) {
            this.operationTimeout = operationTimeout;
        }

        public boolean isEnableAudit() {
            return enableAudit;
        }

        public void setEnableAudit(boolean enableAudit) {
            this.enableAudit = enableAudit;
        }
    }
}
