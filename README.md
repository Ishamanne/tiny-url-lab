# Agentic Software Engineering System - URL Shortener

## Overview

This project demonstrates a production-grade **Agentic Software Engineering System** that transforms requirements into reviewable engineering outcomes using autonomous AI agents with controlled governance. The system implements a complete URL shortener service while showcasing sophisticated SDLC automation through DAG-based workflow orchestration.

## Key Features

### Agentic Orchestration Layer
- **DAG-Based Workflow Engine**: Explicit dependency graphs with parallel and sequential execution paths
- **Entry/Exit Quality Gates**: Security scans, code quality checks, test coverage, compliance validation
- **Human Approval Checkpoints**: Strategic approval gates for critical decisions
- **Context Preservation**: Cross-stage context and decision lineage tracking
- **Retry & Rollback**: Bounded retries, fallback mechanisms, and safe-stop controls
- **Audit-Grade Observability**: Complete audit trails, metrics tracking (success rate, MTTR, latency)
- **Policy Guardrails**: Security, compliance, and change control enforcement

### URL Shortener Service
- **Core APIs**: Create, resolve, list, delete short URLs
- **Custom Short Codes**: Support for user-defined short codes
- **URL Expiration**: Time-based URL expiration
- **Analytics**: Click tracking with country/device distribution
- **Performance**: Database indexing for fast lookups
- **Validation**: Input validation and error handling
- **REST API**: Well-structured RESTful endpoints

## Architecture

### System Components

```
┌─────────────────────────────────────────────────────────────┐
│                   Orchestration Layer                        │
│                                                              │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │   Workflow   │    │   Quality    │    │   Context    │ │
│  │ Orchestrator │◄──►│    Gates     │◄──►│  Management  │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                    │                    │         │
│         ▼                    ▼                    ▼         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │          DAG Execution Engine                        │  │
│  │  • Task scheduling • Parallel execution              │  │
│  │  • Dependency resolution • State management          │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌─────────────────────────────────────────────────────────────┐
│                  URL Shortener Service                       │
│                                                              │
│  ┌────────────┐    ┌────────────┐    ┌────────────┐       │
│  │    URL     │    │ Analytics  │    │   Data     │       │
│  │  Service   │◄──►│  Service   │◄──►│   Layer    │       │
│  └────────────┘    └────────────┘    └────────────┘       │
│         │                  │                  │             │
│         ▼                  ▼                  ▼             │
│  ┌────────────────────────────────────────────────┐        │
│  │        H2 Database (ShortUrl, Analytics)       │        │
│  └────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Workflow Orchestration Model

The orchestration engine implements a sophisticated DAG-based execution model:

1. **Dependency Graph**: Tasks organized with explicit dependencies
2. **Parallel Execution**: Independent tasks run concurrently via ExecutorService
3. **Synchronization Points**: Tasks with multiple dependencies wait for all to complete
4. **Quality Gates**: Entry and exit gates validate task execution
5. **Approval Checkpoints**: Critical tasks require human approval
6. **State Machine**: Tasks transition through PENDING → READY → RUNNING → COMPLETED/FAILED
7. **Retry Logic**: Failed tasks retry up to max attempts before rollback
8. **Audit Trail**: Every state transition logged with timestamp and actor

### SDLC Stages

1. **Requirement Analysis**: Parse and normalize requirements, identify ambiguities
2. **Architecture Design**: Design system components and data flows
3. **Task Decomposition**: Break down into actionable implementation tasks
4. **Implementation**: Generate production-quality code
5. **Testing**: Execute unit and integration tests
6. **Documentation**: Generate technical documentation
7. **Validation**: Run quality gates and compliance checks
8. **Release Readiness**: Final deployment checks

## Three Scenario Demonstrations

### 1. Greenfield Scenario: Build URL Shortener from Scratch

**Requirement**: Build a complete URL shortener service with core APIs, analytics, and reliability features

**Workflow**:
- 11 tasks across 8 SDLC stages
- 2 human approval checkpoints (Architecture Design, Release Validation)
- 7 quality gates (requirements, architecture, code quality, security, test coverage, compliance, performance)
- Parallel implementation tasks (domain model, service, API, analytics)

**Key Decisions**:
- Chose H2 in-memory database for simplicity and fast development
- Implemented Base62 encoding for short codes
- Added database indexes for performance
- Separated analytics into distinct entity for scalability

### 2. Brownfield Scenario: Add Rate Limiting Feature

**Requirement**: Enhance existing URL shortener with rate limiting to prevent abuse

**Workflow**:
- 9 tasks focusing on codebase analysis and integration
- Explicit impact analysis and breaking change detection
- Regression testing to ensure no existing functionality breaks
- Documentation updates for new feature

**Key Decisions**:
- Analyzed existing controllers and identified injection points
- Proposed token bucket algorithm for rate limiting
- Identified public API changes requiring approval
- Prioritized backward compatibility

### 3. Ambiguous Scenario: "Improve Performance"

**Requirement**: Vague requirement "improve URL shortener performance"

**Workflow**:
- Requirement clarification phase with human approval
- Performance profiling to identify bottlenecks
- Multiple solution proposals (caching, indexing, query optimization)
- Performance testing with before/after metrics

**Key Decisions**:
- Identified ambiguity: latency vs throughput vs resource usage
- Profiled system to find database query bottleneck
- Proposed Redis caching and database indexing
- Required 30% performance improvement validation gate

## Setup Instructions

### Prerequisites

- **Java 25** (configured in build.gradle)
- **Gradle** 9.9.1 or higher
- **Optional**: ANTHROPIC_API_KEY environment variable for Spring AI features

### Installation

1. **Clone the repository**:
   ```bash
   cd /Users/vijayl/IdeaProjects/tiny-url-lab
   ```

2. **Build the project**:
   ```bash
   ./gradlew clean build
   ```

3. **Run tests**:
   ```bash
   ./gradlew test
   ```

4. **Start the application**:
   ```bash
   ./gradlew bootRun
   ```

5. **Access the application**:
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
| `/v3/api-docs/**` | GET | Public | OpenAPI specification |

### Using Authentication

#### With cURL
```bash
# Create short URL (requires ADMIN)
curl -X POST http://localhost:8080/api/shorten \
  -u admin:admin123 \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://example.com"}'

# Get URLs (requires USER or ADMIN)
curl -X GET http://localhost:8080/api/urls \
  -u user:user123

# Redirect (no authentication needed)
curl -L http://localhost:8080/abc123
```

#### With Swagger UI
1. Navigate to `http://localhost:8080/swagger-ui.html`
2. Click the **Authorize** button (lock icon)
3. Enter username and password:
   - For read operations: `user:user123`
   - For all operations: `admin:admin123`
4. Click **Authorize** and **Close**
5. All subsequent API calls will include authentication

## API Documentation

### URL Shortener APIs

#### Create Short URL
**Authentication Required**: ADMIN role

```http
POST /api/shorten
Authorization: Basic YWRtaW46YWRtaW4xMjM=
Content-Type: application/json

{
  "originalUrl": "https://example.com/very/long/url",
  "customCode": "optional-custom-code",
  "expiresAt": "2025-12-31T23:59:59Z",
  "description": "My short URL",
  "createdBy": "user@example.com"
}

Response: 201 Created
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/abc123",
  "originalUrl": "https://example.com/very/long/url",
  "createdAt": "2025-01-15T10:30:00Z",
  "expiresAt": "2025-12-31T23:59:59Z",
  "clickCount": 0,
  "active": true,
  "description": "My short URL"
}
```

#### Redirect to Original URL
**Authentication Required**: None (Public endpoint)

```http
GET /{shortCode}

Response: 302 Redirect to original URL
```

#### Get Short URL Details
**Authentication Required**: USER or ADMIN role

```http
GET /api/urls/{shortCode}
Authorization: Basic dXNlcjp1c2VyMTIz

Response: 200 OK
{
  "shortCode": "abc123",
  "shortUrl": "http://localhost:8080/abc123",
  "originalUrl": "https://example.com/very/long/url",
  "clickCount": 42,
  "active": true
}
```

#### Get Analytics
**Authentication Required**: USER or ADMIN role

```http
GET /api/urls/{shortCode}/analytics
Authorization: Basic dXNlcjp1c2VyMTIz

Response: 200 OK
{
  "shortCode": "abc123",
  "totalClicks": 1250,
  "clicksLast24Hours": 45,
  "clicksLast7Days": 320,
  "countryDistribution": {
    "US": 500,
    "UK": 300,
    "CA": 200
  },
  "deviceDistribution": {
    "Desktop": 700,
    "Mobile": 450,
    "Tablet": 100
  }
}
```

#### List All URLs
**Authentication Required**: USER or ADMIN role

```http
GET /api/urls
Authorization: Basic dXNlcjp1c2VyMTIz

Response: 200 OK
[
  {
    "shortCode": "abc123",
    "shortUrl": "http://localhost:8080/abc123",
    "originalUrl": "https://example.com/very/long/url",
    "clickCount": 42,
    "active": true
  }
]
```

#### Delete Short URL
**Authentication Required**: ADMIN role

```http
DELETE /api/urls/{shortCode}
Authorization: Basic YWRtaW46YWRtaW4xMjM=

Response: 204 No Content
```

### Orchestration APIs

#### Execute Greenfield Workflow
```http
POST /api/orchestration/workflows/greenfield

Response: 200 OK
{
  "workflowId": "greenfield-uuid",
  "scenarioType": "GREENFIELD",
  "requirement": "Build URL shortener service...",
  "status": "COMPLETED",
  "startTime": "2025-01-15T10:00:00Z",
  "endTime": "2025-01-15T10:05:30Z",
  "auditTrail": [...],
  "decisionLineage": [...]
}
```

#### Execute Brownfield Workflow
```http
POST /api/orchestration/workflows/brownfield

Response: 200 OK
```

#### Execute Ambiguous Workflow
```http
POST /api/orchestration/workflows/ambiguous

Response: 200 OK
```

#### Get Workflow Status
```http
GET /api/orchestration/workflows/{workflowId}

Response: 200 OK
```

#### Get Audit Trail
```http
GET /api/orchestration/workflows/{workflowId}/audit

Response: 200 OK
[
  {
    "eventId": "uuid",
    "taskId": "task-001",
    "eventType": "TASK_STARTED",
    "description": "Analyze Requirements",
    "timestamp": "2025-01-15T10:00:00Z",
    "actor": "AGENT"
  }
]
```

#### Get Decision Lineage
```http
GET /api/orchestration/workflows/{workflowId}/decisions

Response: 200 OK
```

#### Get Metrics
```http
GET /api/orchestration/metrics

Response: 200 OK
{
  "totalExecutions": 150,
  "successfulExecutions": 142,
  "failedExecutions": 8,
  "successRate": 94.67,
  "retriesPerformed": 12,
  "rollbacksPerformed": 3,
  "averageExecutionTimeMs": 5234.5,
  "mttr": 2500.0
}
```

#### List Scenarios
```http
GET /api/orchestration/scenarios

Response: 200 OK
```

## Testing Approach

### Unit Tests
- **Service Layer**: Mock repositories, test business logic
- **Orchestration Engine**: Test DAG execution, parallel paths, gates, retries
- **Coverage Target**: 80%+

### Integration Tests
- **API Layer**: Test REST endpoints end-to-end
- **Database**: Test JPA repositories with H2
- **Workflow**: Test complete scenario execution

### Test Execution
```bash
./gradlew test
./gradlew test --tests "UrlShortenerServiceTest"
./gradlew test --tests "WorkflowOrchestratorTest"
```

## Reliability & Observability

### Metrics Tracked
- **Success Rate**: % of workflows completed successfully
- **Retry Frequency**: Number of task retries performed
- **Rollback Frequency**: Number of rollbacks executed
- **MTTR**: Mean Time To Recovery from failures
- **End-to-End Latency**: Average workflow execution time

### Audit Trail
Every workflow action is logged with:
- Event ID, Task ID, Event Type
- Description and metadata
- Timestamp
- Actor (AGENT or HUMAN)

### Decision Lineage
All agent decisions are tracked with:
- Decision made
- Rationale
- Context at decision time
- Timestamp

## Trade-offs & Design Decisions

### 1. In-Memory Database (H2)
**Decision**: Use H2 instead of PostgreSQL/MySQL
**Rationale**: Fast development, easy setup, sufficient for prototype
**Trade-off**: Data lost on restart, not suitable for production
**Production Alternative**: PostgreSQL with connection pooling

### 2. Synchronous Workflow Execution
**Decision**: Workflows execute synchronously in API calls
**Rationale**: Simpler implementation, easier debugging
**Trade-off**: Long-running workflows block HTTP threads
**Production Alternative**: Async execution with message queue (Kafka, RabbitMQ)

### 3. Auto-Approval of Human Gates
**Decision**: Automatically approve human checkpoints in demo
**Rationale**: Enable end-to-end automation for demonstration
**Trade-off**: Not production-realistic
**Production Alternative**: Integration with approval systems (JIRA, PagerDuty)

### 4. Simplified Security Scanning
**Decision**: Mock security gate checks
**Rationale**: Focus on orchestration architecture
**Trade-off**: No actual security validation
**Production Alternative**: Integrate with Snyk, SonarQube, Checkmarx

### 5. Single-Node Execution
**Decision**: ExecutorService thread pool on single JVM
**Rationale**: Sufficient for prototype scale
**Trade-off**: Limited scalability, single point of failure
**Production Alternative**: Distributed execution with Temporal, Cadence, or Airflow

### 6. No Agent-to-Agent Communication
**Decision**: Orchestrator dispatches tasks; agents don't communicate directly
**Rationale**: Simpler control flow, easier governance
**Trade-off**: Less autonomous agent collaboration
**Production Alternative**: Multi-agent systems with communication protocols

## Limitations & Future Enhancements

### Current Limitations
1. **No Distributed Execution**: Single-node only
2. **No Persistent Workflow State**: Lost on restart
3. **Limited Scalability**: Thread pool size constraints
4. **Mock Quality Gates**: Not integrated with real tools
5. **No Cost Optimization**: No budget constraints for agent actions
6. **Simple GeoIP**: Country detection is stubbed

### Future Enhancements
1. **Distributed Workflow Engine**: Support for multi-node execution
2. **Workflow Persistence**: Store workflow state in database
3. **Real Tool Integration**: Connect to actual security/quality tools
4. **Advanced Analytics**: Machine learning for bottleneck prediction
5. **Cost Management**: Track and optimize agent action costs
6. **Dynamic Re-planning**: Adjust workflow when upstream changes detected
7. **Multi-Tenancy**: Support multiple projects/teams
8. **WebSocket Updates**: Real-time workflow progress notifications

## Validation & Quality Control

### Entry Gates
- Requirements completeness check
- Architecture validation
- Dependency checks

### Exit Gates
- Code quality metrics (complexity, duplication)
- Security scans (vulnerabilities, secrets)
- Test coverage thresholds (>80%)
- Performance baselines
- Breaking change detection

### Approval Checkpoints
- Architecture design decisions
- High-impact code changes
- Release validation
- Ambiguous requirement clarification

## Compliance & Governance

### Policy Guardrails
- **Security**: No secrets in code, vulnerability scanning
- **Compliance**: Audit trail retention, approval enforcement
- **Change Control**: Breaking change detection, rollback capability

### Bounded Autonomy
- **Agents Execute**: Multi-step work within defined boundaries
- **Humans Oversee**: Critical decisions, approvals, quality control
- **System Enforces**: Quality gates, policy guardrails, safe stops

## Engineering Summary

### What Was Built
- Production-quality URL shortener with REST APIs, analytics, and persistence
- Sophisticated DAG-based workflow orchestration engine
- Three complete scenario demonstrations (greenfield, brownfield, ambiguous)
- Comprehensive test suite with unit and integration tests
- Audit-grade observability with metrics and decision tracking

### Key Architectural Principles
1. **Modularity**: Clear separation of orchestration and domain logic
2. **Testability**: Mockable dependencies, comprehensive test coverage
3. **Reliability**: Retry logic, rollback capability, safe-stop controls
4. **Security**: Input validation, error handling, audit trails
5. **Scalability**: Parallel execution, database indexing, clean design
6. **Maintainability**: Well-documented code, clear interfaces, standard patterns

### Validation Results
- ✅ Workflows execute end-to-end successfully
- ✅ DAG dependencies resolved correctly
- ✅ Parallel tasks execute concurrently
- ✅ Quality gates enforced
- ✅ Approval checkpoints trigger
- ✅ Retry and rollback mechanisms work
- ✅ Audit trail captures all events
- ✅ Metrics track reliability
- ✅ URL shortener APIs functional
- ✅ Analytics tracking works
- ✅ Tests pass

### Assumptions
1. Single-tenant deployment
2. Moderate scale (< 10K requests/sec)
3. Trusted internal network
4. Development/prototype environment
5. Human approvers available during business hours

## License

This project is a demonstration prototype for educational and interview purposes.

---

**Built with**: Java 25, Spring Boot 4.1.2, Spring AI 2.0.1, H2 Database, JPA, Lombok, JUnit 5

**Author**: Agentic Software Engineering System
**Date**: January 2025
