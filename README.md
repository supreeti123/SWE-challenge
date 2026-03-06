# Todo Service

A RESTful backend service for managing a simple to-do list, built with Java and Spring Boot.

## Description

This service provides a REST API for creating and managing to-do items. Each item has a description, status, creation timestamp, optional due date, and a done timestamp. The service automatically detects and marks items as "past due" when their due date has passed, and prevents any modification of past-due items.

### Assumptions

- **Items with no due date** never become "past due" and remain "not done" until explicitly marked as done.
- **Creating an item with a past due date** is allowed; the item is immediately assigned "past due" status.
- **Past-due detection** uses a hybrid approach: a scheduled job runs every minute to batch-update overdue items, and a real-time guard on every mutation prevents modifying items that became overdue between scheduler runs.
- **Idempotent status operations**: marking an already-done item as "done" (or an already-not-done item as "not done") succeeds as a no-op rather than returning an error.
- **"Get not done" endpoint** returns only items with status "not done" (excludes both "done" and "past due" items). Use `?includeAll=true` to retrieve all items.

## Tech Stack

- **Runtime**: Java 17 (Eclipse Temurin)
- **Framework**: Spring Boot 3.2.5
- **Persistence**: Spring Data JPA with H2 in-memory database
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Testing**: JUnit 5, Mockito, Spring MockMvc, AssertJ
- **Build**: Apache Maven
- **Containerization**: Docker (multi-stage build)

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/todos` | Add a new to-do item |
| `PATCH` | `/api/todos/{id}/description` | Change an item's description |
| `PATCH` | `/api/todos/{id}/done` | Mark an item as done |
| `PATCH` | `/api/todos/{id}/not-done` | Mark an item as not done |
| `GET` | `/api/todos` | Get all "not done" items |
| `GET` | `/api/todos?includeAll=true` | Get all items regardless of status |
| `GET` | `/api/todos/{id}` | Get details of a specific item |

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
- **Unit tests** (15): Service layer logic with mocked repository
- **Integration tests** (13): Full HTTP request/response cycle with H2 database
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

### Run with Docker

```bash
docker build -t todo-service .
docker run -p 8080:8080 todo-service
```
