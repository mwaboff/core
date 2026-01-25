# CLAUDE.md

## Project Overview

Java 25 Spring Boot 4.0.1 backend with JWT-based authentication. Uses PostgreSQL with Flyway migrations, Spring Security, and Lombok.

This application allows users to create characters in the Daggerheart TTRPG. It manages character sheets, domain cards, and other useful features for GMs and players alike.

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
./scripts/drop-db.sh               # Drop database (development only)
./scripts/create-migration.sh name # Create Flyway migration
```

## Architecture

Package structure follows layered architecture under `com.aboff.core/`:
- `config/` - Configuration classes (SecurityConfig, JwtConfig)
- `controller/` - REST API endpoints
- `service/` - Business logic
- `repository/` - Data access (Spring Data JPA)
- `model/entity/` - JPA entities (User, LoginAttempt, ActiveToken)
- `model/dto/` - Data transfer objects (request/response)
- `model/enums/` - Enumerations
- `exception/` - Custom exceptions and global handler
- `security/` - JWT infrastructure (filters, providers)

Key paths:
- Source: `src/main/java/com/aboff/core/`
- Tests: `src/test/java/com/aboff/core/`
- Migrations: `src/main/resources/db/migration/`

## Database Migrations

- **Always use `./scripts/create-migration.sh <name>` to create new migration files** - never manually generate migration filenames. The script ensures correct Flyway naming conventions with proper timestamps.
- **Prefer new migrations over modifying existing ones** - when troubleshooting or updating models, create a new migration rather than editing an existing migration file, unless the user explicitly requests modification of an existing migration.

## Environment

Create `.env` in project root:
```
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password

JWT_SECRET=your-secure-256-bit-secret-here-change-in-production
```