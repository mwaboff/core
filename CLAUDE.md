# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Java 25 Spring Boot 4.0.1 backend. Uses PostgreSQL with Flyway migrations, Spring Security with OAuth2, and Lombok.

## Commands

```bash
# Start application (requires database)
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=CoreApplicationTests

# Run unit tests only / integration tests only
./mvnw test -Dtest="*Test"
./mvnw test -Dtest="*IntegrationTest"

# Database operations
./scripts/start-db.sh              # Start PostgreSQL (Docker)
./scripts/stop-db.sh               # Stop database
./scripts/connect-db.sh            # Connect to database CLI
./scripts/create-migration.sh name # Create Flyway migration
```

## Architecture

Package structure follows layered architecture under `com.aboff.core/`:
- `config/` - Configuration classes
- `controller/` - REST API endpoints
- `service/` - Business logic
- `repository/` - Data access (Spring Data JPA)
- `model/entity/`, `model/dto/`, `model/enums/` - Domain models
- `exception/` - Custom exceptions
- `security/` - Security configurations

Key paths:
- Source: `src/main/java/com/aboff/core/`
- Tests: `src/test/java/com/aboff/core/`
- Migrations: `src/main/resources/db/migration/`

## Testing

- Test naming: `{ClassName}Test` for unit tests, `{ClassName}IntegrationTest` for integration tests
- Skip coverage for: Lombok annotations, simple getters/setters, trivial configs

## Environment

Create `.env` in project root:
```
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
```