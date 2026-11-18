# Quick Start Guide

## Prerequisites
- Java 25
- Gradle 9.9.1+

## Build & Run

```bash
# Build the project
./gradlew clean build

# Run the application
./gradlew bootRun

# Run tests
./gradlew test
```

## Test the System

### 1. Create a Short URL
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{
    "originalUrl": "https://www.example.com/very/long/url/path",
    "description": "Example URL"
  }'
```

### 2. Access Short URL
```bash
# This will redirect you
curl -L http://localhost:8080/{shortCode}
```

### 3. Get Analytics
```bash
curl http://localhost:8080/api/urls/{shortCode}/analytics
```

### 4. Execute Workflow Scenarios

#### Greenfield Scenario
```bash
curl -X POST http://localhost:8080/api/orchestration/workflows/greenfield
```

#### Brownfield Scenario
```bash
curl -X POST http://localhost:8080/api/orchestration/workflows/brownfield
```

#### Ambiguous Scenario
```bash
curl -X POST http://localhost:8080/api/orchestration/workflows/ambiguous
```

### 5. View Metrics
```bash
curl http://localhost:8080/api/orchestration/metrics
```

### 6. Get Workflow Status
```bash
curl http://localhost:8080/api/orchestration/workflows/{workflowId}
```

### 7. View Audit Trail
```bash
curl http://localhost:8080/api/orchestration/workflows/{workflowId}/audit
```

## Access H2 Console

Navigate to: http://localhost:8080/h2-console
- JDBC URL: `jdbc:h2:mem:tinylinkdb`
- Username: `sa`
- Password: (empty)

## Key Files

- **README.md** - Full documentation
- **ENGINEERING_SUMMARY.md** - Detailed engineering decisions and architecture
- **build.gradle** - Dependencies and build configuration
- **src/main/java/com/example/tinylinklab/orchestration/** - Orchestration engine
- **src/main/java/com/example/tinylinklab/urlshortener/** - URL shortener service
- **src/test/** - Test suite

## Expected Output

When you run a workflow scenario, you'll see:
1. Task execution logs
2. Quality gate validations
3. Approval checkpoints (auto-approved in demo)
4. Audit trail entries
5. Final workflow status (COMPLETED/FAILED)
6. Metrics (success rate, MTTR, retries)

## Next Steps

1. Review **README.md** for complete API documentation
2. Read **ENGINEERING_SUMMARY.md** for architecture details
3. Examine workflow scenarios in `orchestration/scenarios/`
4. Check test cases in `src/test/`
5. Explore audit trails and metrics via API
