# Prescription Management REST API

A production-grade, secure REST API built with **Spring Boot** and **Java 21 (LTS)** for managing digital medical records, patient prescriptions, and medication entries. Implements role-based access control, atomic transactional persistence, idempotency safeguards, database versioning with Flyway, and comprehensive unit and integration testing.

---

## Table of Contents

- [Features](#features)
- [Architecture & Tech Stack](#architecture--tech-stack)
- [Prerequisites](#prerequisites)
- [Setup & Running Locally](#setup--running-locally)
- [Database Migrations](#database-migrations)
- [Authentication & Roles](#authentication--roles)
- [API Documentation & Sample Requests](#api-documentation--sample-requests)
  - [1. Create Prescription Record](#1-create-prescription-record)
  - [2. Get Prescription Record by ID](#2-get-prescription-record-by-id)
  - [3. List Prescription Records (Paginated)](#3-list-prescription-records-paginated)
- [Error Handling & Responses](#error-handling--responses)
- [Architecture & Design Decisions](#architecture--design-decisions)
- [Running Automated Tests](#running-automated-tests)

---

## Features

- **Entity Domain Model**: Fully normalized relational model connecting Users (doctors), Clients (patients), Records (prescriptions), and Record Entries (medications).
- **Atomic Transactions**: Guarantees that prescription records and their associated medication entries persist or roll back together (`@Transactional` with `CascadeType.ALL`).
- **Idempotency Strategy**: Prevents duplicate submissions under network retry conditions via unique idempotency keys.
- **Role-Based Access Control (RBAC)**: HTTP Basic authentication with strictly segregated roles (`DOCTOR` / `ADMIN` can write; `STAFF` has read-only access).
- **Strict Input Validation**: Rejects invalid dates, missing mandatory fields, and negative/zero quantities with descriptive field-level error messages.
- **Centralized Exception Handling**: Uniform JSON error responses across the entire application without leaking internal stack traces.
- **Database Migrations**: Version-controlled PostgreSQL schema evolution via Flyway with Hibernate schema validation (`ddl-auto: validate`).
- **Automated Tests**: 100% test pass rate across unit tests (Mockito) and integration tests (Spring Boot WebMvc & Security).

---

## Architecture & Tech Stack

- **Java**: 21 (Latest LTS)
- **Framework**: Spring Boot 4.x / Spring Framework 7
- **Database**: PostgreSQL
- **ORM & Data**: Spring Data JPA / Hibernate 7
- **Migrations**: Flyway Database Migration
- **Security**: Spring Security 7 (HTTP Basic & RBAC)
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Testing**: JUnit 5, Mockito, Spring Test (MockMvc)
- **Build Tool**: Apache Maven (Maven Wrapper included)

---

## Prerequisites

- **Java Development Kit (JDK)**: Version 21 or later
- **PostgreSQL**: Version 15+ running on port `5433` (or configured via `application.yml`)
- **Maven**: Included via `./mvnw` wrapper

---

## Setup & Running Locally

### 1. Database Creation
Create the target PostgreSQL database:
```sql
CREATE DATABASE prescription_db;
```

### 2. Configure Credentials
Review or adjust [src/main/resources/application.yml](src/main/resources/application.yml) if your local database credentials differ:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/prescription_db
    username: postgres
    password: your_password
```

### 3. Build & Run
Run the application using the Maven wrapper:
```bash
# Compile and package
./mvnw clean package

# Run the Spring Boot application
./mvnw spring-boot:run
```

The application starts on `http://localhost:8080`.

---

## Database Migrations

Flyway automatically applies incremental database migrations at startup:

| Migration | File | Description |
| :--- | :--- | :--- |
| **V1** | `V1__init_schema.sql` | Baseline tables (`users`, `clients`, `records`, `record_entries`) with constraints and foreign keys. |
| **V2** | `V2__add_idempotency_key.sql` | Adds unique `idempotency_key` column to `records`. |
| **V3** | `V3__add_instructions_to_record_entries.sql` | Adds `instructions` column to `record_entries`. |

---

## Authentication & Roles

HTTP Basic Authentication is enforced for all endpoints.

| Username | Password | Role | Permissions |
| :--- | :--- | :--- | :--- |
| `doctor1` | `password123` | `ROLE_DOCTOR` | Full Access: Create prescriptions (`POST`), read records (`GET`) |
| `staff1` | `staffpass` | `ROLE_STAFF` | Read-only: Read records (`GET`); forbidden (`403`) from creating |

---

## API Documentation & Sample Requests

### 1. Create Prescription Record
Creates a medical record with child medication entries atomically.

- **Endpoint**: `POST /api/v1/records`
- **Authorization**: Requires `DOCTOR` or `ADMIN` role.
- **Headers**:
  - `Content-Type: application/json`
  - `Authorization: Basic ZG9jdG9yMTpwYXNzd29yZDEyMw==` (`doctor1:password123`)

#### Sample Request (`curl`)
```bash
curl -X POST http://localhost:8080/api/v1/records \
  -u doctor1:password123 \
  -H "Content-Type: application/json" \
  -d '{
    "userId": 1,
    "clientId": 1,
    "recordDate": "2027-02-10T14:30:00",
    "diagnosis": "Acute Bacterial Bronchitis",
    "idempotencyKey": "REQ-KEY-998822",
    "entries": [
      {
        "entryName": "Amoxicillin 500mg",
        "quantity": 21,
        "frequency": "Three times daily",
        "duration": "7 days",
        "instructions": "Take after meals with a full glass of water"
      },
      {
        "entryName": "Acetaminophen 650mg",
        "quantity": 10,
        "frequency": "Every 6 hours as needed",
        "duration": "3 days",
        "instructions": "Do not exceed 3000mg per day"
      }
    ]
  }'
```

#### Sample Response (`201 Created`)
```json
{
  "recordId": 1,
  "status": "ACTIVE",
  "createdAt": "2026-10-21T22:15:30.123456",
  "message": "Record created successfully"
}
```

---

### 2. Get Prescription Record by ID
Retrieves complete record details including user (doctor), client (patient), diagnosis, and medication entries.

- **Endpoint**: `GET /api/v1/records/{id}`
- **Authorization**: Authenticated users (`DOCTOR`, `STAFF`, `ADMIN`)

#### Sample Request (`curl`)
```bash
curl -X GET http://localhost:8080/api/v1/records/1 \
  -u staff1:staffpass
```

#### Sample Response (`200 OK`)
```json
{
  "id": 1,
  "userId": 1,
  "userName": "Dr. Smith",
  "clientId": 1,
  "clientName": "John Doe",
  "recordDate": "2027-02-10T14:30:00",
  "diagnosis": "Acute Bacterial Bronchitis",
  "status": "ACTIVE",
  "idempotencyKey": "REQ-KEY-998822",
  "entries": [
    {
      "id": 1,
      "entryName": "Amoxicillin 500mg",
      "quantity": 21,
      "frequency": "Three times daily",
      "duration": "7 days",
      "instructions": "Take after meals with a full glass of water"
    },
    {
      "id": 2,
      "entryName": "Acetaminophen 650mg",
      "quantity": 10,
      "frequency": "Every 6 hours as needed",
      "duration": "3 days",
      "instructions": "Do not exceed 3000mg per day"
    }
  ]
}
```

---

### 3. List Prescription Records (Paginated)
Fetches records using page, size, and sorting parameters.

- **Endpoint**: `GET /api/v1/records?page=0&size=10&sort=id,desc`
- **Authorization**: Authenticated users (`DOCTOR`, `STAFF`, `ADMIN`)

#### Sample Request (`curl`)
```bash
curl -X GET "http://localhost:8080/api/v1/records?page=0&size=10&sort=id,desc" \
  -u staff1:staffpass
```

#### Sample Response (`200 OK`)
```json
{
  "content": [
    {
      "id": 1,
      "userId": 1,
      "userName": "Dr. Smith",
      "clientId": 1,
      "clientName": "John Doe",
      "recordDate": "2027-02-10T14:30:00",
      "diagnosis": "Acute Bacterial Bronchitis",
      "status": "ACTIVE",
      "idempotencyKey": "REQ-KEY-998822",
      "entries": [...]
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

## Error Handling & Responses

Errors are caught by `GlobalExceptionHandler` and converted into a standard format:

### Validation Error (`400 Bad Request`)
Returned when required fields are missing or constraints are violated:
```json
{
  "timestamp": "2026-10-21T22:15:30",
  "status": 400,
  "error": "Bad Request",
  "message": "Input validation failed",
  "validationErrors": {
    "userId": "User ID is mandatory",
    "entries": "A record must contain at least one entry",
    "recordDate": "Record date cannot be in the past"
  }
}
```

### Resource Not Found (`404 Not Found`)
Returned when a referenced User ID, Client ID, or Record ID does not exist:
```json
{
  "timestamp": "2026-10-21T22:15:30",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with ID: 999",
  "validationErrors": null
}
```

### Duplicate Request (`409 Conflict`)
Returned when an idempotency key collision is detected:
```json
{
  "timestamp": "2026-10-21T22:15:30",
  "status": 409,
  "error": "Conflict",
  "message": "A record with this idempotency key already exists.",
  "validationErrors": null
}
```

### Forbidden Access (`403 Forbidden`)
Returned when a user lacks the necessary role (e.g., `STAFF` attempting to create records):
```json
{
  "timestamp": "2026-10-21T22:15:30",
  "status": 403,
  "error": "Forbidden",
  "message": "Access Denied"
}
```

---

## Architecture, Transaction Handling, Security, and Validation Decisions

### 1. Architecture Decisions
- **Layered Clean Architecture**: Built using a strict 4-tier separation of concerns:
  - **Controller Layer** (`RecordController`): Handles HTTP routing, serialization, parameter parsing, and delegating to services.
  - **Service Layer** (`RecordServiceImpl`): Houses business rules, transaction boundaries, idempotency verification, and DTO mapping.
  - **Data Access Layer** (`RecordRepository`, `UserRepository`, `ClientRepository`): Spring Data JPA abstractions managing database queries.
  - **Database Layer**: PostgreSQL versioned through Flyway migration scripts.
- **Strict DTO Isolation**: Persistence entities (`Record`, `RecordEntry`, `User`, `Client`) are completely encapsulated within the persistence context and never exposed across REST boundaries. This prevents:
  - Over-posting / mass-assignment vulnerabilities.
  - Hibernate `LazyInitializationException` during JSON serialization.
  - Infinite recursion loops caused by bidirectional associations.
- **Database Versioning with Flyway**: `hibernate.ddl-auto` is set to `validate` rather than `update`. This ensures Flyway remains the single source of truth for schema evolution across development, testing, and production.

---

### 2. Transaction Handling Decisions
- **Aggregate Root Pattern & Cascading**: A prescription record and its medication entries form a unified domain aggregate. By declaring `CascadeType.ALL` and `orphanRemoval = true` on `Record.entries`, persisting the parent `Record` automatically cascades to all child `RecordEntry` items.
- **Declarative Transaction Boundaries (`@Transactional`)**: The `createRecord` method is wrapped in `@Transactional`. If an exception occurs during client retrieval, entry mapping, or database insertion, the entire database transaction rolls back, guaranteeing zero partial writes or orphaned records.
- **Bidirectional Consistency**: The helper method `record.addEntry(entry)` keeps both sides of the Java object graph synchronized (`record.getEntries().add(entry)` and `entry.setRecord(record)`), preventing persistence errors before flushing to the database.

---

### 3. Security Decisions
- **Role-Based Access Control (RBAC)**: Enforces the principle of least privilege:
  - Creating prescriptions (`POST /api/v1/records`) is restricted to verified medical practitioners with `ROLE_DOCTOR` or `ROLE_ADMIN`.
  - Receptionists/assistants (`ROLE_STAFF`) can view prescription records but receive a `403 Forbidden` if attempting to create them.
- **Stateless REST Security**: CSRF protection is disabled (`csrf.disable()`) because the API is stateless and does not use browser session cookies.
- **Credential Hygiene**: Production credentials and database URLs are externalized via environment variables (`SPRING_DATASOURCE_PASSWORD`, etc.) with safe defaults, preventing credential leakage in public repositories.

---

### 4. Validation & Error Handling Decisions
- **Fail-Fast Bean Validation**: Incoming payloads are validated at the controller threshold using Jakarta Bean Validation (`@Valid`, `@NotNull`, `@NotBlank`, `@Min`, `@FutureOrPresent`). Requests with missing fields, negative dosages, or past dates are rejected before executing business logic.
- **Idempotency Strategy**: To protect against duplicate submissions resulting from network timeouts or aggressive client retries:
  - Clients provide a unique `idempotencyKey` with each request.
  - The service layer performs an initial existence lookup (`findByIdempotencyKey`).
  - A database-level `UNIQUE` index acts as a safety barrier against race conditions, rejecting concurrent identical submissions with a `409 Conflict`.
- **Centralized Exception Handling**: `GlobalExceptionHandler` (`@RestControllerAdvice`) intercepts all runtime exceptions and converts them into uniform, machine-readable JSON envelopes. Internal stack traces and SQL dialect details are never exposed to clients, mitigating information disclosure risks.

---

### 5. Key Design Trade-offs
- **Client-Provided Idempotency Key vs. Two-Phase Token Handshake**:
  - *Trade-off*: Requiring clients to submit an idempotency key (UUID) avoids the latency and server-side state of a pre-flight token endpoint (e.g., Redis-based token issuance), while fully preventing duplicate records.
- **Cascade Persistence vs. Independent Entry Service**:
  - *Trade-off*: Treating entries as dependent child entities of `Record` simplifies API usage (single request creates record + all medications) at the cost of requiring full prescription payloads for updates. This aligns with standard medical workflows where a prescription is issued as an immutable snapshot.
- **DTO Mapping vs. Direct Entity Returns**:
  - *Trade-off*: Writing mapping logic introduces minor boilerplate, but eliminates circular reference bugs, protects entity state, and gives complete control over the external JSON contract.

---

## Running Automated Tests

Run the full test suite (unit tests + integration tests) via the Maven wrapper:

```bash
./mvnw test
```

### Test Scope:
- **`RecordServiceImplTest`**: 7 unit tests testing core business logic, duplicate key rejections, 404 lookups, and pagination mapping using Mockito.
- **`RecordControllerIntegrationTest`**: 9 end-to-end integration tests using `MockMvc` verifying HTTP status codes, security role enforcement (`DOCTOR` vs `STAFF`), validation handling, and error response formatting.
- **`PrescriptionApiApplicationTests`**: Context load verification.
