# CLAUDE.md

## Project Overview

Java 25 Spring Boot 4.0.1 backend for a Daggerheart TTRPG character management application. Uses PostgreSQL with Flyway migrations, Spring Security with JWT authentication (HttpOnly cookies), and Lombok.

The application enables users to create and manage character sheets, campaigns, game content (weapons, armor, cards), and adversaries for the Daggerheart tabletop RPG.

## Critical Considerations

**MANDATORY:** After all code changes, you MUST validate tests are successful (all green), lint checks are successful (all green), and the application builds successfully.

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

### Package Structure

```
com.aboff.core/
├── config/                    # Spring configuration
│   └── SecurityConfig.java    # Spring Security & CORS config
├── controller/                # REST API endpoints
│   ├── AuthController.java    # /api/auth/* (login, register, logout)
│   ├── UserController.java    # /api/users/*
│   ├── AdminController.java   # /api/admin/* (ADMIN/OWNER only)
│   └── dh/                    # Daggerheart-specific endpoints
│       ├── CharacterSheetController.java
│       ├── CampaignController.java
│       ├── AdversaryController.java
│       └── [Card/Item Controllers...]
├── service/                   # Business logic layer
│   ├── AuthenticationService.java    # Login, registration, password validation
│   ├── UserService.java              # User CRUD, profile management
│   ├── RoleHierarchyService.java     # Role comparison & permission checks
│   ├── LoginAttemptService.java      # Audit logging for login attempts
│   ├── TokenCleanupService.java      # Scheduled cleanup of expired tokens
│   └── dh/                           # Daggerheart services
│       ├── CharacterSheetService.java
│       ├── CampaignService.java
│       ├── AdversaryService.java
│       └── [Card/Item Services...]
├── repository/                # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── ActiveTokenRepository.java
│   ├── LoginAttemptRepository.java
│   └── dh/                    # Daggerheart repositories
├── model/
│   ├── entity/                # JPA entities
│   │   ├── User.java          # Core user entity
│   │   ├── BaseEntity.java    # Abstract base (id, createdAt, lastModifiedAt)
│   │   ├── LoginAttempt.java  # Login audit trail
│   │   ├── ActiveToken.java   # JWT token tracking
│   │   └── dh/                # Daggerheart entities
│   │       ├── CharacterSheet.java  # Player character data
│   │       ├── Campaign.java        # Campaign with players/GMs
│   │       ├── Adversary.java       # NPCs/enemies
│   │       ├── Card.java            # Abstract card base (JOINED inheritance)
│   │       ├── BaseItem.java        # Abstract item base
│   │       └── [Concrete entities...]
│   ├── dto/                   # Data transfer objects
│   │   ├── request/           # Core request DTOs
│   │   ├── response/          # Core response DTOs
│   │   └── dh/                # Daggerheart DTOs
│   │       ├── request/
│   │       └── response/
│   ├── enums/                 # Enumeration types
│   │   ├── Role.java          # OWNER > ADMIN > MODERATOR > USER
│   │   ├── Trait.java         # AGILITY, STRENGTH, FINESSE, etc.
│   │   └── [Game enums...]
│   └── embeddable/            # JPA embeddable components
│       └── DamageRoll.java    # Dice + modifier + damage type
├── security/                  # JWT authentication infrastructure
│   ├── JwtTokenProvider.java        # Token generation/validation
│   ├── JwtAuthenticationFilter.java # Request filter
│   ├── JwtAuthenticationEntryPoint.java
│   └── CustomUserDetails.java       # UserDetails implementation
├── exception/                 # Exception handling
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice
│   └── [Custom exceptions...]
└── util/                      # Utility classes
    ├── CookieUtil.java        # JWT cookie management
    └── PasswordValidator.java # Password strength validation
```

### Key Paths

- **Source**: `src/main/java/com/aboff/core/`
- **Tests**: `src/test/java/com/aboff/core/`
- **Migrations**: `src/main/resources/db/migration/`
- **Config**: `src/main/resources/application.yaml`

## Core Patterns

### Entity Inheritance

- **BaseEntity**: All entities extend this for `id`, `createdAt`, `lastModifiedAt`
- **Card (JOINED)**: Abstract base for AncestryCard, CommunityCard, SubclassCard, DomainCard
- **BaseItem**: Abstract base for Weapon, Armor, Loot

### Soft Deletion

Most entities support soft deletion via `deletedAt` field:
- `isDeleted()`, `softDelete()`, `restore()` methods
- Repositories filter deleted items: `findAllActive()`, `findActiveById()`
- Use `@Query` with `deletedAt IS NULL` for custom queries

### Access Control

Three-tiered access model:
1. **Public**: `/api/auth/register`, `/api/auth/login`
2. **Authenticated**: All other endpoints require valid JWT
3. **Role-based**: MODERATOR+ can bypass ownership checks

**Important**: All role hierarchy checks should use `RoleHierarchyService`:
- `isHigherRole(actor, target)` - Compare two roles
- `hasRoleOrHigher(user, minimumRole)` - Check minimum role requirement
- `requireRoleOrHigher(user, role)` - Throws if insufficient
- `isPrivilegedRole(role)` - Checks for MODERATOR/ADMIN/OWNER

### Response Expansion

API endpoints support `?expand=field1,field2` to include related objects:
```
GET /api/dh/character-sheets/1?expand=owner,experiences,inventoryWeapons
```

Services implement `parseExpand()` and conditional inclusion in `toResponse()`.

### Content Management

Game content uses official/public/custom pattern:
- `isOfficial` - Official game content (OWNER-only modification)
- `isPublic` - Visible to all users
- `originalItem` - Self-reference for tracking copies of official content

## Database Migrations

- **Always use `./scripts/create-migration.sh <name>`** - Never manually create migration filenames
- **Prefer new migrations over modifying existing** - Create new migration for schema changes
- Naming: `V{timestamp}__{description}.sql`

## Environment

Create `.env` in project root:
```
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
JWT_SECRET=your-secure-256-bit-secret-here-change-in-production
```

## Key Configuration (application.yaml)

- JWT expiration: 30 days
- Failed login lockout: 5 attempts, 30 minute lockout
- Password: min 8 chars, requires upper/lower/digit/special
- BCrypt strength: configurable via `application.security.bcrypt-strength` (default 12, tests use 4)

## Testing Requirements

- Unit tests: `{ClassName}Test`
- Integration tests: `{ClassName}IntegrationTest`
- Target: 80%+ coverage for all service/controller code
- Run tests after changes: `./mvnw test`
