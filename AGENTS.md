# AGENTS.md - Daggerheart TTRPG Application

## Application Overview
This is a Spring Boot backend application that provides tools for running TTRPGs, specifically Daggerheart. It uses Java 25, PostgreSQL database, and includes authentication/security features.

## Quick Start Commands

### Database Operations
```bash
# Start database
./scripts/start-db.sh

# Stop database  
./scripts/stop-db.sh

# Connect to database
./scripts/connect-db.sh

# Drop database (development only)
./scripts/drop-db.sh
```

### Application Management
```bash
# Start the application
./mvnw spring-boot:run

# Clean build and start
./mvnw clean spring-boot:run

# Build JAR
./mvnw clean package

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Skip tests during development
./mvnw spring-boot:run -Dmaven.test.skip=true
```

### Testing
```bash
# Run all tests
./mvnw test

# Run unit tests only
./mvnw test -Dtest="*Test"

# Run integration tests
./mvnw test -Dtest="*IntegrationTest"

# Generate test coverage report
./mvnw jacoco:report
```

### Database Migrations
```bash
# Create new migration
./scripts/create-migration.sh migration_name

# This creates: src/main/resources/db/migration/V{timestamp}__migration_name.sql
```

## Development Guidelines

### Unit Testing Requirements
- **ALWAYS** create or update unit tests with any new code/modifications
- Aim for high test coverage, but can skip:
  - Lombok annotations
  - Simple getters/setters
  - Trivial configuration classes
- Test classes should follow naming convention: `{ClassName}Test`
- Integration tests should be named: `{ClassName}IntegrationTest`

### Code Organization
- Main application: `src/main/java/com/aboff/core/`
- Tests: `src/test/java/com/aboff/core/`
- Database migrations: `src/main/resources/db/migration/`
- Utility scripts: `scripts/`

## Current Application Structure

### Core Components
- **CoreApplication.java**: Main Spring Boot application entry point
- **CoreApplicationTests.java**: Basic application context test

### Key Dependencies
- Spring Boot 4.0.1 with Web MVC
- PostgreSQL database with Flyway migrations
- Spring Security with OAuth2 client support
- Lombok for reducing boilerplate code
- Comprehensive testing support (Spring Boot Test, Security Test)

## Environment Setup
Create `.env` file in project root:
```bash
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
```

## Application Access
- Application runs on: `http://localhost:8080`
- Database: PostgreSQL on localhost:5432

---
*This file should be updated as new sections are added to the application or important dependencies are changed.*
