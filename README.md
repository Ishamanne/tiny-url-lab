# URL Shortener Service

A production-grade URL shortener built with Spring Boot, featuring caching, metrics monitoring, and role-based access control.

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

| Endpoint | Method | Access | Description |
|----------|--------|--------|-------------|
| `/{shortCode}` | GET | Public | Redirect to original URL (no auth required) |
| `/api/urls` | GET | USER, ADMIN | List all short URLs |
| `/api/urls/{shortCode}` | GET | USER, ADMIN | Get short URL details |
| `/api/urls/{shortCode}/analytics` | GET | USER, ADMIN | Get analytics data |
| `/api/shorten` | POST | ADMIN | Create new short URL |
| `/api/urls/{shortCode}` | DELETE | ADMIN | Delete short URL |
| `/swagger-ui/**` | GET | Public | Swagger UI documentation |
| `/actuator/**` | GET | Public | Metrics and health endpoints |

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

### Delete URL (ADMIN only)
```bash
curl -X DELETE http://localhost:8080/api/urls/mylink \
  -u admin:admin123
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
- `url.shorten` - Create short URL latency
- `url.getAll` - Get all URLs latency
- `url.getOne` - Get single URL latency
- `url.analytics` - Get analytics latency
- `url.delete` - Delete URL latency
- `url.redirect` - Redirect latency

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

# Security
spring.security.user.name=admin
spring.security.user.password=admin123

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
