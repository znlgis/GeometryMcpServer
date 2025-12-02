# GeometryMcpServer

A GIS MCP (Model Context Protocol) Server that provides AI-powered spatial analysis capabilities. This server enables AI assistants like Claude to perform complex GIS operations through a standardized protocol.

## 🌟 Features

- **ID-driven Architecture**: All GIS objects use unique IDs to minimize token consumption
- **Lazy Loading**: Data is loaded only when needed
- **Rich GIS Operations**: Buffer, intersection, union, difference, and more
- **Multiple Formats**: Support for WKT and GeoJSON
- **Session Management**: Automatic cleanup and TTL-based expiration
- **CRS Metadata**: Coordinate Reference System support

## 🏗️ Architecture

```
AI Client <---> MCP Server <---> GIS Data Management <---> Storage
            (Lightweight)      (Heavy Computation)     (Persistence)
```

### Layers

1. **MCP Protocol Layer**: Standard MCP protocol implementation with STDIO transport
2. **GIS Service Layer**: Core GIS capabilities with JTS Topology Suite
3. **Data Management Layer**: Session management, caching, ID generation
4. **Storage Layer**: In-memory caching with Caffeine

## 🛠️ Available Tools

### Data Loading
- `load_wkt_geometry` - Load geometry from WKT format
- `load_geojson` - Load geometry from GeoJSON format

### Spatial Analysis
- `buffer_geometry` - Create buffer around geometry
- `intersect` - Compute intersection of two geometries
- `union` - Compute union of two geometries
- `difference` - Compute difference of two geometries
- `convex_hull` - Compute convex hull
- `centroid` - Compute centroid

### Query Operations
- `get_metadata` - Get metadata for a data object
- `query_by_bbox` - Query by bounding box
- `check_spatial_relation` - Check spatial relationships (intersects, contains, within, etc.)
- `calculate_distance` - Calculate distance between geometries
- `calculate_area` - Calculate geometry area
- `calculate_length` - Calculate geometry length/perimeter

### Transformation
- `transform_crs` - Update CRS metadata
- `simplify_geometry` - Simplify using Douglas-Peucker

### Export
- `export_to_geojson` - Export as GeoJSON
- `export_to_wkt` - Export as WKT
- `export_summary` - Get geometry summary

### Session Management
- `list_data_objects` - List all objects
- `delete_data_object` - Delete an object
- `set_alias` - Set alias for easier reference
- `set_ttl` - Update time-to-live
- `clear_cache` - Clear all cached data
- `get_cache_statistics` - Get cache stats

## 🚀 Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.8+

### Build

```bash
mvn clean package
```

### Run

```bash
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar
```

### Configure with Claude Desktop

Add to your Claude Desktop configuration (`claude_desktop_config.json`):

```json
{
  "mcpServers": {
    "gis-mcp-server": {
      "command": "java",
      "args": ["-jar", "/path/to/geometry-mcp-server-1.0.0-SNAPSHOT.jar"]
    }
  }
}
```

## 📖 Usage Examples

### Basic Workflow

```
User: "Create a buffer of 100 meters around this point: POINT(116.4 39.9)"

AI uses tools:
1. load_wkt_geometry("POINT(116.4 39.9)") → geom_20251202_001
2. buffer_geometry("geom_20251202_001", 100) → geom_20251202_002
3. export_summary("geom_20251202_002") → summary with area, bounds, etc.
```

### Spatial Analysis

```
User: "Check if polygon A contains polygon B"

AI uses tools:
1. load_wkt_geometry(polygonA) → geom_20251202_001
2. load_wkt_geometry(polygonB) → geom_20251202_002
3. check_spatial_relation("geom_20251202_001", "geom_20251202_002", "contains") → true/false
```

### Geometry Operations

```
User: "Find the area of the intersection of two polygons"

AI uses tools:
1. load_wkt_geometry(poly1) → geom_20251202_001
2. load_wkt_geometry(poly2) → geom_20251202_002
3. intersect("geom_20251202_001", "geom_20251202_002") → geom_20251202_003
4. calculate_area("geom_20251202_003") → area value
```

## ⚙️ Configuration

Configuration is in `src/main/resources/application.yml`:

```yaml
gis:
  mcp:
    server:
      name: gis-mcp-server
      version: 1.0.0
  
  data:
    max-object-size: 104857600  # 100MB
    max-objects-per-session: 50
    default-ttl: 3600
    max-memory-usage: 2147483648  # 2GB
  
  cache:
    max-size: 1000
    ttl: 3600
  
  security:
    operation-timeout: 60
    enable-audit: true
```

## 🔧 Technology Stack

- **Spring Boot 3.3** - Application framework
- **MCP Java SDK 0.8.1** - MCP protocol implementation
- **JTS Topology Suite 1.19** - Geometry operations
- **Caffeine 3.1** - High-performance caching
- **Jackson** - JSON serialization

## 📁 Project Structure

```
src/
├── main/
│   ├── java/org/opengis/mcp/
│   │   ├── config/          # Configuration classes
│   │   ├── core/            # Core data models (GisDataObject, GisMetadata, etc.)
│   │   ├── data/            # Data management (SessionManager)
│   │   ├── service/         # GIS service layer
│   │   ├── storage/         # Storage layer
│   │   └── tools/           # MCP tool definitions
│   └── resources/
│       └── application.yml  # Configuration
└── test/
    └── java/org/opengis/mcp/ # Unit tests
```

## 📝 License

MIT License - see [LICENSE](LICENSE) file.
