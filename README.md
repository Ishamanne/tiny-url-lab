# Agentic Software Engineering System - URL Shortener

A **complete demonstration** of an AI-driven SDLC orchestration system that automates software development from requirements to deployment. This project showcases how AI agents can intelligently manage the full software engineering lifecycle with governance, approval gates, and human oversight.

## 🎯 Assignment Context

This project fulfills the **Agentic Software Engineering System** assignment requirements, demonstrating:
- **Workflow Orchestration**: DAG-based execution engine with parallel task scheduling
- **Three Scenarios**: Greenfield (build from scratch), Brownfield (enhance existing), Ambiguous (clarify vague requirements)
- **Governance Controls**: Entry/exit gates, human approval checkpoints, policy guardrails
- **Production-Quality Output**: Working URL shortener service with 33 Java classes, 31+ tests, comprehensive documentation
- **REST API**: Orchestration endpoints for triggering workflows, inspecting status, approving gates, and retrieving metrics

> **Note on Agent Execution:** This prototype uses **deterministic simulation** for agent tasks to enable local reproducibility without requiring LLM API keys. The orchestration architecture is production-ready and includes clear integration patterns for plugging in live LLM providers (Claude, GPT-4, etc.). See [ENGINEERING_SUMMARY.md](ENGINEERING_SUMMARY.md#agent-execution-model) for details.

## Features

### Core Functionality
- **URL Shortening**: Create short URLs from long URLs with custom or auto-generated codes
- **URL Resolution**: Fast redirect to original URLs with analytics tracking
- **Analytics**: Track clicks with device, country, and timestamp information
- **URL Management**: List, retrieve, and delete short URLs
- **Expiration Support**: Time-based URL expiration

### Production Features
- **Caching**: Spring Cache with @Cacheable for improved performance
- **Metrics**: Spring Actuator + Prometheus for monitoring and observability
- **Security**: Role-based access control (USER/ADMIN) with HTTP Basic Auth
- **API Documentation**: Interactive Swagger UI with OpenAPI 3.0 spec
- **Validation**: Input validation and comprehensive error handling
- **Database**: H2 in-memory database with JPA/Hibernate

## 🏗️ Architecture Overview

This project contains **two independent systems**:

### 1️⃣ **URL Shortener Application** (The Product)
- **Purpose:** Production-ready URL shortening service
- **Endpoints:** `/api/shorten`, `/api/urls/*`
- **Controller:** `UrlShortenerController`
- **For:** End users creating/managing short URLs
- **Auth:** USER (read) / ADMIN (write) roles

### 2️⃣ **Orchestration System** (The Assignment Requirement)
- **Purpose:** AI-driven SDLC workflow automation (meta-layer)
- **Endpoints:** `/api/orchestration/*`
- **Controller:** `OrchestrationController`
- **For:** Developers/reviewers testing agentic workflows
- **Auth:** ADMIN only
- **Note:** This is NOT needed to use the URL shortener - it's the system that would BUILD applications like the URL shortener

> **Important:** The orchestration API demonstrates how AI agents would automate building/enhancing applications like the URL shortener. End users never interact with it - only developers/DevOps teams testing the agentic SDLC automation.

### 📊 Visual Separation

```
┌─────────────────────────────────────────────────────────────────┐
│                     URL SHORTENER APPLICATION                    │
│                         (The Product)                            │
├─────────────────────────────────────────────────────────────────┤
│  Controller: UrlShortenerController                             │
│  Endpoints:  /api/shorten, /api/urls/*                          │
│  Users:      End users creating/using short URLs                │
│  Purpose:    Production URL shortening service                  │
└─────────────────────────────────────────────────────────────────┘
                              ↕️
                    (completely independent)
                              ↕️
┌─────────────────────────────────────────────────────────────────┐
│                   ORCHESTRATION SYSTEM                           │
│              (AI-Driven SDLC Automation Layer)                   │
├─────────────────────────────────────────────────────────────────┤
│  Controller: OrchestrationController                            │
│  Endpoints:  /api/orchestration/*                               │
│  Users:      Developers testing agentic workflows               │
│  Purpose:    Demonstrates how AI agents BUILD apps like above   │
└─────────────────────────────────────────────────────────────────┘
```

**Example Workflow:**
- **End User:** `POST /api/shorten` → Creates short URL ✅
- **Developer/Reviewer:** `POST /api/orchestration/run/brownfield` → AI agents enhance URL shortener (meta-operation) ✅

---

## Quick Start

### Prerequisites
- Java 21+
- Gradle 8.5+

### Running the Application

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Ishamanne/tiny-url-lab.git
   cd tiny-url-lab
   ```

2. **Build the project**:
   ```bash
   ./gradlew build
   ```

3. **Run the application**:
   ```bash
   ./gradlew bootRun
   ```

4. **Access the application**:
   - Application: `http://localhost:8080`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - H2 Console: `http://localhost:8080/h2-console`
     - JDBC URL: `jdbc:h2:mem:tinylinkdb`
     - Username: `sa`
     - Password: (empty)

## Authentication & Authorization

The application uses HTTP Basic Authentication with role-based access control:

### Users
- **User Account**: `user:user123` (Role: USER)
  - Can view all URLs and analytics
  - Cannot create or delete URLs

- **Admin Account**: `admin:admin123` (Roles: ADMIN, USER)
  - Full access to all operations
  - Can create, view, and delete URLs

### Endpoint Security

#### **URL Shortener Endpoints** (End-user facing)

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/{shortCode}` | GET | Public | Redirect to original URL (no auth required) |
| `/api/urls` | GET | USER, ADMIN | List all short URLs |
| `/api/urls/{shortCode}` | GET | USER, ADMIN | Get short URL details |
| `/api/urls/{shortCode}/analytics` | GET | USER, ADMIN | Get analytics data |
| `/api/shorten` | POST | ADMIN | Create new short URL |
| `/api/urls/{shortCode}` | PATCH | ADMIN | Update short URL expiration |
| `/api/urls` | DELETE | ADMIN | Delete short URL |
| `/swagger-ui/**` | GET | Public | Swagger UI documentation |
| `/actuator/**` | GET | Public | Metrics and health endpoints |

#### **Orchestration Endpoints** (Developer/reviewer facing - NOT for URL shortening)

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/api/orchestration/run/{scenario}` | POST | ADMIN | Trigger AI workflow scenario |
| `/api/orchestration/status/{workflowId}` | GET | ADMIN | Get workflow execution status |
| `/api/orchestration/approve/{workflowId}/{taskId}` | POST | ADMIN | Approve HITL gate |
| `/api/orchestration/metrics` | GET | ADMIN | Get workflow reliability metrics |
| `/api/orchestration/scenarios` | GET | ADMIN | List all workflow scenarios |
| `/api/orchestration/workflows` | GET | ADMIN | List all workflow executions |

> ⚠️ **For Reviewers:** The `/api/orchestration/*` endpoints are for testing the agentic SDLC automation system (the assignment requirement). They are NOT part of the URL shortener functionality.

## API Usage Examples

### Create Short URL (ADMIN only)
```bash
curl -X POST http://localhost:8080/api/shorten \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://example.com/very/long/url",
    "customShortCode": "mylink",
    "expiresAt": "2025-12-31T23:59:59"
  }'
```

**Response**:
```json
{
  "shortCode": "mylink",
  "shortUrl": "http://localhost:8080/mylink",
  "originalUrl": "https://example.com/very/long/url",
  "clickCount": 0,
  "active": true
}
```

### Get All URLs (USER or ADMIN)
```bash
curl -X GET http://localhost:8080/api/urls \
  -u user:user123
```

### Get URL Details (USER or ADMIN)
```bash
curl -X GET http://localhost:8080/api/urls/mylink \
  -u user:user123
```

### Get Analytics (USER or ADMIN)
```bash
curl -X GET http://localhost:8080/api/urls/mylink/analytics \
  -u user:user123
```

**Response**:
```json
{
  "shortCode": "mylink",
  "totalClicks": 42,
  "clicksByCountry": {
    "US": 25,
    "UK": 10,
    "CA": 7
  },
  "clicksByDevice": {
    "mobile": 30,
    "desktop": 12
  },
  "recentClicks": [
    {
      "timestamp": "2024-01-15T10:30:00",
      "country": "US",
      "device": "mobile"
    }
  ]
}
```

### Update URL (ADMIN only)
```bash
curl -X PATCH http://localhost:8080/api/urls/mylink \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "expiresInMinutes": 60
  }'
```

### Delete URL (ADMIN only)
```bash
curl -X DELETE http://localhost:8080/api/urls \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{
    "shortCode": "mylink"
  }'
```

### Redirect to Original URL (Public)
```bash
# In browser or with curl -L
curl -L http://localhost:8080/mylink
```

## Monitoring & Metrics

The application exposes metrics through Spring Actuator:

### Available Endpoints
- **Health**: `http://localhost:8080/actuator/health`
- **All Metrics**: `http://localhost:8080/actuator/metrics`
- **Prometheus**: `http://localhost:8080/actuator/prometheus`

### Custom Metrics
The application tracks custom latency metrics for all endpoints:
- `url.shorten` - POST /api/shorten (Create short URL)
- `url.getAll` - GET /api/urls (Get all URLs)
- `url.getOne` - GET /api/urls/{shortCode} (Get single URL)
- `url.analytics` - GET /api/urls/{shortCode}/analytics (Get analytics)
- `url.update` - PATCH /api/urls/{shortCode} (Update URL)
- `url.delete` - DELETE /api/urls (Delete URL)
- `url.redirect` - GET /{shortCode} (Redirect)

### Prometheus Metrics
Access Prometheus-formatted metrics at `http://localhost:8080/actuator/prometheus`:
- `url_shorten_seconds_count` - Total number of short URL creations
- `url_shorten_seconds_sum` - Total time spent creating short URLs
- `url_shorten_seconds_max` - Maximum time for a single creation
- `url_getAll_seconds_*` - Metrics for listing all URLs
- `url_getOne_seconds_*` - Metrics for getting single URL
- `url_analytics_seconds_*` - Metrics for analytics requests
- `url_update_seconds_*` - Metrics for URL updates
- `url_delete_seconds_*` - Metrics for URL deletions
- `url_redirect_seconds_*` - Metrics for redirects

Each metric includes labels: `application`, `class`, `method`, and `exception`

### View Metrics
```bash
# View all available metrics
curl http://localhost:8080/actuator/metrics

# View specific metric
curl http://localhost:8080/actuator/metrics/url.shorten

# View HTTP request metrics with percentiles
curl http://localhost:8080/actuator/metrics/http.server.requests

# Export to Prometheus
curl http://localhost:8080/actuator/prometheus
```

### Metrics Configuration
The application is configured with:
- Percentiles: p50, p75, p95, p99
- SLO buckets: 10ms, 50ms, 100ms, 200ms, 500ms, 1s, 2s
- Histogram support for latency distribution

## Caching

The application uses Spring Cache to improve performance:

### Cached Operations
- `getAllUrls()` - Cached with key: `shortUrls`
- `getShortUrl(shortCode)` - Cached with key: `shortUrl:{shortCode}`
- `getAnalytics(shortCode)` - Cached with key: `analytics:{shortCode}`

### Cache Invalidation
Caches are automatically invalidated when:
- A new URL is created (evicts `shortUrls` and `shortUrl` caches)
- A URL is deleted (evicts all caches)

## Project Structure

```
src/main/java/com/example/tinyurllab/
├── TinyUrlLabApplication.java          # Main application entry point
├── config/
│   ├── MetricsConfig.java             # Micrometer @Timed aspect configuration
│   ├── OpenApiConfig.java             # Swagger/OpenAPI configuration
│   └── SecurityConfig.java            # Spring Security configuration
└── urlshortener/
    ├── controller/
    │   └── UrlShortenerController.java # REST API endpoints
    ├── service/
    │   └── UrlShortenerService.java    # Business logic
    ├── repository/
    │   ├── UrlRepository.java          # URL data access
    │   └── AnalyticsRepository.java    # Analytics data access
    ├── model/
    │   ├── ShortUrl.java              # URL entity
    │   └── ClickEvent.java            # Analytics entity
    └── dto/
        ├── CreateShortUrlRequest.java  # Request DTOs
        ├── ShortUrlResponse.java       # Response DTOs
        └── AnalyticsResponse.java      # Analytics DTOs
```

## Configuration

Key configuration in `application.properties`:

```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:h2:mem:tinylinkdb
spring.h2.console.enabled=true



# Application
app.base-url=http://localhost:8080

# Actuator
management.endpoints.web.exposure.include=health,metrics,prometheus,info
management.metrics.distribution.percentiles.http.server.requests=0.5,0.75,0.95,0.99

# Cache (default: ConcurrentMapCacheManager)
spring.cache.type=simple
```

## Technology Stack

- **Framework**: Spring Boot 3.4.0
- **Language**: Java 21
- **Build Tool**: Gradle 8.5
- **Database**: H2 (in-memory)
- **ORM**: Spring Data JPA / Hibernate
- **Security**: Spring Security (HTTP Basic Auth)
- **Cache**: Spring Cache
- **Metrics**: Spring Actuator + Micrometer + Prometheus
- **API Docs**: SpringDoc OpenAPI 3 (Swagger UI)
- **Validation**: Jakarta Bean Validation

## Development

### Running Tests
```bash
./gradlew test
```

### Building for Production
```bash
./gradlew clean build
java -jar build/libs/tiny-url-lab-0.0.1-SNAPSHOT.jar
```

### Hot Reload (Development)
The application includes Spring DevTools for automatic restart on code changes.

## API Documentation

Interactive API documentation is available at:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/v3/api-docs

## License

Apache 2.0

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 🤖 AI Orchestration Scenarios

The project includes three demonstration scenarios showing how AI agents orchestrate the SDLC:

### 1. Greenfield Scenario - Build New Feature
```bash
./.claude/commands/run-greenfield-scenario.sh "Build a URL shortener with QR codes"
```
Demonstrates building a system from scratch with 11 tasks across 7 SDLC stages.

### 2. Brownfield Scenario - Enhance Existing System
```bash
./.claude/commands/run-brownfield-scenario.sh "Add rate limiting to prevent API abuse"
```
Demonstrates adding features to existing codebase with impact analysis and regression testing.

### 3. Ambiguous Scenario - Clarify Vague Requirements
```bash
./.claude/commands/run-ambiguous-scenario.sh "Make the system more reliable"
```
Demonstrates requirement clarification, option evaluation, and human decision points.

Each scenario shows:
- ✅ Requirement parsing and normalization
- ✅ Task decomposition with dependency graphs
- ✅ Parallel execution where possible
- ✅ Human approval gates for critical decisions
- ✅ Quality gates (security, testing, compliance)
- ✅ Audit trail and metrics collection

