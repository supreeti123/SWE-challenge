# Todo Service

A RESTful backend service for managing a simple to-do list, built with Java and Spring Boot.

## Description

This service provides a REST API for creating and managing to-do items. Each item has a description, status, creation timestamp, optional due date, and a done timestamp. The service automatically detects and marks items as "past due" when their due date has passed, and prevents any modification of past-due items.

### Assumptions

- **Items with no due date** never become "past due" and remain "not done" until explicitly marked as done.
- **Creating an item with a past due date** is allowed; the item is immediately assigned "past due" status.
- **Past-due consistency** uses a hybrid approach: a scheduled job runs every minute to batch-update overdue items, and the service also synchronizes overdue items at API call time for stronger read/write consistency.
- **Idempotent status operations**: marking an already-done item as "done" (or an already-not-done item as "not done") succeeds as a no-op and does not rewrite business timestamps.
- **"Get not done" endpoint** returns only items with status "not done" (excludes both "done" and "past due" items). Use `?includeAll=true` to retrieve all items.

## Tech Stack

- **Runtime**: Java 21 (Eclipse Temurin)
- **Framework**: Spring Boot 3.2.5
- **Persistence**: Spring Data JPA with H2 in-memory database
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Operations**: Spring Boot Actuator (health/readiness/liveness), graceful shutdown
- **Testing**: JUnit 5, Mockito, Spring MockMvc, AssertJ
- **Build**: Apache Maven
- **Containerization**: Docker (multi-stage build)
- **API Contract**: OpenAPI 3 via springdoc (`/api-docs`, `/swagger-ui.html`)

## Production Readiness Notes

- **Consistency**
  - Optimistic locking (`@Version`) prevents lost updates on concurrent writes.
  - Overdue-state synchronization happens both in scheduler and during API requests.
- **Scalability**
  - Bulk JPQL update is used for overdue transitions (no per-row entity loading for batch transitions).
  - Database indexes are present for high-frequency query fields (`status`, `dueAt`).
- **Availability/Operations**
  - Graceful shutdown is enabled.
  - Actuator health endpoints are exposed for platform probes:
    - `/actuator/health`
    - `/actuator/health/liveness`
    - `/actuator/health/readiness`
  - Docker image runs as a non-root user and includes a readiness healthcheck.

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/todos` | Add a new to-do item |
| `PATCH` | `/api/todos/{id}/description` | Change an item's description |
| `PATCH` | `/api/todos/{id}/done` | Mark an item as done |
| `PATCH` | `/api/todos/{id}/not-done` | Mark an item as not done |
| `GET` | `/api/todos` | Get paginated "not done" items |
| `GET` | `/api/todos?includeAll=true` | Get paginated items regardless of status |
| `GET` | `/api/todos/{id}` | Get details of a specific item |

Pagination query params for `GET /api/todos`:
- `page` (default `0`)
- `size` (default `20`, max `100`)
- `sort` (default `createdAt,desc`)

## How To

### Build the service

```bash
mvn clean package
```

### Run automatic tests

```bash
mvn test
```

The test suite includes:
- **Unit tests** (16): Service layer logic with mocked repository
- **Integration tests** (14): Full HTTP request/response cycle with H2 database
- **Scheduler tests** (4): Past-due batch transition logic
- **Smoke test** (1): Spring application context loads

### Run the service locally

**With Maven:**
```bash
mvn spring-boot:run
```

**With the packaged JAR:**
```bash
java -jar target/todo-service-0.0.1-SNAPSHOT.jar
```

The service starts on `http://localhost:8080`.

**H2 Console** is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:tododb`, username: `sa`, no password).

**OpenAPI docs**:
- Spec: `http://localhost:8080/api-docs`
- Swagger UI: `http://localhost:8080/swagger-ui.html`

### Run with Docker

```bash
docker build -t todo-service .
docker run -p 8080:8080 todo-service
```

## CI and Security Scan

GitHub Actions workflow (`.github/workflows/ci.yml`) runs:
- Maven build + tests (`mvn -B clean verify`)
- Docker image build
- Trivy filesystem scan
- Trivy container image scan
