# Geometry MCP Server

> **📦 Maven Multi-Module Project** - This project now uses Maven multi-module structure. See [QUICKSTART.md](./QUICKSTART.md) for build instructions.

[English](#english) | [中文](#中文)

---

<a name="english"></a>
## 🌍 English

A **Model Context Protocol (MCP)** server for GIS operations, enabling AI agents to perform spatial analysis, data management, and coordinate transformations through a standardized protocol.

### ✨ Features

- 🗺️ **Spatial Analysis**: Buffer, intersect, union, spatial queries with OGC predicates
- 📤 **Data Management**: Upload, store, retrieve, and manage GIS data with session isolation
- 🔄 **Coordinate Transform**: CRS transformations using Proj4J (EPSG codes supported)
- 📊 **Data Export**: Export to GeoJSON, KML, Shapefile with thumbnail generation
- 🔌 **MCP Protocol**: Full JSON-RPC 2.0 compliance with HTTP and STDIO transports
- ⏱️ **Lifecycle Management**: Automatic TTL, reference counting, and garbage collection

### 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│              MCP Protocol Layer (通信层)                 │
│    HTTP REST Endpoint  |  STDIO Transport               │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│          MCP Server Core (协议处理层)                    │
│  ToolRegistry  |  RequestRouter  |  McpProtocolHandler  │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│          GIS Service Layer (业务逻辑层)                  │
│  DataManagementService  |  SpatialAnalysisService       │
│  CoordinateTransformService  |  DataExportService       │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│        Data Storage Layer (存储层)                       │
│  H2 Database (Metadata)  |  File System (GeoJSON)       │
└─────────────────────────────────────────────────────────┘
                          ↓↑
┌─────────────────────────────────────────────────────────┐
│          Core Libraries (核心库)                         │
│  JTS 1.19.0 (Geometry)  |  Proj4J 1.3.0 (CRS)          │
└─────────────────────────────────────────────────────────┘
```

### 🛠️ MCP Tools (13 Tools)

#### A. Data Management (数据管理)

| Tool | Description | Parameters |
|------|-------------|------------|
| `upload_data` | Upload GIS data | `data`, `format`, `type` |
| `list_data` | List session data | - |
| `describe_data` | Get data metadata | `data_id` |
| `delete_data` | Delete data | `data_id` |
| `fetch_result` | Fetch data content | `data_id`, `format`, `max_features` |

#### B. Spatial Analysis (空间分析)

| Tool | Description | Parameters |
|------|-------------|------------|
| `spatial_buffer` | Create buffer zones | `source_data_id`, `distance`, `units` |
| `spatial_intersect` | Compute intersection | `data_id_a`, `data_id_b` |
| `spatial_union` | Compute union | `data_ids` |
| `spatial_query` | Query by spatial predicate | `data_id`, `geometry`, `predicate` |
| `attribute_filter` | Filter by attributes | `data_id`, `expression` |

**Supported Spatial Predicates**: `INTERSECTS`, `WITHIN`, `CONTAINS`, `CROSSES`, `TOUCHES`, `OVERLAPS`, `DISJOINT`, `EQUALS`

**Supported Distance Units**: `meters`, `kilometers`, `miles`, `feet`, `degrees`

#### C. Coordinate Transform (坐标转换)

| Tool | Description | Parameters |
|------|-------------|------------|
| `transform_crs` | Transform CRS | `data_id`, `source_crs`, `target_crs` |

**Common EPSG Codes**: `EPSG:4326` (WGS84), `EPSG:3857` (Web Mercator), `EPSG:4490` (CGCS2000)

#### D. Export & Visualization (导出与可视化)

| Tool | Description | Parameters |
|------|-------------|------------|
| `generate_thumbnail` | Generate preview image | `data_id`, `width`, `height` |
| `export_data` | Export to file | `data_id`, `format`, `compression` |

**Supported Export Formats**: `GeoJSON`, `KML`, `Shapefile`

### 🚀 Quick Start

#### Prerequisites

- **Java 17+** (Required)
- **Maven 3.8+** (For building)

#### Build

```bash
# Clone repository
git clone https://github.com/znlgis/GeometryMcpServer.git
cd GeometryMcpServer

# Build with Maven
mvn clean package

# Skip tests (optional)
mvn clean package -DskipTests
```

#### Run

```bash
# HTTP mode (default, port 8080)
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar

# STDIO mode (for MCP client integration)
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar --mcp.transport=stdio

# Custom port
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar --server.port=9090
```

### 📡 API Reference

#### HTTP Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/mcp` | POST | MCP JSON-RPC endpoint |
| `/api/mcp/health` | GET | Health check |
| `/api/exports/{filename}` | GET | Download exported files |
| `/h2-console` | GET | H2 database console (dev) |

#### MCP Protocol Messages

##### 1. Initialize Session

```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "initialize",
  "params": {}
}
```

**Response:**
```json
{
  "jsonrpc": "2.0",
  "id": 1,
  "result": {
    "protocolVersion": "2024-11-05",
    "capabilities": { "tools": {} },
    "serverInfo": {
      "name": "geometry-mcp-server",
      "version": "1.0.0"
    },
    "sessionId": "550e8400-e29b-41d4-a716-446655440000"
  }
}
```

##### 2. List Available Tools

```json
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/list"
}
```

##### 3. Upload Data

```json
{
  "jsonrpc": "2.0",
  "id": 3,
  "method": "tools/call",
  "params": {
    "sessionId": "your-session-id",
    "name": "upload_data",
    "arguments": {
      "data": "{\"type\":\"FeatureCollection\",\"features\":[{\"type\":\"Feature\",\"geometry\":{\"type\":\"Point\",\"coordinates\":[116.4,39.9]},\"properties\":{\"name\":\"Beijing\"}}]}",
      "format": "GeoJSON",
      "type": "FEATURE_COLLECTION"
    }
  }
}
```

##### 4. Buffer Analysis

```json
{
  "jsonrpc": "2.0",
  "id": 4,
  "method": "tools/call",
  "params": {
    "sessionId": "your-session-id",
    "name": "spatial_buffer",
    "arguments": {
      "source_data_id": "data-uuid-from-upload",
      "distance": 1000,
      "units": "meters"
    }
  }
}
```

##### 5. Spatial Query

```json
{
  "jsonrpc": "2.0",
  "id": 5,
  "method": "tools/call",
  "params": {
    "sessionId": "your-session-id",
    "name": "spatial_query",
    "arguments": {
      "data_id": "data-uuid",
      "geometry": "POLYGON((116.3 39.8, 116.5 39.8, 116.5 40.0, 116.3 40.0, 116.3 39.8))",
      "predicate": "INTERSECTS"
    }
  }
}
```

### ⚙️ Configuration

Configuration in `src/main/resources/application.properties`:

```properties
# Server Configuration
server.port=8080
spring.application.name=geometry-mcp-server

# MCP Transport (http | stdio)
mcp.transport=http

# Storage Paths
gis.storage.path=./data
gis.export.path=./exports
gis.server.baseUrl=http://localhost:8080

# Database (H2 embedded)
spring.datasource.url=jdbc:h2:file:./data/gis-db
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

# JPA
spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
spring.jpa.hibernate.ddl-auto=update

# H2 Console (development)
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Logging
logging.level.com.github.znlgis.gis=INFO
```

### 📦 Data Lifecycle

Data follows a state machine for lifecycle management:

```
CREATED → ACTIVE → REFERENCED → EXPIRED → MARKED_FOR_DELETION → DELETED
    ↓         ↓          ↓            ↓              ↓
    └─────────┴──────────┴────────────┴──────────────┴→ ARCHIVED (optional)
```

| State | Description |
|-------|-------------|
| `CREATED` | Initial state after data upload |
| `ACTIVE` | Data is being actively used by tools |
| `REFERENCED` | Data is referenced by derived data (e.g., buffer result) |
| `EXPIRED` | Past TTL (24h default) with no active references |
| `MARKED_FOR_DELETION` | Scheduled for cleanup by background task |
| `DELETED` | Physically removed from storage |

**Lifecycle Features:**
- ⏰ **Default TTL**: 24 hours, auto-extended on access
- 🔗 **Reference Counting**: Derived data extends parent's lifecycle
- 🧹 **Auto Cleanup**: Hourly background task removes expired data
- 🔒 **Session Isolation**: Data is scoped to session, invisible to others

### 🧪 Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### 📁 Project Structure

```
src/main/java/com/github/znlgis/gis/
├── GeometryMcpServerApplication.java   # Main entry point
├── config/                              # Spring configurations
│   └── SchedulingConfig.java           # Scheduled task config
├── controller/                          # REST controllers
│   ├── McpController.java              # MCP HTTP endpoint
│   └── ExportController.java           # Export file download
├── mcp/                                 # MCP protocol layer
│   ├── ToolRegistry.java               # Tool discovery & registration
│   ├── RequestRouter.java              # Request routing
│   ├── protocol/                       # Protocol handling
│   │   ├── McpProtocolHandler.java     # JSON-RPC handler
│   │   └── StdioTransport.java         # STDIO transport
│   └── tool/                           # MCP tools (13 tools)
│       ├── McpTool.java                # Tool interface
│       ├── UploadDataTool.java
│       ├── SpatialBufferTool.java
│       └── ...
├── model/                               # Domain models
│   ├── DataReference.java              # Data reference entity
│   ├── Session.java                    # Session entity
│   ├── DataMetadata.java               # Metadata model
│   └── enums/                          # Enumerations
│       ├── DataType.java               # VECTOR, RASTER, FEATURE_COLLECTION
│       ├── DataState.java              # Lifecycle states
│       ├── SessionState.java           # Session states
│       └── SpatialPredicate.java       # OGC spatial predicates
├── repository/                          # JPA repositories
│   ├── DataReferenceRepository.java
│   └── SessionRepository.java
├── service/                             # Business services
│   ├── DataManagementService.java
│   ├── SpatialAnalysisService.java
│   ├── CoordinateTransformService.java
│   ├── DataExportService.java
│   ├── SessionService.java
│   └── impl/                           # Service implementations
└── util/                                # Utilities
    └── GeoJsonUtils.java               # GeoJSON parsing utilities
```

### 🔗 Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Spring Boot | 3.2.0 | Application framework |
| JTS | 1.19.0 | Geometry operations |
| Proj4J | 1.3.0 | CRS transformations |
| H2 Database | (managed) | Embedded database |
| Jackson | (managed) | JSON processing |
| Lombok | (managed) | Code generation |

---

<a name="中文"></a>
## 🌍 中文

一个用于 GIS 操作的 **模型上下文协议 (MCP)** 服务器，使 AI 代理能够通过标准化协议执行空间分析、数据管理和坐标转换。

### ✨ 特性

- 🗺️ **空间分析**：缓冲区、交集、并集、基于 OGC 谓词的空间查询
- 📤 **数据管理**：上传、存储、检索和管理 GIS 数据，支持会话隔离
- 🔄 **坐标转换**：使用 Proj4J 进行坐标参考系统转换（支持 EPSG 代码）
- 📊 **数据导出**：导出为 GeoJSON、KML、Shapefile，支持缩略图生成
- 🔌 **MCP 协议**：完全兼容 JSON-RPC 2.0，支持 HTTP 和 STDIO 传输
- ⏱️ **生命周期管理**：自动 TTL、引用计数和垃圾回收

### 🛠️ MCP 工具（13 个工具）

#### A. 数据管理工具

| 工具 | 描述 | 参数 |
|------|------|------|
| `upload_data` | 上传 GIS 数据 | `data`, `format`, `type` |
| `list_data` | 列出会话数据 | - |
| `describe_data` | 获取数据元信息 | `data_id` |
| `delete_data` | 删除数据 | `data_id` |
| `fetch_result` | 获取数据内容 | `data_id`, `format`, `max_features` |

#### B. 空间分析工具

| 工具 | 描述 | 参数 |
|------|------|------|
| `spatial_buffer` | 创建缓冲区 | `source_data_id`, `distance`, `units` |
| `spatial_intersect` | 计算交集 | `data_id_a`, `data_id_b` |
| `spatial_union` | 计算并集 | `data_ids` |
| `spatial_query` | 空间谓词查询 | `data_id`, `geometry`, `predicate` |
| `attribute_filter` | 属性过滤 | `data_id`, `expression` |

**支持的空间谓词**：`INTERSECTS`（相交）、`WITHIN`（在内）、`CONTAINS`（包含）、`CROSSES`（交叉）、`TOUCHES`（接触）、`OVERLAPS`（重叠）、`DISJOINT`（不相交）、`EQUALS`（相等）

#### C. 坐标转换工具

| 工具 | 描述 | 参数 |
|------|------|------|
| `transform_crs` | 坐标系转换 | `data_id`, `source_crs`, `target_crs` |

**常用 EPSG 代码**：`EPSG:4326`（WGS84）、`EPSG:3857`（Web 墨卡托）、`EPSG:4490`（CGCS2000）

#### D. 导出与可视化工具

| 工具 | 描述 | 参数 |
|------|------|------|
| `generate_thumbnail` | 生成预览图 | `data_id`, `width`, `height` |
| `export_data` | 导出文件 | `data_id`, `format`, `compression` |

### 🚀 快速开始

#### 环境要求

- **Java 17+**（必需）
- **Maven 3.8+**（用于构建）

#### 构建

```bash
# 克隆仓库
git clone https://github.com/znlgis/GeometryMcpServer.git
cd GeometryMcpServer

# 使用 Maven 构建
mvn clean package

# 跳过测试（可选）
mvn clean package -DskipTests
```

#### 运行

```bash
# HTTP 模式（默认，端口 8080）
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar

# STDIO 模式（用于 MCP 客户端集成）
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar --mcp.transport=stdio

# 自定义端口
java -jar target/geometry-mcp-server-1.0.0-SNAPSHOT.jar --server.port=9090
```

### 📡 典型工作流示例

**场景**：找出城市边界内距离河流 500 米范围内的所有建筑物

```
1. upload_data → {city_boundary_id}      # 上传城市边界
2. upload_data → {river_id}              # 上传河流数据
3. upload_data → {buildings_id}          # 上传建筑物数据
4. spatial_buffer(river_id, 500m) → {river_buffer_id}  # 创建河流缓冲区
5. spatial_intersect(city_boundary_id, river_buffer_id) → {analysis_zone_id}  # 计算分析区域
6. spatial_query(buildings_id, analysis_zone_id, WITHIN) → {result_buildings_id}  # 查询建筑物
7. fetch_result(result_buildings_id, format="summary") → 
   {count: 247, total_area: 125000m², preview_geojson: {...}}  # 获取结果
```

### 📦 数据生命周期

```
CREATED → ACTIVE → REFERENCED → EXPIRED → MARKED_FOR_DELETION → DELETED
```

| 状态 | 描述 |
|------|------|
| `CREATED` | 数据上传后的初始状态 |
| `ACTIVE` | 数据正在被工具使用 |
| `REFERENCED` | 数据被派生数据引用（如缓冲区结果） |
| `EXPIRED` | 超过 TTL（默认 24 小时）且无活跃引用 |
| `MARKED_FOR_DELETION` | 被后台任务标记为待删除 |
| `DELETED` | 从存储中物理删除 |

**生命周期特性**：
- ⏰ **默认 TTL**：24 小时，访问时自动延期
- 🔗 **引用计数**：派生数据延长父数据生命周期
- 🧹 **自动清理**：每小时后台任务清理过期数据
- 🔒 **会话隔离**：数据作用域限于会话，对其他会话不可见

### 📄 许可证

MIT License - 详见 [LICENSE](LICENSE) 文件

---

## 🤝 Contributing

欢迎贡献代码！请阅读贡献指南后提交 Pull Request。

Contributions are welcome! Please read the contributing guidelines before submitting a Pull Request.
