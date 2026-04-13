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
│   ├── AuthController.java    # /api/auth/* (logout, me)
│   ├── DevAuthController.java # /api/auth/dev-login (@Profile("dev") only)
│   ├── UserController.java    # /api/users/*
│   ├── AdminController.java   # /api/admin/* (ADMIN/OWNER only)
│   └── dh/                    # Daggerheart-specific endpoints
│       ├── CharacterSheetController.java
│       ├── CampaignController.java
│       ├── AdversaryController.java
│       └── [Card/Item Controllers...]
├── service/                   # Business logic layer
│   ├── AuthenticationService.java         # Token issuance and revocation
│   ├── OAuth2UserProvisioningService.java # Find-or-create user from OAuth2 principal
│   ├── UserService.java              # User CRUD, profile management
│   ├── RoleHierarchyService.java     # Role comparison & permission checks
│   ├── TokenCleanupService.java      # Scheduled cleanup of expired tokens
│   └── dh/                           # Daggerheart services
│       ├── CharacterSheetService.java
│       ├── CampaignService.java
│       ├── AdversaryService.java
│       └── [Card/Item Services...]
├── repository/                # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── UserIdentityRepository.java
│   ├── ActiveTokenRepository.java
│   └── dh/                    # Daggerheart repositories
├── model/
│   ├── entity/                # JPA entities
│   │   ├── User.java          # Core user entity
│   │   ├── BaseEntity.java    # Abstract base (id, createdAt, lastModifiedAt)
│   │   ├── UserIdentity.java  # OAuth provider identity linked to a User
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
│   ├── JwtTokenProvider.java             # Token generation/validation
│   ├── JwtAuthenticationFilter.java      # Request filter
│   ├── JwtAuthenticationEntryPoint.java
│   ├── CustomUserDetails.java            # UserDetails implementation
│   ├── OAuth2LoginSuccessHandler.java    # Issues JWT cookie after OAuth2 success
│   └── OAuth2LoginFailureHandler.java    # Redirects to frontend on OAuth2 failure
├── exception/                 # Exception handling
│   ├── GlobalExceptionHandler.java  # @ControllerAdvice
│   └── [Custom exceptions...]
└── util/                      # Utility classes
    └── CookieUtil.java        # JWT cookie management
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
1. **Public**: `/oauth2/**`, `/login/oauth2/**`, `/api/auth/logout`, `/api/auth/dev-login` (dev profile only)
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

### Search Indexing

Full-text search uses a centralized `search_index` table backed by PostgreSQL TSVECTOR:
- **Weighted fields**: A=name, B=description, C=features/tags
- **Auto-indexing**: Spring events (`EntityChangeEvent`) trigger `SearchIndexEventListener` to keep the index current
- **Access control**: Search queries filter by `isOfficial`, `isPublic`, ownership, and privileged roles
- **Expansion**: `SearchService.resolveEntity()` maps indexed types to full entity lookups for `?expand=` support

#### Search Indexing Checklist — Adding a New Searchable Entity

When adding a new entity that should appear in search results, complete all of the following steps:

1. **Annotate the entity** — add `@SearchIndexed(type = SearchableEntityType.XXX)` to the entity class
2. **Register the type** — add the new constant to the `SearchableEntityType` enum
3. **Map the fields** — add a builder method in `SearchFieldMapping` that defines the A/B/C weight assignments for the new type
4. **Publish change events** — in the entity's service, publish `EntityChangeEvent` after create, update, and soft-delete operations
5. **Backfill existing data** — create a Flyway migration to populate `search_index` for any rows that already exist in the database
6. **Support expansion** — add a case for the new type in `SearchService.resolveEntity()` so it works with `?expand=`

## Database Migrations

- **Always use `./scripts/create-migration.sh <name>`** - Never manually create migration filenames
- **Always use new migrations over modifying existing** - Create new migration for schema changes
- Naming: `V{timestamp}__{description}.sql`

## Controller/Endpoint Updates

- Whenever adding or updating an Endpoint, Controller, DTO, Model, or similar used by an endpoint, always review the .api-blueprint directory and update any relevant files accordingly. 

## Environment

Create `.env` in project root:
```
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
JWT_SECRET=your-secure-256-bit-secret-here-change-in-production
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
FRONTEND_BASE_URL=http://localhost:5173
```

## Key Configuration (application.yaml)

- JWT expiration: 30 days
- OAuth2: Google provider (client-id/secret via `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` env vars)
- Frontend base URL: configurable via `FRONTEND_BASE_URL` env var (used for post-OAuth redirects)
- Dev login: available only with `SPRING_PROFILES_ACTIVE=dev`

## Testing Requirements

- Unit tests: `{ClassName}Test`
- Integration tests: `{ClassName}IntegrationTest`
- Target: 80%+ coverage for all service/controller code
- Run tests after changes: `./mvnw test`


<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:ca08a54f -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

## Session Completion

**When ending a work session**, you MUST complete ALL steps below. Work is NOT complete until `git push` succeeds.

**MANDATORY WORKFLOW:**

1. **File issues for remaining work** - Create issues for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **PUSH TO REMOTE** - This is MANDATORY:
   ```bash
   git pull --rebase
   bd dolt push
   git push
   git status  # MUST show "up to date with origin"
   ```
5. **Clean up** - Clear stashes, prune remote branches
6. **Verify** - All changes committed AND pushed
7. **Hand off** - Provide context for next session

**CRITICAL RULES:**
- Work is NOT complete until `git push` succeeds
- NEVER stop before pushing - that leaves work stranded locally
- NEVER say "ready to push when you are" - YOU must push
- If push fails, resolve and retry until it succeeds
<!-- END BEADS INTEGRATION -->
