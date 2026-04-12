# Core Backend Project

A Java 25 Spring Boot 4.0.1 application with PostgreSQL database, OAuth2 authentication (Google), and JWT-based session management via HttpOnly cookies.

## Database Setup

### Prerequisites
- Docker and Docker Compose installed
- Java 25
- Maven

### Environment Configuration
The application uses environment variables for database configuration. Create a `.env` file in the project root:

```bash
# Database
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password

# JWT Configuration
JWT_SECRET=your-secure-256-bit-secret-here-change-in-production

# Google OAuth2 (optional for local dev with dev-login endpoint)
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret

# Frontend URL (where the SPA lives)
FRONTEND_BASE_URL=http://localhost:4200
```

**Important**: Generate a secure JWT secret for production:
```bash
openssl rand -base64 32
```

### Database Operations

#### Start Database
```bash
docker compose up -d
```

#### Stop Database
```bash
docker compose down
```

#### Connect to Database
```bash
./scripts/connect-db.sh
```

Or manually:
```bash
docker compose exec postgres psql -U postgres -d heartandfear
```

#### Drop Database (Development)
```bash
./scripts/drop-db.sh
```

### Flyway Migrations
Place SQL migration files in `src/main/resources/db/migration/`. Flyway will automatically apply them on application startup.

### Running the Application
1. Start the database: `./scripts/start-db.sh`
2. Run the application: `./mvnw spring-boot:run`

The application will be available at `http://localhost:8080`.

### Maven Commands

#### Start the Application
```bash
./mvnw spring-boot:run
```

#### Development Commands
```bash
# Clean and start
./mvnw clean spring-boot:run

# Build and run JAR
./mvnw clean package
java -jar target/core-0.0.1-SNAPSHOT.jar

# Skip tests during development
./mvnw spring-boot:run -Dmaven.test.skip=true

# Run with dev profile (enables dev-login endpoint)
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

## Scripts
Use the scripts in the `scripts/` directory for common database operations:
- `start-db.sh` - Start the database
- `stop-db.sh` - Stop the database
- `connect-db.sh` - Connect to the database
- `drop-db.sh` - Drop the database (development only)
- `create-migration.sh` - Generate a blank Flyway migration file

### Creating Migrations
```bash
# Create a new migration file
./scripts/create-migration.sh create_users_table

# This creates: src/main/resources/db/migration/V20260107093512345__create_users_table.sql
```

## Authentication

This application uses **OAuth2-only** authentication. There is no username/password login. Users authenticate via Google (with Discord/Steam ready to add).

For full architecture details, see [docs/authentication.md](docs/authentication.md).

### How It Works

1. User clicks "Sign in with Google" in the SPA
2. SPA navigates to `GET /oauth2/authorization/google`
3. Spring Security redirects to Google's consent screen
4. Google redirects back with an authorization code
5. The backend exchanges the code, provisions a User + UserIdentity if new
6. A JWT is issued and set as an `AUTH_TOKEN` HttpOnly cookie
7. The user is redirected back to the frontend (`FRONTEND_BASE_URL`)

All subsequent API calls are authenticated via the `AUTH_TOKEN` cookie, validated by `JwtAuthenticationFilter`.

### Security Features

- **OAuth2-only login**: No passwords to leak or brute-force
- **JWT Tokens**: 30-day expiration, stored in HttpOnly cookies
- **HttpOnly Cookies**: XSS protection (JavaScript cannot access tokens)
- **SameSite=Strict**: CSRF protection (browser blocks cross-site requests)
- **Server-side token tracking**: Active tokens stored in DB for revocation
- **Multi-device Support**: Each login creates a separate token
- **Soft Delete**: Users marked as deleted without data loss

### Google OAuth Setup

To use real Google authentication (required for staging/production):

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a project and configure the OAuth consent screen
3. Create an OAuth Client ID (Web application type)
4. Add authorized redirect URIs:
   ```
   http://localhost:8080/login/oauth2/code/google
   https://your-production-domain/login/oauth2/code/google
   ```
5. Set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` in your `.env` file

See [docs/authentication.md](docs/authentication.md) for detailed GCP setup steps.

### Dev Login Endpoint (Local Development)

For local development and testing, a mock login endpoint is available when the `dev` profile is active. This lets you authenticate without setting up Google OAuth.

**Start with dev profile:**
```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

**Create a user and get an auth cookie:**
```bash
# Create a regular user
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"email": "alice@example.com"}'

# Create an admin user
curl -i -c cookies.txt -X POST http://localhost:8080/api/auth/dev-login \
  -H 'Content-Type: application/json' \
  -d '{"email": "admin@example.com", "role": "ADMIN"}'
```

**Verify the session:**
```bash
curl -i -b cookies.txt http://localhost:8080/api/auth/me
# Returns: {"id":1,"username":"alice","role":"USER","email":"alice@example.com",...}
```

**Logout:**
```bash
curl -i -b cookies.txt -X POST http://localhost:8080/api/auth/logout
```

The dev-login endpoint is **not available** without the dev profile. In staging/production, authentication goes through the real Google OAuth flow.

### Role Promotion

All new users are created with the `USER` role. To promote yourself to `OWNER` after first login:

```bash
./scripts/connect-db.sh
# Then run:
UPDATE users SET role = 'OWNER' WHERE email = 'your-email@example.com';
```

## API Endpoints

### Authentication Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/oauth2/authorization/google` | Public | Initiates Google OAuth2 login flow |
| GET | `/api/auth/me` | Required | Returns current authenticated user |
| POST | `/api/auth/logout` | Public | Revokes token and clears cookie |
| POST | `/api/auth/dev-login` | Public (dev only) | Dev mock login with email + optional role |

### User Management Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| GET | `/api/users/me` | Required | Get current user's profile |
| GET | `/api/users/{userId}` | Required | Get a user's profile by ID |
| GET | `/api/users/{userId}/campaigns` | Required | Get a user's campaigns |
| PATCH | `/api/users/me` | Required | Update current user's profile |
| DELETE | `/api/users/me` | Required | Soft-delete current user's account |

### Admin Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/admin/users/{userId}/ban` | MODERATOR+ | Ban a user |
| POST | `/api/admin/users/{userId}/unban` | MODERATOR+ | Unban a user |
| POST | `/api/admin/users/{userId}/change-role` | OWNER | Change a user's role |
| POST | `/api/admin/search/reindex` | OWNER | Rebuild search index |

See the `.api-blueprint/` directory for detailed endpoint documentation.

## Testing

### Run All Tests

```bash
# Unit tests only (surefire)
./mvnw test

# Unit + integration tests (surefire + failsafe)
./mvnw verify
```

### Run Specific Test Types

```bash
# Unit tests matching a pattern
./mvnw test -Dtest="*ServiceTest"

# A specific integration test
./mvnw verify -Dit.test=DevAuthControllerIntegrationTest

# A specific unit test class
./mvnw test -Dtest=AuthenticationServiceTest
```

## Production Deployment

### Production Profile

Run with the `prod` profile:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/core-0.0.1-SNAPSHOT.jar
```

Required environment variables for production:
- `JWT_SECRET` — secure 256-bit secret
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` — from GCP
- `FRONTEND_BASE_URL` — e.g., `https://ohsheet.aboff.com`
- `CORS_ALLOWED_ORIGINS` — e.g., `https://ohsheet.aboff.com`
- `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_HOST`, `DB_PORT`

### Security Checklist

- [ ] Generate secure JWT secret (256-bit minimum)
- [ ] Set `JWT_COOKIE_SECURE=true` for HTTPS-only cookies
- [ ] Configure Google OAuth with production redirect URI
- [ ] Use strong database password
- [ ] Enable HTTPS/TLS
- [ ] Configure CORS for your frontend domain only
- [ ] Verify `dev` profile is NOT active
- [ ] Set up database backups

## Architecture

### Package Structure

```
com.aboff.core/
├── config/           # Configuration (SecurityConfig)
├── controller/       # REST API endpoints (AuthController, DevAuthController, UserController, AdminController)
│   └── dh/          # Daggerheart-specific controllers
├── service/          # Business logic (AuthenticationService, OAuth2UserProvisioningService, UserService)
│   └── dh/          # Daggerheart-specific services
├── repository/       # Data access (Spring Data JPA)
│   └── dh/          # Daggerheart-specific repositories
├── model/
│   ├── entity/      # JPA entities (User, UserIdentity, ActiveToken)
│   │   └── dh/     # Daggerheart entities
│   ├── dto/         # Data transfer objects (request/response)
│   └── enums/       # Enumerations (Role, Trait, etc.)
├── security/        # JWT + OAuth2 (JwtTokenProvider, JwtAuthenticationFilter, OAuth2LoginSuccessHandler)
├── exception/       # Custom exceptions and GlobalExceptionHandler
└── util/            # Utilities (CookieUtil)
```

### Database Schema

- **users**: User accounts (OAuth-provisioned, no passwords)
- **user_identities**: OAuth provider identities linked to users (provider, provider_sub)
- **active_tokens**: Active JWT token hashes for validation and revocation

## Troubleshooting

### Common Issues

**Database Connection Failed**
```bash
# Ensure database is running
docker ps | grep postgres

# Restart database
./scripts/stop-db.sh && ./scripts/start-db.sh

# Check database logs
docker compose logs postgres
```

**Tests Failing**
```bash
# Clean and run tests
./mvnw clean verify
```

**401 Unauthorized on Protected Endpoints**
- Ensure you're including the `AUTH_TOKEN` cookie
- Token may have expired (30-day expiration)
- Token may have been revoked
- Check if account was deleted or banned

**OAuth2 Login Not Working**
- Ensure `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` are set
- Verify the redirect URI in GCP matches your backend URL
- For local dev, use the dev-login endpoint instead
