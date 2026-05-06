# Ops-Ready User Task API

Operations-focused Spring Boot REST API for controlled user/task fixes, safe change execution, and automation-friendly workflows.

## Why this project

This project is built to simulate Operations Engineering practices:

- Controlled account/data fixes through API endpoints
- Safe mutation workflows with idempotency keys and request IDs
- Structured diagnostics with standardized error contracts
- Traceability with audit timestamps (`createdAt`, `updatedAt`)
- Scriptable automation for repeatable operational changes

## Features

- User and task CRUD APIs with layered architecture
- Request validation using Bean Validation
- Standardized error response format with error codes
- Precise HTTP statuses (`400`, `404`, `409`, `422`, `500`)
- Business rule enforcement for task status transitions:
  - `TODO -> IN_PROGRESS -> DONE`
- Idempotency support for create/update mutations via `Idempotency-Key`
- Request correlation using `X-Request-Id`
- Structured mutation logs for operational diagnostics
- Integration test coverage for positive and negative flows
- Python scripts for controlled data changes with dry-run and confirmation

## Tech stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA (Hibernate)
- H2 Database
- Bean Validation
- SpringDoc OpenAPI (Swagger UI)
- JUnit 5 + MockMvc
- Maven
- Python 3 (automation scripts)

## Project structure

- `src/main/java/com/tamar/user_task_api/controller` - REST endpoints
- `src/main/java/com/tamar/user_task_api/service` - business logic, idempotency
- `src/main/java/com/tamar/user_task_api/repository` - data access
- `src/main/java/com/tamar/user_task_api/entity` - JPA entities + audit fields
- `src/main/java/com/tamar/user_task_api/dto` - request/response models
- `src/main/java/com/tamar/user_task_api/exception` - custom exceptions + handlers
- `src/main/java/com/tamar/user_task_api/config` - request ID filter
- `src/test/java/com/tamar/user_task_api` - integration tests
- `scripts` - operational Python automation scripts

## Data model

- `User`
  - `id`, `name`, `email`, `createdAt`, `updatedAt`
- `Task`
  - `id`, `title`, `description`, `status`, `user`, `createdAt`, `updatedAt`
- Relationship
  - One `User` to many `Task` (`OneToMany` / `ManyToOne`)

## Run locally

### Prerequisites

- JDK 21
- Maven Wrapper (`mvnw.cmd`, already included)
- Python 3 (optional, for scripts)

### Start application

```powershell
.\mvnw.cmd spring-boot:run
```

App URL: `http://localhost:8081`

### Run tests

```powershell
.\mvnw.cmd test
```

## Useful URLs

- Swagger UI: `http://localhost:8081/swagger-ui.html`
- OpenAPI docs: `http://localhost:8081/v3/api-docs`
- H2 console: `http://localhost:8081/h2-console`

H2 values:

- JDBC URL: `jdbc:h2:mem:usertaskdb`
- Username: `sa`
- Password: *(empty)*

## API endpoints

### Users

- `POST /api/users`
- `GET /api/users`
- `GET /api/users/{id}`
- `PUT /api/users/{id}`
- `DELETE /api/users/{id}`

### Tasks

- `POST /api/tasks`
- `GET /api/tasks`
- `GET /api/tasks/{id}`
- `PUT /api/tasks/{id}`
- `DELETE /api/tasks/{id}`

## Operational safety contracts

### Request ID

- Send optional `X-Request-Id` header.
- If missing, server generates one and returns it in response headers.

### Idempotency

- Send optional `Idempotency-Key` on `POST`/`PUT`.
- Same key + same payload returns cached response.
- Same key + different payload returns `409 CONFLICT`.

### Error response format

```json
{
  "timestamp": "2026-05-06T21:26:52.6188601",
  "status": 422,
  "error": "Unprocessable Entity",
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Invalid task status transition: TODO -> DONE",
  "path": "/api/tasks/1",
  "requestId": "1cf18c1d-cafd-41bc-b2ed-f4bd4857f6f7",
  "validationErrors": null
}
```

## Automation scripts

### 1) Fix a user email

Dry run:

```powershell
python scripts/fix_user_email.py --user-id 1 --new-email new@email.com --dry-run
```

Execute:

```powershell
python scripts/fix_user_email.py --user-id 1 --new-email new@email.com --yes
```

### 2) Bulk update task statuses

Dry run:

```powershell
python scripts/bulk_task_status_update.py --from-status TODO --to-status IN_PROGRESS --dry-run
```

Execute with user filter:

```powershell
python scripts/bulk_task_status_update.py --from-status IN_PROGRESS --to-status DONE --user-id 1 --yes
```

## Test coverage highlights

Integration tests include:

- User/task CRUD happy paths
- Validation errors
- Missing resources (`404`)
- Duplicate email conflicts (`409`)
- Invalid status transitions (`422`)
- Idempotent create behavior with repeated idempotency key

## Screenshots

Postman/API screenshots and H2 screenshots are available in `screenshots/`.