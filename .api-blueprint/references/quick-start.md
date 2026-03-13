# API Quick-Start Reference

**Base URL:** `http://localhost:8080`

---

## 1. Authentication Flow

Authentication uses **HttpOnly cookies** with JWT tokens. The token is never exposed in response bodies -- it is set and read automatically via the `AUTH_TOKEN` cookie.

### Register a new user

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "email": "test@example.com",
    "password": "Password123!"
  }'
```

**Response (201 Created):**

```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com"
}
```

Registration does **not** set an auth cookie. You must log in separately.

### Log in

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -c cookies.txt \
  -d '{
    "usernameOrEmail": "testuser",
    "password": "Password123!"
  }'
```

The `usernameOrEmail` field accepts either a username or an email address.

**Response (200 OK):**

```json
{
  "id": 1,
  "username": "testuser",
  "email": "test@example.com"
}
```

The response also sets a `Set-Cookie` header:

```
Set-Cookie: AUTH_TOKEN=<jwt>; Path=/; Max-Age=2592000; HttpOnly; SameSite=Strict
```

### Make authenticated requests

Include the cookie in subsequent requests:

```bash
curl http://localhost:8080/api/dh/character-sheets/1 \
  -b cookies.txt
```

Or pass the cookie header directly:

```bash
curl http://localhost:8080/api/dh/character-sheets/1 \
  -H "Cookie: AUTH_TOKEN=<jwt-value>"
```

### Log out

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt
```

**Response:** `204 No Content`. The server revokes the token in the database and sends back a `Set-Cookie` header with `Max-Age=0` to clear the cookie.

---

## 2. Common Request Headers

| Header         | Value                | When                    |
|----------------|----------------------|-------------------------|
| `Content-Type` | `application/json`   | All POST/PUT/PATCH requests |
| `Cookie`       | `AUTH_TOKEN=<jwt>`   | All authenticated requests  |
| `Accept`       | `application/json`   | Optional (default)          |

No `Authorization: Bearer` header is used. Authentication is entirely cookie-based.

---

## 3. Error Response Format

### Standard error

All non-validation errors return an `ErrorResponse`:

```json
{
  "status": 401,
  "error": "Invalid Credentials",
  "message": "Invalid username or password",
  "path": "/api/auth/login",
  "timestamp": "2026-03-13T10:30:00"
}
```

### Validation error

Bean validation failures (`@NotBlank`, `@Size`, etc.) return a `ValidationErrorResponse` with per-field errors:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "fieldErrors": {
    "username": "Username is required",
    "email": "Invalid email format"
  },
  "path": "/api/auth/register",
  "timestamp": "2026-03-13T10:30:00"
}
```

### Error catalog

| HTTP Status | `error` field              | Trigger                                  |
|-------------|---------------------------|------------------------------------------|
| 400         | `"Validation Failed"`     | Bean validation failure (missing/invalid fields) |
| 400         | `"Invalid Password"`      | Password does not meet complexity rules  |
| 400         | `"Invalid Operation"`     | Illegal state (e.g., invalid entity transition) |
| 401         | `"Invalid Credentials"`   | Wrong username/password (generic to prevent enumeration) |
| 403         | `"Account Locked"`        | Too many failed login attempts           |
| 403         | `"Insufficient Permissions"` | Role-based access denied              |
| 403         | `"Access Denied"`         | Spring Security access denied            |
| 404         | `"User Not Found"`        | User lookup by ID failed                 |
| 404         | `"Entity Not Found"`      | JPA entity lookup failed                 |
| 409         | `"User Already Exists"`   | Duplicate username or email on register  |
| 500         | `"Internal Server Error"` | Unhandled exception (details hidden)     |

---

## 4. Password Requirements

Passwords are validated on registration with these rules (from `application.yaml`):

| Rule                        | Value    |
|-----------------------------|----------|
| Minimum length              | 8 chars  |
| Maximum length              | 100 chars |
| Requires uppercase letter   | Yes      |
| Requires lowercase letter   | Yes      |
| Requires digit              | Yes      |
| Requires special character  | Yes      |

Valid special characters: `!@#$%^&*()_+-=[]{};':"\\|,.<>/?`

**Example valid password:** `Password123!`

**Example invalid passwords and their errors:**
- `"weak"` -- "Password must be at least 8 characters long"
- `"password123!"` -- "Password must contain at least one uppercase letter"
- `"PASSWORD123!"` -- "Password must contain at least one lowercase letter"
- `"Password!!!"` -- "Password must contain at least one digit"
- `"Password123"` -- "Password must contain at least one special character"

Username constraints: 3-100 characters, alphanumeric plus underscores and hyphens (`^[a-zA-Z0-9_-]+$`).

---

## 5. Pagination

All list endpoints return a paginated `PagedResponse` wrapper:

```bash
curl "http://localhost:8080/api/dh/weapons?page=0&size=20" -b cookies.txt
```

**Response:**

```json
{
  "content": [ ... ],
  "totalElements": 57,
  "totalPages": 3,
  "currentPage": 0,
  "pageSize": 20
}
```

| Parameter | Default | Description                          |
|-----------|---------|--------------------------------------|
| `page`    | `0`     | Zero-based page number               |
| `size`    | `20`    | Items per page (max: 100)            |

---

## 6. Response Expansion

Most GET endpoints accept an `expand` query parameter to include related entities inline, reducing round-trips.

```bash
# Without expand -- related objects returned as IDs or omitted
curl "http://localhost:8080/api/dh/character-sheets/1" -b cookies.txt

# With expand -- related objects included as nested JSON
curl "http://localhost:8080/api/dh/character-sheets/1?expand=owner,experiences,inventoryWeapons" \
  -b cookies.txt
```

- Pass a **comma-separated** list of relationship names: `?expand=field1,field2`
- Available expand fields are specific to each entity type (e.g., `expansion`, `features`, `owner`, `originalWeapon`)
- Works on both single-resource (`GET /{id}`) and list (`GET /`) endpoints
- Invalid expand values are silently ignored

---

## 7. Soft Deletion

Most entities (cards, items, characters, campaigns, adversaries, etc.) support soft deletion. Deleted records are **not removed** from the database; instead, a `deletedAt` timestamp is set.

**Behavior:**
- **Default queries** exclude soft-deleted records (`deletedAt IS NULL`)
- **Admin access:** Some list endpoints accept `?includeDeleted=true` to show deleted records (typically requires ADMIN/OWNER role)
- **DELETE endpoints** perform soft deletion (set `deletedAt` to current time), not physical removal
- Soft-deleted users cannot log in (treated as invalid credentials)

Each entity provides:
- `isDeleted()` -- returns `true` if `deletedAt` is not null
- `softDelete()` -- sets `deletedAt` to the current timestamp
- `restore()` -- clears `deletedAt`, making the record active again

---

## 8. Content Visibility Model

Game content (cards, items) uses an **official/custom** ownership model:

| Field         | Type      | Description                                                  |
|---------------|-----------|--------------------------------------------------------------|
| `isOfficial`  | `boolean` | `true` for official Daggerheart game content. Modification restricted to OWNER role. |
| `createdBy`   | `User`    | The user who created a custom item. `null` for official content. |

- **Official content** (`isOfficial=true`): Managed by system administrators. Visible to all authenticated users.
- **Custom content** (`isOfficial=false`): Created by individual users. Tracked via `createdBy` field.
- **Filtering:** List endpoints support `?isOfficial=true` or `?isOfficial=false` to filter by content type.

Items are also associated with an `expansion` entity that groups content by game expansion/sourcebook.

---

## 9. Rate Limiting / Login Lockout

The API uses a **login attempt lockout** mechanism (not general rate limiting):

| Setting                     | Value       |
|-----------------------------|-------------|
| Max failed attempts         | **5**       |
| Lockout duration            | **30 minutes** |
| Failed attempt window       | **15 minutes** |

**How it works:**
1. Each login attempt (success or failure) is recorded in the `login_attempts` table with a timestamp and reason.
2. After **5 failed attempts** within a **15-minute window**, the account is locked for **30 minutes**.
3. During lockout, login requests return `403 Forbidden`:

```json
{
  "status": 403,
  "error": "Account Locked",
  "message": "Account is temporarily locked due to multiple failed login attempts. Account locked until 2026-03-13T11:00:00",
  "path": "/api/auth/login",
  "timestamp": "2026-03-13T10:30:00"
}
```

4. The lockout expires automatically. There is no unlock endpoint.
5. Successful logins reset the failed attempt counter.

---

## 10. CORS Configuration

Allowed origins (configurable via `CORS_ALLOWED_ORIGINS` env var, defaults for development):
- `http://localhost:3000`
- `http://localhost:5173`
- `http://localhost:4200`
- `http://localhost:8081`

Allowed methods: `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`

Credentials (cookies) are allowed. Preflight responses are cached for 1 hour.

---

## Public vs. Authenticated Endpoints

| Endpoint                  | Access         |
|---------------------------|----------------|
| `POST /api/auth/register` | Public         |
| `POST /api/auth/login`    | Public         |
| `GET /actuator/health`    | Public         |
| All other `/api/**`       | Authenticated (valid `AUTH_TOKEN` cookie required) |

Role-based restrictions (MODERATOR, ADMIN, OWNER) are enforced at the endpoint level via `@PreAuthorize`.

---

## JWT Token Details

| Property     | Value                  |
|--------------|------------------------|
| Algorithm    | HMAC-SHA (symmetric)   |
| Expiration   | 30 days (2,592,000,000 ms) |
| Cookie name  | `AUTH_TOKEN`           |
| Cookie path  | `/`                    |
| HttpOnly     | `true`                 |
| Secure       | `false` (dev) / `true` (prod, via `JWT_COOKIE_SECURE` env var) |
| SameSite     | `Strict`               |
| Cookie max-age | 30 days (2,592,000 s) |

The JWT subject contains the user ID. Claims include `username` and standard `iat`/`exp` fields. Tokens are tracked server-side in the `active_tokens` table (stored as SHA-256 hashes) and can be revoked on logout.
