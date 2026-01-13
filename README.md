# Core Backend Project

A Java 25 Spring Boot 4.0.1 application with PostgreSQL database, JWT-based authentication, and comprehensive security features.

## Database Setup

### Prerequisites
- Docker and Docker Compose installed
- Java 25
- Maven

### Environment Configuration
The application uses environment variables for database configuration. Create a `.env` file in the project root:

```bash
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password
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

### Application Configuration
The application automatically reads database connection details from environment variables:
- Database URL: `jdbc:postgresql://localhost:5432/${POSTGRES_DB:heartandfear}`
- Username: `${POSTGRES_USER:postgres}`
- Password: `${POSTGRES_PASSWORD:password}`

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

# Run with specific profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
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

## Authentication & Security

This application uses JWT-based authentication with httpOnly cookies for secure, stateless authentication.

### Security Features

- **JWT Tokens**: 30-day expiration, stored in httpOnly cookies
- **httpOnly Cookies**: XSS protection (JavaScript cannot access tokens)
- **SameSite=Strict**: Automatic CSRF protection
- **CSRF Tokens**: Additional protection for state-changing requests
- **BCrypt Password Hashing**: Strength 12 (~300ms per hash)
- **Account Locking**: 5 failed attempts in 15 minutes = 30-minute lock
- **Multi-device Support**: Each login creates a separate token
- **Token Invalidation**: Password change revokes ALL user tokens
- **Soft Delete**: Users marked as deleted without data loss

### Password Requirements

Passwords must meet the following criteria:
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one digit (0-9)
- At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)

### Environment Variables

Add these to your `.env` file:

```bash
# Database
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password

# JWT Configuration
JWT_SECRET=your-secure-256-bit-secret-here-change-in-production
```

**Important**: Generate a secure JWT secret for production:
```bash
openssl rand -base64 32
```

## API Endpoints

### Authentication Endpoints

#### Register User

**POST** `/api/auth/register`

Create a new user account.

**Request Body**:
```json
{
  "username": "johndoe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "timezone": "America/New_York",
  "avatarUrl": "https://example.com/avatar.jpg"
}
```

**Required Fields**: `username`, `email`, `password`
**Optional Fields**: `timezone` (defaults to UTC), `avatarUrl` (uses default if not provided)

**Response** (201 Created):
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "avatarUrl": "https://example.com/avatar.jpg",
  "timezone": "America/New_York",
  "createdAt": "2026-01-13T10:30:00",
  "lastModifiedAt": "2026-01-13T10:30:00"
}
```

**Error Responses**:
- `409 Conflict`: Username or email already exists
- `400 Bad Request`: Invalid data or weak password

**Example**:
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "timezone": "America/New_York"
  }'
```

#### Login

**POST** `/api/auth/login`

Authenticate with username/email and password. Sets httpOnly `AUTH_TOKEN` cookie.

**Request Body**:
```json
{
  "usernameOrEmail": "johndoe",
  "password": "SecurePass123!"
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "avatarUrl": "https://example.com/avatar.jpg",
  "timezone": "America/New_York",
  "createdAt": "2026-01-13T10:30:00",
  "lastModifiedAt": "2026-01-13T10:30:00"
}
```

**Headers**:
```
Set-Cookie: AUTH_TOKEN=eyJhbGci...; Max-Age=2592000; Path=/; HttpOnly; SameSite=Strict
Set-Cookie: XSRF-TOKEN=...; Path=/
```

**Error Responses**:
- `401 Unauthorized`: Invalid credentials
- `403 Forbidden`: Account is locked
- `404 Not Found`: User not found or deleted

**Example**:
```bash
# Login and save cookies
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "usernameOrEmail": "johndoe",
    "password": "SecurePass123!"
  }'
```

**Account Locking**: After 5 failed login attempts within 15 minutes, the account is locked for 30 minutes.

#### Logout

**POST** `/api/auth/logout`

Revoke current authentication token and clear cookie.

**Headers**: Requires `AUTH_TOKEN` cookie

**Response** (204 No Content)

**Example**:
```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt \
  -c cookies.txt
```

### User Management Endpoints

All user endpoints require authentication (valid `AUTH_TOKEN` cookie).

#### Get Current User

**GET** `/api/users/me`

Retrieve authenticated user's profile.

**Headers**: Requires `AUTH_TOKEN` cookie

**Response** (200 OK):
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "john@example.com",
  "avatarUrl": "https://example.com/avatar.jpg",
  "timezone": "America/New_York",
  "createdAt": "2026-01-13T10:30:00",
  "lastModifiedAt": "2026-01-13T10:30:00"
}
```

**Example**:
```bash
curl -X GET http://localhost:8080/api/users/me \
  -b cookies.txt
```

#### Update Current User

**PATCH** `/api/users/me`

Update authenticated user's profile (email, avatarUrl, timezone).

**Headers**:
- Requires `AUTH_TOKEN` cookie
- Requires `X-XSRF-TOKEN` header (from XSRF-TOKEN cookie)

**Request Body** (all fields optional):
```json
{
  "email": "newemail@example.com",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "timezone": "America/Los_Angeles"
}
```

**Response** (200 OK):
```json
{
  "id": 1,
  "username": "johndoe",
  "email": "newemail@example.com",
  "avatarUrl": "https://example.com/new-avatar.jpg",
  "timezone": "America/Los_Angeles",
  "createdAt": "2026-01-13T10:30:00",
  "lastModifiedAt": "2026-01-13T14:20:00"
}
```

**Error Responses**:
- `409 Conflict`: Email already taken by another user
- `400 Bad Request`: Invalid email format

**Example**:
```bash
# Extract CSRF token from cookies
CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

curl -X PATCH http://localhost:8080/api/users/me \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -b cookies.txt \
  -d '{
    "email": "newemail@example.com",
    "timezone": "America/Los_Angeles"
  }'
```

#### Change Password

**POST** `/api/users/me/change-password`

Change authenticated user's password. **Invalidates ALL user tokens** (logs out all devices).

**Headers**:
- Requires `AUTH_TOKEN` cookie
- Requires `X-XSRF-TOKEN` header

**Request Body**:
```json
{
  "currentPassword": "SecurePass123!",
  "newPassword": "NewSecurePass456!"
}
```

**Response** (204 No Content)

**Behavior**:
- Verifies current password
- Validates new password strength
- Updates password hash
- **Revokes ALL user's active tokens** (all devices must re-login)
- Clears current `AUTH_TOKEN` cookie

**Example**:
```bash
CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

curl -X POST http://localhost:8080/api/users/me/change-password \
  -H "Content-Type: application/json" \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -b cookies.txt \
  -c cookies.txt \
  -d '{
    "currentPassword": "SecurePass123!",
    "newPassword": "NewSecurePass456!"
  }'

# Must login again with new password
```

#### Delete Current User

**DELETE** `/api/users/me`

Soft-delete authenticated user's account and invalidate all tokens.

**Headers**:
- Requires `AUTH_TOKEN` cookie
- Requires `X-XSRF-TOKEN` header

**Response** (204 No Content)

**Behavior**:
- Sets `deleted_at` timestamp (soft delete)
- Revokes all user's active tokens
- Clears `AUTH_TOKEN` cookie
- User cannot login after deletion

**Example**:
```bash
CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')

curl -X DELETE http://localhost:8080/api/users/me \
  -H "X-XSRF-TOKEN: $CSRF_TOKEN" \
  -b cookies.txt \
  -c cookies.txt
```

## Testing

### Run All Tests

```bash
./mvnw test
```

### Run Specific Test Types

```bash
# Unit tests only
./mvnw test -Dtest="*Test"

# Integration tests only
./mvnw test -Dtest="*IntegrationTest"

# Specific test class
./mvnw test -Dtest=AuthenticationServiceTest
```

### Test Coverage

- **Unit Tests**: 73 tests covering service layer logic
- **Integration Tests**: 26 tests covering API endpoints
- **Total**: 99 tests with comprehensive coverage of authentication and user management

## Production Deployment

### Production Profile

Create `application-prod.yaml` or set environment variables:

```yaml
spring:
  profiles:
    active: prod

jwt:
  secret: ${JWT_SECRET}
  cookie:
    secure: true  # HTTPS-only cookies
```

### Run with Production Profile

```bash
# Using Maven
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Using JAR
java -jar target/core-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Security Checklist

- [ ] Generate secure JWT secret (256-bit minimum)
- [ ] Set `JWT_COOKIE_SECURE=true` for HTTPS-only cookies
- [ ] Use strong database password
- [ ] Enable HTTPS/TLS for production
- [ ] Configure CORS for your frontend domain
- [ ] Set up database backups
- [ ] Configure rate limiting (future enhancement)
- [ ] Monitor failed login attempts

## Architecture

### Package Structure

```
com.aboff.core/
├── config/           # Configuration classes (SecurityConfig, JwtConfig)
├── controller/       # REST API endpoints
├── service/          # Business logic layer
├── repository/       # Data access (Spring Data JPA)
├── model/
│   ├── entity/      # JPA entities (User, LoginAttempt, ActiveToken)
│   ├── dto/         # Data transfer objects (request/response)
│   └── enums/       # Enumerations
├── security/        # Security infrastructure (JWT, filters, providers)
└── exception/       # Custom exceptions and global handler
```

### Database Schema

- **users**: User accounts with password hashing and locking
- **login_attempts**: Audit log of login attempts (success/failure)
- **active_tokens**: Active JWT token hashes for validation and invalidation

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
# Ensure database is running for integration tests
./scripts/start-db.sh

# Clean and run tests
./mvnw clean test
```

**401 Unauthorized on Protected Endpoints**
- Ensure you're including the `AUTH_TOKEN` cookie
- Token may have expired (30-day expiration)
- Token may have been revoked (password change invalidates all tokens)
- Check if account was deleted

**403 Forbidden on POST/PATCH/DELETE**
- Include `X-XSRF-TOKEN` header with value from `XSRF-TOKEN` cookie
- CSRF protection is enabled for all state-changing requests
