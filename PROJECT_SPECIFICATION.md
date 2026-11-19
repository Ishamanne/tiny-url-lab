# Tiny URL Lab - Project Specification

## Project Overview

**Name:** Tiny URL Lab
**Version:** 0.0.1-SNAPSHOT
**Description:** A production-ready URL shortener service built with Spring Boot
**Java Version:** 21
**Spring Boot Version:** 3.4.0
**Build Tool:** Gradle 9.9.1

## Architecture

### Tech Stack

**Core Framework:**
- Spring Boot 3.4.0
- Spring Web MVC
- Spring Data JPA
- Spring Security (Basic Auth)
- Spring Cache
- Spring Actuator
- Spring AOP

**Database:**
- H2 in-memory database (for development)
- Hibernate/JPA for ORM

**Documentation:**
- SpringDoc OpenAPI 3 (Swagger UI)

**Monitoring & Metrics:**
- Micrometer with Prometheus registry
- Spring Boot Actuator endpoints

**Development:**
- Lombok (for boilerplate reduction)
- Spring DevTools (hot reload)

**Testing:**
- JUnit 5 (Jupiter)
- Spring Boot Test
- Mockito
- Spring Security Test

## Features

### 1. URL Shortening
- Generate random 7-character short codes
- Support for custom aliases
- URL validation (must start with http:// or https://)
- Configurable expiration time (default: 7 minutes)
- Base URL: `http://localhost:8080`

### 2. URL Management
- Create shortened URLs with optional custom alias
- Retrieve URL details by short code
- List all URLs
- Update URL expiration time
- Delete URLs (soft delete)
- Track click counts

### 3. URL Status
- **ACTIVE:** URL is valid and accessible
- **EXPIRED:** URL has passed its expiration time

### 4. Analytics
- Track each URL access:
  - IP address
  - User agent
  - Referer
  - Country (placeholder for future implementation)
  - Device type (Mobile/Tablet/Desktop)
  - Access timestamp
- Get analytics summary:
  - Total clicks
  - Device distribution

### 5. Security
- Basic Authentication (username: `admin`, password: `admin123`)
- Role-based access: ADMIN role required
- All API endpoints protected except redirect endpoint

### 6. Caching
- In-memory caching for:
  - Short URLs list
  - Individual short URLs
  - Analytics data
- Cache eviction on create/update/delete operations

### 7. Monitoring
- Health check endpoint
- Metrics with Prometheus format
- Detailed performance metrics:
  - JVM memory usage
  - HTTP request statistics
  - SLA percentiles (50%, 75%, 95%, 99%)
  - Response time histograms

### 8. API Documentation
- Interactive Swagger UI at `/swagger-ui.html`
- OpenAPI 3.0 specification at `/v3/api-docs`
- Comprehensive endpoint documentation
- Request/response schemas

## API Endpoints

### Public Endpoints
- `GET /{shortCode}` - Redirect to original URL (hidden from API docs)

### Protected Endpoints (Basic Auth Required)

**URL Management:**
- `POST /api/shorten` - Create shortened URL
- `GET /api/urls` - List all URLs
- `GET /api/urls/{shortCode}` - Get URL details
- `PATCH /api/urls/{shortCode}` - Update URL expiration
- `DELETE /api/urls` - Delete URL (requires shortCode in body)

**Analytics:**
- `GET /api/urls/{shortCode}/analytics` - Get URL analytics

**Monitoring:**
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Available metrics
- `GET /actuator/metrics/{metricName}` - Specific metric
- `GET /actuator/prometheus` - Prometheus metrics

## Data Models

### ShortUrl Entity
```java
- id: Long (Primary Key)
- shortCode: String (Unique, 10 chars max)
- originalUrl: String (2048 chars max)
- createdAt: Instant
- expiresAt: Instant
- clickCount: Long
- createdBy: String
- status: UrlStatus (ACTIVE/EXPIRED)
```

### UrlAnalytics Entity
```java
- id: Long (Primary Key)
- shortCode: String
- accessedAt: Instant
- ipAddress: String
- userAgent: String
- referer: String
- country: String (nullable)
- device: String (Mobile/Tablet/Desktop)
```

### DTOs

**CreateShortUrlRequest:**
- originalUrl: String (required, URL pattern)
- customAlias: String (optional)
- expiresInMinutes: Long (optional, positive)

**UpdateShortUrlRequest:**
- expiresInMinutes: Long (optional, positive)

**DeleteShortUrlRequest:**
- shortCode: String (required)

**ShortUrlResponse:**
- shortCode: String
- shortUrl: String (full short URL)
- originalUrl: String
- createdAt: Instant
- expiresAt: Instant
- clickCount: Long
- status: UrlStatus

**AnalyticsResponse:**
- shortCode: String
- totalClicks: Long
- deviceDistribution: Map<String, Long>

## Configuration

### Application Properties
```properties
# Server
server.port=8080

# Database (H2 In-Memory)
spring.datasource.url=jdbc:h2:mem:tinylinkdb
spring.datasource.driverClassName=org.h2.Driver
spring.jpa.hibernate.ddl-auto=create-drop

# H2 Console
spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

# Security
spring.security.user.name=admin
spring.security.user.password=admin123
spring.security.user.roles=ADMIN

# Base URL
app.base-url=http://localhost:8080

# Actuator
management.endpoints.web.exposure.include=*
management.endpoint.health.show-details=always
management.metrics.export.prometheus.enabled=true

# OpenAPI/Swagger
springdoc.api-docs.path=/v3/api-docs
springdoc.swagger-ui.path=/swagger-ui.html
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/tinyurllab/
│   │   ├── TinyUrlLabApplication.java
│   │   ├── config/
│   │   │   ├── OpenApiConfig.java
│   │   │   └── SecurityConfig.java
│   │   └── urlshortener/
│   │       ├── controller/
│   │       │   └── UrlShortenerController.java
│   │       ├── service/
│   │       │   └── UrlShortenerService.java
│   │       ├── repository/
│   │       │   ├── ShortUrlRepository.java
│   │       │   └── UrlAnalyticsRepository.java
│   │       ├── model/
│   │       │   ├── ShortUrl.java
│   │       │   ├── UrlAnalytics.java
│   │       │   └── UrlStatus.java
│   │       ├── dto/
│   │       │   ├── CreateShortUrlRequest.java
│   │       │   ├── UpdateShortUrlRequest.java
│   │       │   ├── DeleteShortUrlRequest.java
│   │       │   ├── ShortUrlResponse.java
│   │       │   └── AnalyticsResponse.java
│   │       └── exception/
│   │           ├── UrlNotFoundException.java
│   │           └── UrlExpiredException.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/example/tinyurllab/
        ├── config/
        │   └── OpenApiConfigTest.java
        └── urlshortener/
            └── service/
                └── UrlShortenerServiceTest.java
```

## Development Commands

All commands are located in `.claude/commands/` directory:

### Build & Test
- `./api-test.sh` - Run all tests (14 tests)
- `./test-all.sh` - Run tests with clean build
- `./build-prod.sh` - Build production JAR (excludes tests)
- `./lint-check.sh` - Run compilation checks
- `./security-scan.sh` - Run dependency security scan (requires OWASP plugin)

### Development
- `./dev-start.sh` - Start application locally
- `./dev-stop.sh` - Stop Gradle daemon
- `./logs-tail.sh` - Tail application logs

### Database
- `./db-seed.sh` - Seed database with 10 sample URLs
- `./db-backup.sh` - Backup URLs to JSON file
- `./db-reset.sh` - Reset H2 database (restart app)
- `./cache-clear.sh` - Clear cache (restart app)

### Monitoring
- `./check-health.sh` - Check application health and metrics
- `./metrics-snapshot.sh` - Export metrics to timestamped files

### Testing
- `./load-test.sh` - Run load test (100 requests, 10 concurrent)
- `./api-docs-export.sh` - Export OpenAPI specification

### Deployment
- `./docker-build.sh` - Build JAR (optional: Docker image)

## Running the Application

### Quick Start
```bash
./gradlew bootRun
```

### Access Points
- **Application:** http://localhost:8080
- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/v3/api-docs
- **H2 Console:** http://localhost:8080/h2-console
- **Health:** http://localhost:8080/actuator/health
- **Metrics:** http://localhost:8080/actuator/metrics

### Authentication
- Username: `admin`
- Password: `admin123`

## Testing

### Test Coverage
- **14 tests total**
- 9 service layer tests
- 5 configuration/integration tests

### Running Tests
```bash
./gradlew test
# or
./.claude/commands/api-test.sh
```

### Test Files
1. `UrlShortenerServiceTest.java` - Service layer unit tests
   - Create short URL (with/without custom alias)
   - Resolve short code
   - Handle expired URLs
   - Handle not found URLs
   - Delete URLs
   - Get URL details

2. `OpenApiConfigTest.java` - OpenAPI configuration tests
   - OpenAPI documentation generation
   - Swagger UI accessibility
   - Endpoint presence verification
   - Schema definitions

## Key Implementation Details

### Short Code Generation
- Uses secure random with alphanumeric characters (a-z, A-Z, 0-9)
- 7-character length
- Collision detection with retry logic
- Custom alias support with availability check

### Expiration Handling
- Default expiration: 7 minutes
- Automatic status update to EXPIRED on access
- Expired URLs can be reused (soft delete + cleanup)

### Click Tracking
- Increments count on each redirect
- Saves analytics with device detection
- IP address and user agent tracking

### Caching Strategy
- Read operations cached
- Write operations evict cache
- Cache keys: "shortUrls", "shortUrl", "analytics"

### Security
- All endpoints require authentication except redirect
- Password stored in application.properties (use environment variables in production)
- Basic Auth over HTTPS recommended for production

### Error Handling
- Custom exceptions: `UrlNotFoundException`, `UrlExpiredException`
- Global exception handling with appropriate HTTP status codes
- Validation errors return 400 Bad Request
- Authentication failures return 401 Unauthorized

## Metrics & Observability

### Available Metrics
- `application.ready.time` - Application startup time
- `application.started.time` - Time when application started
- `jvm.memory.used` - JVM memory usage
- `http.server.requests` - HTTP request statistics
- `url.shorten` - URL shortening operation time
- `url.redirect` - Redirect operation time
- `url.delete` - Delete operation time

### SLA Monitoring
- P50, P75, P95, P99 percentiles tracked
- Response time histograms: 10ms, 50ms, 100ms, 200ms, 500ms, 1s, 2s

## Production Considerations

### Database Migration
Current setup uses H2 in-memory database. For production:

1. **Switch to PostgreSQL:**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/tinyurl
spring.datasource.username=your_username
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=validate
```

2. **Add Flyway for migrations:**
```gradle
implementation 'org.flywaydb:flyway-core'
```

### Security Enhancements
- Use environment variables for credentials
- Implement JWT authentication
- Add rate limiting
- Enable HTTPS
- Use secure password encoding

### Caching Improvements
- Use Redis for distributed caching
- Configure TTL for cache entries
- Implement cache warming strategy

### Monitoring Setup
- Deploy Prometheus for metrics collection
- Set up Grafana dashboards
- Configure alerting rules
- Implement distributed tracing with Zipkin/Jaeger

### Scaling Considerations
- Add database connection pooling (HikariCP is included)
- Implement database read replicas
- Use CDN for redirect endpoints
- Consider sharding strategy for high volume

## Troubleshooting

### Common Issues

**Port 8080 already in use:**
```bash
# Find and kill process
lsof -i :8080
kill -9 <PID>
```

**Tests failing:**
```bash
# Clean build and rerun
./gradlew clean test
```

**Gradle daemon issues:**
```bash
./gradlew --stop
```

## Future Enhancements

### Potential Features
- [ ] QR code generation for short URLs
- [ ] Custom domain support
- [ ] Link preview with Open Graph metadata
- [ ] Bulk URL creation API
- [ ] URL expiry notifications
- [ ] Geographic analytics with MaxMind GeoIP
- [ ] Browser analytics
- [ ] Click fraud detection
- [ ] API rate limiting per user
- [ ] Webhook notifications on URL access

### Technical Improvements
- [ ] Add Flyway migrations
- [ ] Implement Redis caching
- [ ] Add integration tests with Testcontainers
- [ ] Add code coverage reporting with JaCoCo
- [ ] Add static code analysis (Checkstyle, SpotBugs, PMD)
- [ ] Add OWASP dependency check
- [ ] Implement API versioning
- [ ] Add GraphQL endpoint
- [ ] Implement async processing for analytics
- [ ] Add Elasticsearch for analytics queries

## License

[Specify your license here]

## Contributors

[Specify contributors here]

---

**Last Updated:** November 19, 2026
**Maintained By:** [Your Name/Team]
