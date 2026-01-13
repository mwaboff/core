# Authentication Implementation Summary

## Overview

This document summarizes the JWT-based authentication system implemented for the Core Backend Project. The implementation provides secure, cookie-based authentication with comprehensive security features including account locking, multi-device support, and token invalidation.

## Implementation Status

✅ **COMPLETE** - All phases implemented and tested (98/98 tests passing)

### Completed Phases

1. ✅ **Phase 1**: Database & Entities
2. ✅ **Phase 2**: Security Infrastructure
3. ✅ **Phase 3**: DTOs & Exceptions
4. ✅ **Phase 4**: Service Layer
5. ✅ **Phase 5**: Controllers
6. ✅ **Phase 6**: Testing & Verification
7. ✅ **Phase 7**: Documentation

## Architecture Decisions

### ID Type: BIGSERIAL (Long)
- **Decision**: Use BIGSERIAL auto-incrementing IDs instead of UUIDs
- **Rationale**:
  - Faster joins and lookups (8 bytes vs 16 bytes)
  - Better database performance and smaller indexes
  - Human-readable for debugging (User #1, #2, #3)
  - PostgreSQL handles generation automatically
- **Trade-off**: Sequential IDs reveal user count (acceptable for most applications)

### Token Storage: httpOnly Cookies
- **Decision**: Store JWT tokens in httpOnly cookies instead of localStorage
- **Rationale**:
  - XSS Protection: JavaScript cannot access httpOnly cookies
  - Frontend Simplicity: Browser automatically includes cookie in requests
  - Security: SameSite=Strict prevents CSRF attacks
  - HTTPS-only in production with Secure flag
- **Trade-off**: Not suitable for mobile apps (web-only solution)

### Authentication Method: JWT Tokens
- **Decision**: Stateless JWT authentication with database-backed invalidation
- **Rationale**:
  - Scalable and performant (no session storage)
  - Can be validated without database lookup for performance
  - Database tracking enables multi-device support and invalidation
- **Implementation**:
  - 30-day expiration
  - SHA-256 hash stored in database (not plain JWT)
  - Custom `active_tokens` table for tracking and invalidation

### Password Storage: In Users Table
- **Decision**: Store password hash directly in `users` table
- **Rationale**: Follows Spring Security best practices and standard patterns
- **Security**: BCrypt hashing with strength 12 (~300ms per hash)

### CSRF Protection: Enabled
- **Decision**: Enable Spring Security CSRF protection
- **Rationale**: Defense-in-depth security approach
- **Implementation**: Automatic CSRF token generation and validation

## Database Schema

### Users Table
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(60),  -- BCrypt hash, nullable for OAuth-only users
    avatar_url VARCHAR(500),
    timezone VARCHAR(50),
    account_locked_until TIMESTAMP,
    failed_login_attempts INTEGER DEFAULT 0,
    last_failed_login TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP  -- Soft delete
);
```

### Active Tokens Table
```sql
CREATE TABLE active_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL UNIQUE,  -- SHA-256 hash
    device_info VARCHAR(500),
    ip_address VARCHAR(45),  -- IPv6 compatible
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

### Login Attempts Table
```sql
CREATE TABLE login_attempts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    username_attempted VARCHAR(100) NOT NULL,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(100),
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),
    attempted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Security Features

### Password Requirements
- Minimum 8 characters
- At least one uppercase letter (A-Z)
- At least one lowercase letter (a-z)
- At least one digit (0-9)
- At least one special character (!@#$%^&*()_+-=[]{}|;:,.<>?)

### Account Locking
- **Trigger**: 5 failed login attempts within 15 minutes
- **Duration**: 30-minute lockout
- **Reset**: Successful login resets failed attempt counter
- **Tracking**: All attempts logged in `login_attempts` table

### Token Management
- **Generation**: JWT with 30-day expiration on login
- **Storage**: SHA-256 hash stored in `active_tokens` table
- **Validation**: Database check on each authenticated request
- **Multi-device**: Each login creates separate token entry
- **Invalidation**: Password change revokes ALL user tokens

### Cookie Security
- **httpOnly**: JavaScript cannot access token (XSS protection)
- **Secure**: HTTPS-only in production
- **SameSite=Strict**: Prevents CSRF attacks
- **Max-Age**: 30 days (matches JWT expiration)

### CSRF Protection
- Spring Security automatic CSRF token generation
- Required for all POST/PATCH/DELETE requests
- Token passed via `X-XSRF-TOKEN` header

## API Endpoints

### Authentication
- `POST /api/auth/register` - Create new user account
- `POST /api/auth/login` - Authenticate and receive token cookie
- `POST /api/auth/logout` - Revoke token and clear cookie

### User Management
- `GET /api/users/me` - Get current user profile
- `PATCH /api/users/me` - Update profile (email, avatar, timezone)
- `POST /api/users/me/change-password` - Change password (invalidates all tokens)
- `DELETE /api/users/me` - Soft-delete account (invalidates all tokens)

See [README.md](README.md) for complete API documentation with request/response examples.

## Code Structure

### Packages Created/Modified

**New Packages** (30 files):
- `security/` - JWT provider, authentication filter, UserDetails implementation
- `model/entity/` - LoginAttempt, ActiveToken entities
- `model/dto/request/` - RegisterRequest, LoginRequest, UpdateUserRequest, ChangePasswordRequest
- `model/dto/response/` - UserResponse, ErrorResponse
- `repository/` - LoginAttemptRepository, ActiveTokenRepository
- `service/` - AuthenticationService, UserService, LoginAttemptService, TokenCleanupService
- `controller/` - AuthController, UserController
- `exception/` - Custom exceptions and GlobalExceptionHandler
- `util/` - PasswordValidator, CookieUtil
- `config/` - SecurityConfig

**Modified Files**:
- `model/entity/User.java` - Changed ID from UUID to Long, added password fields
- `repository/UserRepository.java` - Updated method signatures for Long IDs
- `application.yaml` - Added JWT and security configuration

**Database Migrations** (4 files):
- V{timestamp}__change_user_id_to_bigserial.sql
- V{timestamp}__add_password_authentication_fields.sql
- V{timestamp}__create_login_attempts_table.sql
- V{timestamp}__create_active_tokens_table.sql

## Testing

### Test Coverage: 98/98 Tests Passing (100%)

**Unit Tests** (73 tests):
- AuthenticationServiceTest (14 tests)
- UserServiceTest (13 tests)
- JwtTokenProviderTest (15 tests)
- PasswordValidatorTest (21 tests)
- LoginAttemptServiceTest (7 tests)
- TokenCleanupServiceTest (3 tests)

**Integration Tests** (25 tests):
- AuthControllerIntegrationTest (13 tests)
- UserControllerIntegrationTest (12 tests)

### Test Configuration
- Test database: Same PostgreSQL database (heartandfear)
- Transaction management: @Transactional rollback after each test
- Security context: MockMvc with Spring Security integration
- CSRF: Injected using `.with(csrf())` for state-changing requests

### Running Tests
```bash
# All tests
./mvnw test

# Unit tests only
./mvnw test -Dtest="*Test"

# Integration tests only
./mvnw test -Dtest="*IntegrationTest"

# Specific test class
./mvnw test -Dtest=AuthenticationServiceTest
```

## Configuration

### Required Environment Variables

Create `.env` file in project root:
```bash
# Database
POSTGRES_DB=heartandfear
POSTGRES_USER=postgres
POSTGRES_PASSWORD=password

# JWT Secret (generate with: openssl rand -base64 32)
JWT_SECRET=your-secure-256-bit-secret-here
```

### Application Configuration

**JWT Settings** (`application.yaml`):
```yaml
jwt:
  secret: ${JWT_SECRET}
  expiration: 2592000000  # 30 days in milliseconds
  cookie:
    name: AUTH_TOKEN
    http-only: true
    secure: false  # Set to true in production
    same-site: Strict
    max-age: 2592000  # 30 days in seconds
```

**Security Settings**:
```yaml
application:
  security:
    max-failed-attempts: 5
    lockout-duration-minutes: 30
    failed-attempt-window-minutes: 15
  password:
    min-length: 8
    require-uppercase: true
    require-lowercase: true
    require-digit: true
    require-special: true
  user:
    default-avatar-url: https://api.dicebear.com/7.x/avatars/svg?seed=default
    default-timezone: UTC
```

### Production Configuration

Create `application-prod.yaml`:
```yaml
jwt:
  cookie:
    secure: true  # HTTPS-only cookies

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${POSTGRES_DB}
    username: ${POSTGRES_USER}
    password: ${POSTGRES_PASSWORD}
```

Run with: `./mvnw spring-boot:run -Dspring-boot.run.profiles=prod`

## Scheduled Tasks

### Token Cleanup (Daily)
- **Service**: TokenCleanupService
- **Schedule**: Daily at 3:00 AM
- **Action**: Delete expired tokens and revoked tokens older than 30 days
- **Purpose**: Prevent database bloat from old tokens

### Login Attempt Cleanup (Daily)
- **Service**: LoginAttemptService
- **Schedule**: Daily at 2:00 AM
- **Action**: Delete login attempts older than 90 days
- **Purpose**: Maintain audit trail while managing database size

## Known Limitations & Future Enhancements

### Current Limitations
1. **Email Verification**: Not implemented - users can register without email verification
2. **Forgot Password**: Not implemented - no password reset flow
3. **Rate Limiting**: Not implemented - relies on account locking only
4. **Mobile Apps**: Cookie-based auth not suitable for native mobile apps
5. **Refresh Tokens**: Not implemented - fixed 30-day expiration

### Potential Enhancements
1. **Email System**:
   - Email verification on registration
   - Forgot password flow with reset tokens
   - Email notifications for security events

2. **Advanced Security**:
   - Rate limiting per IP address
   - Device fingerprinting for suspicious activity detection
   - Two-factor authentication (TOTP)
   - Password history to prevent reuse

3. **OAuth Enhancement**:
   - Link multiple OAuth providers to one account
   - OAuth-only users (no password)

4. **Token Management**:
   - Refresh token rotation
   - Configurable token expiration
   - User-initiated token revocation (manage devices)

5. **Audit & Monitoring**:
   - Admin dashboard for login attempts
   - Geolocation tracking from IP addresses
   - Suspicious activity alerts
   - Export audit logs

6. **Mobile Support**:
   - API key authentication for mobile apps
   - OAuth2 authorization code flow

## Security Checklist

### Development
- ✅ Passwords never logged or exposed in responses
- ✅ BCrypt password hashing (strength 12)
- ✅ Account locking after failed attempts
- ✅ Soft delete preserves audit trail
- ✅ CSRF protection enabled
- ✅ XSS protection via httpOnly cookies
- ✅ SQL injection prevention via JPA/Hibernate

### Production Deployment
- [ ] Generate secure JWT secret (256-bit minimum)
- [ ] Set JWT_COOKIE_SECURE=true for HTTPS-only cookies
- [ ] Use strong database password
- [ ] Enable HTTPS/TLS for production domain
- [ ] Configure CORS for frontend domain
- [ ] Set up database backups
- [ ] Monitor failed login attempts
- [ ] Configure firewall rules
- [ ] Regular security updates for dependencies

## Troubleshooting

### Tests Failing with Duplicate Key Errors
**Problem**: Integration tests fail with "duplicate key violates unique constraint"

**Solution**: Clean the database before running tests
```bash
docker compose exec postgres psql -U postgres -d heartandfear \
  -c "TRUNCATE TABLE active_tokens, login_attempts, users CASCADE;"
./mvnw test
```

### 401 Unauthorized on Protected Endpoints
**Causes**:
- Missing AUTH_TOKEN cookie
- Token expired (30-day expiration)
- Token revoked (password change)
- Account deleted

**Solution**: Login again to get new token

### 403 Forbidden on POST/PATCH/DELETE
**Cause**: Missing CSRF token

**Solution**: Include X-XSRF-TOKEN header with value from XSRF-TOKEN cookie
```bash
CSRF_TOKEN=$(grep XSRF-TOKEN cookies.txt | awk '{print $7}')
curl -H "X-XSRF-TOKEN: $CSRF_TOKEN" ...
```

### Account Locked
**Cause**: 5 failed login attempts within 15 minutes

**Solution**: Wait 30 minutes for automatic unlock or manually unlock via database:
```sql
UPDATE users SET account_locked_until = NULL,
                 failed_login_attempts = 0
WHERE username = 'username';
```

## References

- [Spring Security Documentation](https://docs.spring.io/spring-security/reference/)
- [JWT Introduction](https://jwt.io/introduction)
- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [BCrypt Password Hashing](https://en.wikipedia.org/wiki/Bcrypt)

## Completion Date

**January 13, 2026** - All phases completed and tested

---

For API usage examples and endpoint documentation, see [README.md](README.md).

For project instructions and development commands, see [CLAUDE.md](CLAUDE.md).
