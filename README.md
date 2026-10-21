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

## Architecture & Design Decisions

### 1. Transaction & Atomicity Strategy
- The prescription creation workflow (`RecordServiceImpl.createRecord`) is marked `@Transactional`.
- Cascade configuration (`CascadeType.ALL`, `orphanRemoval = true`) ensures that when the parent `Record` entity is persisted, all associated `RecordEntry` items are written within the same database transaction.
- If any validation or database constraint fails midway, the entire transaction rolls back, preventing orphaned medication entries or incomplete prescriptions.

### 2. Idempotency Implementation
- Network timeouts can cause clients to retry requests, resulting in duplicate prescriptions.
- Clients supply a client-generated UUID in `idempotencyKey`.
- A check queries `RecordRepository.findByIdempotencyKey(...)` before entity building.
- Additionally, a database-level `UNIQUE` constraint guarantees zero double-writes under concurrent conditions.

### 3. Clean Separation via DTOs
- JPA entities (`Record`, `User`, `Client`) are never exposed directly to external clients, protecting database internals and avoiding serialization infinite recursion on bidirectional associations.
- Distinct DTOs are maintained: `CreateRecordRequest`, `RecordResponse`, `RecordDetailsResponse`, and `RecordEntryResponse`.

### 4. Schema Versioning vs Schema Auto-Generation
- `hibernate.ddl-auto` is set to `validate`. Hibernate validates entity models against the live schema on startup.
- Flyway serves as the single source of truth for all schema modifications, ensuring zero unvetted schema mutations in production.

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
