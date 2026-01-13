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

### Testing Requirements

- **All new logic must have comprehensive tests** - any code written to service classes, model classes, controller classes, or other components must include tests that provide near 100% test coverage.
- **Update tests when modifying existing code** - when making changes to existing code, update the corresponding tests to maintain coverage and prevent regressions.
- **Run tests after changes** - always run `./mvnw test` after making code changes to verify no regressions occur and all tests pass.

## Database Migrations

- **Always use `./scripts/create-migration.sh <name>` to create new migration files** - never manually generate migration filenames. The script ensures correct Flyway naming conventions with proper timestamps.
- **Prefer new migrations over modifying existing ones** - when troubleshooting or updating models, create a new migration rather than editing an existing migration file, unless the user explicitly requests modification of an existing migration.

## Environment

Create `.env` in project root:
```
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
```