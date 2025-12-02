# Geometry MCP Server

A Model Context Protocol (MCP) server for GIS operations, enabling AI agents to perform spatial analysis, data management, and coordinate transformations.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│              MCP Protocol Layer (通信层)                 │
│    (stdio/HTTP/SSE endpoint adapters)                   │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│          MCP Server Core (协议处理层)                    │
│  - Tool Registry & Discovery                            │
│  - Request Router & Validator                           │
│  - Session Manager                                      │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│          GIS Service Layer (业务逻辑层)                  │
│  - Data Management Service                              │
│  - Spatial Analysis Service                             │
│  - Coordinate Transform Service                         │
│  - Data Export Service                                  │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│        Data Storage & Cache Layer (存储层)              │
│  - Metadata Store (H2/PostgreSQL)                       │
│  - Data Object Store (File System)                      │
│  - Session Cache                                        │
└─────────────────────────────────────────────────────────┘
```

## Features

### Data Management Tools
- **upload_data** - Upload GIS data (GeoJSON, Shapefile, etc.)
- **list_data** - List all data in the current session
- **describe_data** - Get detailed metadata about data
- **delete_data** - Delete data from the session
- **fetch_result** - Fetch data content with formatting options

### Spatial Analysis Tools
- **spatial_buffer** - Create buffer zones around features
- **spatial_intersect** - Compute intersection of two datasets
- **spatial_union** - Compute union of multiple datasets
- **spatial_query** - Query features using spatial predicates
- **attribute_filter** - Filter features by attribute expression

### Coordinate Transform Tools
- **transform_crs** - Transform data between coordinate systems

### Export Tools
- **generate_thumbnail** - Generate preview images
- **export_data** - Export to GeoJSON, KML, Shapefile

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Build
```bash
mvn clean package
```

### Run (HTTP mode)
```bash
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar
```

### Run (STDIO mode)
```bash
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar --mcp.transport=stdio
```

## API Usage

### Initialize Session
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}
```

### List Tools
```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}
```

### Call Tool (Example: Buffer Analysis)
```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "sessionId": "your-session-id",
    "name": "spatial_buffer",
    "arguments": {
      "source_data_id": "data-uuid",
      "distance": 100,
      "units": "meters"
    }
  }
}
```

## Configuration

Configuration can be set in `application.properties`:

```properties
# Server port
server.port=8080

# MCP transport (http or stdio)
mcp.transport=http

# Storage paths
gis.storage.path=./data
gis.export.path=./exports
```

## Data Lifecycle Management

Data follows this state machine:
```
CREATED → ACTIVE → REFERENCED → EXPIRED → MARKED_FOR_DELETION → DELETED
```

- **CREATED**: Initial state after upload
- **ACTIVE**: Being used by tools
- **REFERENCED**: Referenced by derived data
- **EXPIRED**: Past TTL with no references
- **MARKED_FOR_DELETION**: Scheduled for cleanup
- **DELETED**: Physically removed

Default TTL is 24 hours, automatically extended on access.

## License

MIT License - see LICENSE file for details
