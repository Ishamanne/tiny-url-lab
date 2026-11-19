# Project Structure

## Directory Layout

```
tiny-url-lab/
├── build.gradle                    # Build configuration
├── settings.gradle
├── gradlew                         # Gradle wrapper
├── README.md                       # Complete documentation
├── QUICKSTART.md                   # Quick start guide
├── ENGINEERING_SUMMARY.md          # Engineering decisions & architecture
├── PROJECT_STRUCTURE.md           # This file
│
├── src/main/
│   ├── java/com/example/tinylinklab/
│   │   ├── TinyLinkLabApplication.java       # Spring Boot main class
│   │   │
│   │   ├── orchestration/                     # Agentic Orchestration Layer
│   │   │   ├── controller/
│   │   │   │   └── OrchestrationController.java    # Workflow APIs
│   │   │   │
│   │   │   ├── engine/
│   │   │   │   └── WorkflowOrchestrator.java       # DAG execution engine
│   │   │   │
│   │   │   ├── model/
│   │   │   │   ├── TaskNode.java                   # Task representation
│   │   │   │   ├── WorkflowContext.java            # Context & lineage
│   │   │   │   ├── WorkflowStage.java              # SDLC stages
│   │   │   │   ├── WorkflowMetrics.java            # Reliability metrics
│   │   │   │   └── GateCheck.java                  # Quality gates
│   │   │   │
│   │   │   ├── scenarios/
│   │   │   │   ├── WorkflowScenario.java           # Scenario interface
│   │   │   │   ├── GreenfieldScenario.java         # Build from scratch
│   │   │   │   ├── BrownfieldScenario.java         # Enhance existing
│   │   │   │   └── AmbiguousScenario.java          # Handle vague reqs
│   │   │   │
│   │   │   └── governance/
│   │   │       └── PolicyGuardrails.java           # Security & compliance
│   │   │
│   │   └── urlshortener/                      # URL Shortener Service
│   │       ├── controller/
│   │       │   ├── UrlShortenerController.java     # REST endpoints
│   │       │   └── GlobalExceptionHandler.java     # Error handling
│   │       │
│   │       ├── service/
│   │       │   └── UrlShortenerService.java        # Business logic
│   │       │
│   │       ├── repository/
│   │       │   ├── ShortUrlRepository.java         # URL data access
│   │       │   └── UrlAnalyticsRepository.java     # Analytics data access
│   │       │
│   │       ├── model/
│   │       │   ├── ShortUrl.java                   # URL entity
│   │       │   └── UrlAnalytics.java               # Analytics entity
│   │       │
│   │       ├── dto/
│   │       │   ├── CreateShortUrlRequest.java      # Create request
│   │       │   ├── ShortUrlResponse.java           # URL response
│   │       │   └── AnalyticsResponse.java          # Analytics response
│   │       │
│   │       └── exception/
│   │           ├── UrlNotFoundException.java       # Not found exception
│   │           └── UrlExpiredException.java        # Expired exception
│   │
│   └── resources/
│       └── application.properties              # Configuration
│
└── src/test/
    └── java/com/example/tinylinklab/
        ├── TinyLinkLabApplicationTests.java
        ├── urlshortener/service/
        │   └── UrlShortenerServiceTest.java    # Service unit tests
        └── orchestration/engine/
            └── WorkflowOrchestratorTest.java   # Orchestrator tests
```

## Module Breakdown

### Orchestration Layer (Core Innovation)

**Purpose**: Autonomous SDLC automation with governance

**Key Components**:
- `WorkflowOrchestrator`: DAG-based execution engine with parallel scheduling
- `TaskNode`: Task with status, dependencies, gates, context, metrics
- `WorkflowContext`: Cross-stage context, decision lineage, audit trail
- `WorkflowStage`: 8 SDLC stages (Requirements → Release)
- `GateCheck`: Quality gates (security, code quality, compliance)
- `PolicyGuardrails`: Security, compliance, change control enforcement
- `WorkflowMetrics`: Reliability tracking (success rate, MTTR, retries)

**Scenarios**:
- `GreenfieldScenario`: Build URL shortener from scratch (11 tasks)
- `BrownfieldScenario`: Add rate limiting to existing system (9 tasks)
- `AmbiguousScenario`: Handle "improve performance" vague requirement (8 tasks)

### URL Shortener Service

**Purpose**: Production-quality URL shortening with analytics

**Key Components**:
- `UrlShortenerService`: Core business logic (create, resolve, analytics)
- `ShortUrlRepository`: Database access for URLs
- `UrlAnalyticsRepository`: Database access for click tracking
- `UrlShortenerController`: REST API endpoints
- `GlobalExceptionHandler`: Centralized error handling

**Features**:
- Create short URLs with optional custom codes
- URL expiration support
- Click tracking with analytics
- Country/device distribution
- Input validation
- Error handling

## Key Design Patterns

1. **Repository Pattern**: Data access abstraction
2. **Service Layer**: Business logic separation
3. **DTO Pattern**: Request/response objects
4. **Builder Pattern**: Complex object construction
5. **Strategy Pattern**: Different workflow scenarios
6. **State Machine**: Task lifecycle management
7. **Observer Pattern**: Task status updates
8. **Dependency Injection**: Spring-managed components

## Component Dependencies

```
OrchestrationController
    ↓
WorkflowOrchestrator → TaskNode → GateCheck
    ↓                      ↓
WorkflowContext      WorkflowMetrics
    ↓
PolicyGuardrails

UrlShortenerController
    ↓
UrlShortenerService
    ↓
ShortUrlRepository ← ShortUrl (JPA Entity)
UrlAnalyticsRepository ← UrlAnalytics (JPA Entity)
```

## Technology Stack

- **Framework**: Spring Boot 4.1.2
- **Language**: Java 25
- **Build Tool**: Gradle 9.9.1
- **Database**: H2 (in-memory)
- **ORM**: Spring Data JPA
- **AI**: Spring AI 2.0.1 (Anthropic integration)
- **Validation**: Spring Validation
- **Utilities**: Lombok, Jackson
- **Testing**: JUnit 5, Mockito

## Configuration Files

- `build.gradle`: Dependencies, Java version, Spring Boot plugin
- `application.properties`: Database, server, logging, Spring AI config
- `settings.gradle`: Project name

## Testing Strategy

- **Unit Tests**: Service layer with mocked dependencies
- **Integration Tests**: (Planned) Full API end-to-end tests
- **Coverage Target**: 80%+

## Documentation Files

- **README.md**: Complete system documentation with API reference
- **ENGINEERING_SUMMARY.md**: Architecture, decisions, trade-offs, validation
- **QUICKSTART.md**: Quick start guide with curl examples
- **PROJECT_STRUCTURE.md**: This file

## Running the System

```bash
# Build
./gradlew clean build

# Run
./gradlew bootRun

# Test
./gradlew test

# Access
http://localhost:8080
```

## Key Metrics

- **Total Files**: 28 Java files
- **Lines of Code**: ~3500+ LOC
- **Test Coverage**: 80%+ (target)
- **API Endpoints**: 15+ REST endpoints
- **Workflow Tasks**: 28 tasks across 3 scenarios
- **Quality Gates**: 7 types of gates
- **SDLC Stages**: 8 stages

## Extension Points

1. **Add New Scenario**: Implement `WorkflowScenario` interface
2. **Add Quality Gate**: Create new `GateCheck.GateType`
3. **Add Policy**: Extend `PolicyGuardrails` checks
4. **Add Metrics**: Extend `WorkflowMetrics` tracking
5. **Add URL Feature**: Extend `UrlShortenerService`

## Production Enhancements Needed

1. Replace H2 with PostgreSQL
2. Add distributed execution (Temporal/Cadence)
3. Integrate real security tools (Snyk, SonarQube)
4. Add persistent workflow state storage
5. Implement real approval system integration
6. Add cost tracking and optimization
7. Implement dynamic workflow re-planning
8. Add multi-tenancy support

---

**Last Updated**: January 2025
**Version**: 1.0.0
**Author**: Agentic Software Engineering System
