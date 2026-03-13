# Auth API

Base path: `/api/auth`
Authentication: Mixed (register and login are public; logout requires authentication)

## Endpoints

### POST `/api/auth/register`

Register a new user account.

**Authentication:** Public
**Status:** `201 Created`

#### Request Body: `RegisterRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `username` | `String` | Yes | 3-100 chars; alphanumeric, underscores, hyphens only (`^[a-zA-Z0-9_-]+$`); unique (case-insensitive) | The desired username |
| `email` | `String` | Yes | Max 255 chars; valid email format; unique (case-insensitive) | The user's email address |
| `password` | `String` | Yes | 8-100 chars; must contain uppercase, lowercase, digit, and special character | The user's password |
| `timezone` | `String` | No | Max 50 chars | User's timezone (defaults to `"UTC"`) |
| `avatarUrl` | `String` | No | Max 500 chars | URL to avatar image (defaults to `"https://api.dicebear.com/7.x/avatars/svg?seed=default"`) |

```json
{
  "username": "testuser",
  "email": "test@example.com",
  "password": "Password123!",
  "timezone": "America/New_York",
  "avatarUrl": "https://custom.avatar/image.png"
}
```

#### Response: `UserResponse` — `201 Created`

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique user identifier |
| `username` | `String` | No | The user's username |
| `email` | `String` | No | The user's email address |
| `avatarUrl` | `String` | Yes | URL to the user's avatar image |
| `timezone` | `String` | Yes | The user's timezone |
| `createdAt` | `LocalDateTime` | No | Account creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |

Note: `UserResponse` uses `@JsonInclude(NON_NULL)`, so null fields are omitted from the response. The fields `accountLockedUntil`, `failedLoginAttempts`, `deletedAt`, and `bannedAt` are only included for privileged users viewing other accounts and will not appear in the register response.

```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "avatarUrl": "https://api.dicebear.com/7.x/avatars/svg?seed=default",
  "timezone": "UTC",
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Error Responses

| Status | Condition | Example Body |
|---|---|---|
| `400 Bad Request` | Validation failed (missing/invalid fields) | `{"status": 400, "error": "Validation Failed", "fieldErrors": {"username": "Username is required", "password": "Password must be between 8 and 100 characters"}, "path": "/api/auth/register", "timestamp": "2026-03-13T12:00:00"}` |
| `400 Bad Request` | Password does not meet complexity requirements | `{"status": 400, "error": "Invalid Password", "message": "Password must contain at least one uppercase letter", "path": "/api/auth/register", "timestamp": "2026-03-13T12:00:00"}` |
| `409 Conflict` | Username already taken (case-insensitive) | `{"status": 409, "error": "User Already Exists", "message": "Username already taken", "path": "/api/auth/register", "timestamp": "2026-03-13T12:00:00"}` |
| `409 Conflict` | Email already registered (case-insensitive) | `{"status": 409, "error": "User Already Exists", "message": "Email already registered", "path": "/api/auth/register", "timestamp": "2026-03-13T12:00:00"}` |

#### Example

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "email": "test@example.com", "password": "Password123!"}'
```

---

### POST `/api/auth/login`

Authenticate a user and receive a JWT token via an HttpOnly cookie.

**Authentication:** Public
**Status:** `200 OK`

#### Request Body: `LoginRequest`

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `usernameOrEmail` | `String` | Yes | Must not be blank | Username or email address (case-insensitive lookup) |
| `password` | `String` | Yes | Must not be blank | The user's password (case-sensitive) |

```json
{
  "usernameOrEmail": "testuser",
  "password": "Password123!"
}
```

Or with email:

```json
{
  "usernameOrEmail": "test@example.com",
  "password": "Password123!"
}
```

#### Response: `UserResponse` — `200 OK`

The response body contains user information. The JWT token is **not** in the response body; it is set as an HttpOnly cookie named `AUTH_TOKEN`.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Unique user identifier |
| `username` | `String` | No | The user's username |
| `email` | `String` | No | The user's email address |
| `avatarUrl` | `String` | Yes | URL to the user's avatar image |
| `timezone` | `String` | Yes | The user's timezone |
| `createdAt` | `LocalDateTime` | No | Account creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last modification timestamp |

Response headers include:

```
Set-Cookie: AUTH_TOKEN=<jwt-token>; Path=/; HttpOnly; SameSite=Strict
```

```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com",
  "avatarUrl": "https://avatar.url",
  "timezone": "UTC",
  "createdAt": "2026-03-13T12:00:00",
  "lastModifiedAt": "2026-03-13T12:00:00"
}
```

#### Error Responses

| Status | Condition | Example Body |
|---|---|---|
| `400 Bad Request` | Validation failed (blank fields) | `{"status": 400, "error": "Validation Failed", "fieldErrors": {"usernameOrEmail": "Username or email is required"}, "path": "/api/auth/login", "timestamp": "2026-03-13T12:00:00"}` |
| `401 Unauthorized` | Invalid credentials (wrong password, user not found, soft-deleted user, banned user) | `{"status": 401, "error": "Invalid Credentials", "message": "Invalid username or password", "path": "/api/auth/login", "timestamp": "2026-03-13T12:00:00"}` |
| `403 Forbidden` | Account locked due to too many failed attempts | `{"status": 403, "error": "Account Locked", "message": "Account is temporarily locked due to multiple failed login attempts. Account locked until 2026-03-13T12:30:00", "path": "/api/auth/login", "timestamp": "2026-03-13T12:00:00"}` |

#### Account Lockout Behavior

- After **5 failed login attempts** within a **15-minute window**, the account is locked for **30 minutes**.
- Each failed attempt increments the `failedLoginAttempts` counter on the user.
- A successful login resets the counter to 0.
- All login attempts (success and failure) are recorded in the `login_attempts` audit table.

#### Example

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{"usernameOrEmail": "testuser", "password": "Password123!"}'
```

---

### POST `/api/auth/logout`

Revoke the current JWT token and clear the authentication cookie.

**Authentication:** Authenticated (requires valid `AUTH_TOKEN` cookie)
**Status:** `204 No Content`

#### Request Body

None. The JWT token is read from the `AUTH_TOKEN` cookie.

#### Response — `204 No Content`

Empty body. The `AUTH_TOKEN` cookie is cleared (set to `maxAge=0`).

Response headers include:

```
Set-Cookie: AUTH_TOKEN=; Path=/; Max-Age=0; HttpOnly; SameSite=Strict
```

#### Error Responses

| Status | Condition | Example Body |
|---|---|---|
| `401 Unauthorized` | No valid `AUTH_TOKEN` cookie provided | `{"status": 401, "error": "Unauthorized", "message": "Full authentication is required to access this resource", "path": "/api/auth/logout", "timestamp": "2026-03-13T12:00:00"}` |

#### Example

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt \
  -c cookies.txt
```

---

## Models

### `RegisterRequest`

Request DTO for user registration.

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `username` | `String` | Yes | `@NotBlank`; `@Size(min=3, max=100)`; `@Pattern(^[a-zA-Z0-9_-]+$)` | Alphanumeric username with underscores/hyphens |
| `email` | `String` | Yes | `@NotBlank`; `@Email`; `@Size(max=255)` | Valid email address |
| `password` | `String` | Yes | `@NotBlank`; `@Size(min=8, max=100)`; validated by `PasswordValidator` (requires uppercase, lowercase, digit, special char) | User password |
| `timezone` | `String` | No | `@Size(max=50)` | IANA timezone string (e.g., `"America/New_York"`) |
| `avatarUrl` | `String` | No | `@Size(max=500)` | URL to avatar image |

### `LoginRequest`

Request DTO for user login.

| Field | Type | Required | Constraints | Description |
|---|---|---|---|---|
| `usernameOrEmail` | `String` | Yes | `@NotBlank` | Username or email address (case-insensitive) |
| `password` | `String` | Yes | `@NotBlank` | User password (case-sensitive) |

### `UserResponse`

Response DTO containing non-sensitive user profile data. Uses `@JsonInclude(NON_NULL)` so null fields are omitted.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Auto-generated unique identifier (BIGSERIAL) |
| `username` | `String` | No | The user's username |
| `email` | `String` | No | The user's email address |
| `avatarUrl` | `String` | Yes | URL to avatar image |
| `timezone` | `String` | Yes | User's timezone |
| `createdAt` | `LocalDateTime` | No | Account creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last update timestamp |
| `accountLockedUntil` | `LocalDateTime` | Yes | Lock expiration (privileged view only) |
| `failedLoginAttempts` | `Integer` | Yes | Failed attempt count (privileged view only) |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (privileged view only) |
| `bannedAt` | `LocalDateTime` | Yes | Ban timestamp (privileged view only) |

### `ErrorResponse`

Standard error response body.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `status` | `int` | No | HTTP status code |
| `error` | `String` | No | Error category (e.g., `"Invalid Credentials"`) |
| `message` | `String` | No | Human-readable error message |
| `path` | `String` | No | Request URI path |
| `timestamp` | `LocalDateTime` | No | When the error occurred |

### `ValidationErrorResponse`

Validation error response body (returned for `@Valid` failures).

| Field | Type | Nullable | Description |
|---|---|---|---|
| `status` | `int` | No | HTTP status code (`400`) |
| `error` | `String` | No | Always `"Validation Failed"` |
| `fieldErrors` | `Map<String, String>` | No | Map of field name to error message |
| `path` | `String` | No | Request URI path |
| `timestamp` | `LocalDateTime` | No | When the error occurred |

### `Role` (Enum)

User role hierarchy (highest to lowest privilege):

| Value | Description |
|---|---|
| `OWNER` | System owner, highest privilege |
| `ADMIN` | Administrator |
| `MODERATOR` | Moderator, can bypass ownership checks |
| `USER` | Standard user (default for new registrations) |

## Database Schema: `users` Table

Final schema after all migrations:

| Column | Type | Nullable | Default | Constraints |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | No | Auto-increment | Primary key |
| `username` | `VARCHAR(100)` | No | -- | Unique; indexed |
| `email` | `VARCHAR(255)` | No | -- | Unique; indexed |
| `avatar_url` | `VARCHAR(500)` | Yes | `NULL` | -- |
| `timezone` | `VARCHAR(50)` | Yes | `NULL` | -- |
| `password_hash` | `VARCHAR(60)` | Yes | `NULL` | BCrypt hash |
| `account_locked_until` | `TIMESTAMP` | Yes | `NULL` | Indexed |
| `failed_login_attempts` | `INTEGER` | Yes | `0` | -- |
| `last_failed_login` | `TIMESTAMP` | Yes | `NULL` | -- |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | -- |
| `last_modified_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | -- |
| `deleted_at` | `TIMESTAMP` | Yes | `NULL` | Indexed; partial index on `NULL` values |
| `banned_at` | `TIMESTAMP` | Yes | `NULL` | Indexed |
| `role` | `VARCHAR(20)` | No | `'USER'` | Indexed |
