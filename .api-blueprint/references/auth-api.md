# Auth API

Base path: `/api/auth`

Authentication is handled exclusively via OAuth2 providers. The login flow is initiated by navigating to `/oauth2/authorization/{provider}` (e.g., `/oauth2/authorization/google`). On success, the server sets an `AUTH_TOKEN` HttpOnly cookie and redirects to the frontend. The `logout` and `me` endpoints require a valid cookie; `dev-login` is available in the `dev` profile only.

---

## OAuth2 Login Flow

### GET `/oauth2/authorization/google`

Initiates the Google OAuth2 login flow. The browser is redirected to Google's consent screen. After the user grants permission, Google redirects back to `/login/oauth2/code/google`, which the Spring Security filter handles automatically.

**Authentication:** None (public)

**Flow:**
1. SPA navigates to `GET /oauth2/authorization/google`
2. Spring Security redirects to Google consent screen (requests `openid, email` scopes only)
3. Google redirects to `GET /login/oauth2/code/google` with authorization code
4. Spring Security exchanges code for tokens, calls `OAuth2UserProvisioningService` to find or create the user
5. `OAuth2LoginSuccessHandler` issues a JWT, sets `AUTH_TOKEN` HttpOnly cookie
6. If `usernameChosen` is `false` (first-time user), redirects to `${FRONTEND_BASE_URL}/choose-username`; otherwise redirects to `${FRONTEND_BASE_URL}`
7. On failure, `OAuth2LoginFailureHandler` redirects to `${FRONTEND_BASE_URL}/login?error`

**First-time user flow:** New OAuth users receive a temporary random username and are redirected to `/choose-username`. They call `POST /api/auth/choose-username` with their desired username. Their JWT cookie is already set, so no re-login is needed.

**No direct response body** — this endpoint triggers a redirect chain.

---

## Frontend Integration Guide

This section describes the complete authentication lifecycle from the SPA's perspective.

### Initial Page Load

On every page load (or route change), the SPA should call `GET /api/auth/me`:
- **200 with `usernameChosen: true`** — user is fully authenticated. Show the app.
- **200 with `usernameChosen: false`** — user is authenticated but hasn't picked a username. Redirect to the username selection page.
- **401** — user is not logged in. Show the login page with a "Sign in with Google" button.

### First-Time Login Flow

```
1. User clicks "Sign in with Google"
   → SPA navigates to: GET /oauth2/authorization/google

2. Google consent screen
   → User grants permission

3. Backend callback (automatic)
   → Creates User + UserIdentity
   → Sets AUTH_TOKEN HttpOnly cookie
   → Redirects to: ${FRONTEND_BASE_URL}/choose-username
     (because usernameChosen is false)

4. SPA renders username selection form
   → User types desired username

5. SPA calls: POST /api/auth/choose-username
   Body: {"username": "desired-name"}
   → 200: username set, usernameChosen now true
   → 400: validation error (too short, invalid chars)
   → 409: username already taken
   → SPA redirects to main app on success

No re-login is needed — the JWT cookie was set in step 3.
```

### Returning User Flow

```
1. User clicks "Sign in with Google"
   → SPA navigates to: GET /oauth2/authorization/google

2. Google consent screen (may auto-approve if previously consented)

3. Backend callback
   → Finds existing User via UserIdentity
   → Sets AUTH_TOKEN cookie
   → Redirects to: ${FRONTEND_BASE_URL}
     (because usernameChosen is true)

4. SPA loads normally
```

### Logout

```
1. SPA calls: POST /api/auth/logout
   → AUTH_TOKEN cookie cleared
   → SPA redirects to login page
```

### Dev Login (Local Development Only)

When running with `SPRING_PROFILES_ACTIVE=dev`, use the mock endpoint instead of Google:

```bash
curl -X POST http://localhost:8080/api/auth/dev-login \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com", "role": "USER", "username": "testuser"}'
```

Dev users always have `usernameChosen: true` so they skip the username selection flow.

### Key Points for Frontend

- The `AUTH_TOKEN` cookie is HttpOnly — JavaScript cannot read it. It's sent automatically by the browser on every request.
- Use `GET /api/auth/me` as the session check — not cookie inspection.
- The `usernameChosen` field in the response determines whether to show the username selection page.
- Username validation: 3–30 characters, only letters, numbers, underscores, hyphens.

---

## Endpoints

### GET `/api/auth/me`

Return the currently authenticated user's profile.

**Authentication:** Authenticated (requires valid `AUTH_TOKEN` cookie)
**Status:** `200 OK`

#### Response Body

```json
{
  "id": 1,
  "username": "johndoe",
  "role": "USER",
  "email": "john@example.com",
  "avatarUrl": "https://api.dicebear.com/7.x/avatars/svg?seed=default",
  "timezone": "UTC",
  "createdAt": "2026-04-12T10:00:00",
  "lastModifiedAt": "2026-04-12T10:00:00",
  "usernameChosen": true
}
```

#### Error Responses

| Status | Condition | Example Body |
|---|---|---|
| `401 Unauthorized` | No valid `AUTH_TOKEN` cookie | `{"status": 401, "error": "Unauthorized", ...}` |

#### Example

```bash
curl -s http://localhost:8080/api/auth/me \
  -b "AUTH_TOKEN=<jwt>"
```

---

### POST `/api/auth/choose-username`

Choose a username for the current user. Only permitted when `usernameChosen` is `false` (i.e., the user has not yet completed the username selection flow). The user is already authenticated via the JWT cookie set during OAuth — no re-login is needed.

**Authentication:** Authenticated (requires valid `AUTH_TOKEN` cookie)
**Status:** `200 OK`

#### Request Body

```json
{
  "username": "mycoolname"
}
```

| Field | Type | Required | Constraints |
|---|---|---|---|
| `username` | String | Yes | 3–30 characters; letters, numbers, underscores, and hyphens only |

#### Response Body

Returns the updated user profile with `usernameChosen: true`.

```json
{
  "id": 1,
  "username": "mycoolname",
  "role": "USER",
  "email": "john@example.com",
  "avatarUrl": "https://api.dicebear.com/7.x/avatars/svg?seed=default",
  "timezone": "UTC",
  "createdAt": "2026-04-12T10:00:00",
  "lastModifiedAt": "2026-04-12T10:00:00",
  "usernameChosen": true
}
```

#### Error Responses

| Status | Condition | Example Body |
|---|---|---|
| `400 Bad Request` | Validation failed (blank, too short/long, invalid chars) | `{"status": 400, ...}` |
| `401 Unauthorized` | No valid `AUTH_TOKEN` cookie | `{"status": 401, "error": "Unauthorized", ...}` |
| `409 Conflict` | Username already taken | `{"status": 409, "error": "Conflict", ...}` |
| `500 Internal Server Error` | Username already chosen | `{"status": 500, ...}` |

#### Example

```bash
curl -s -X POST http://localhost:8080/api/auth/choose-username \
  -H "Content-Type: application/json" \
  -d '{"username": "mycoolname"}' \
  -b "AUTH_TOKEN=<jwt>"
```

---

### POST `/api/auth/dev-login`

Create or retrieve a user by email and issue a JWT session. **Available in the `dev` Spring profile only.** Used for local development and integration tests. If the user does not exist, a new User and UserIdentity (provider=`dev`) are created automatically.

**Authentication:** None (public, dev profile only)
**Status:** `200 OK`

#### Request Body

```json
{
  "email": "alice@example.com",
  "role": "USER"
}
```

| Field | Type | Required | Description |
|---|---|---|---|
| `email` | String | Yes | Email address for the dev user (used as provider_sub) |
| `role` | String | No | Role to assign (`USER`, `MODERATOR`, `ADMIN`, `OWNER`). Defaults to `USER` |
| `username` | String | No | Explicit username. If omitted, generated from email. Dev users always have `usernameChosen: true` |

#### Response Body

Returns the user's profile and sets the `AUTH_TOKEN` cookie.

```json
{
  "id": 1,
  "username": "alice",
  "role": "USER",
  "email": "alice@example.com",
  "avatarUrl": "https://api.dicebear.com/7.x/avatars/svg?seed=default",
  "timezone": "UTC",
  "createdAt": "2026-04-12T10:00:00",
  "lastModifiedAt": "2026-04-12T10:00:00",
  "usernameChosen": true
}
```

Response headers include:

```
Set-Cookie: AUTH_TOKEN=<jwt>; Path=/; HttpOnly; SameSite=Strict
```

#### Error Responses

| Status | Condition | Example Body |
|---|---|---|
| `500 Internal Server Error` | Missing or null email | `{"status": 500, "error": "Internal Server Error", ...}` |

#### Example

```bash
curl -s -X POST http://localhost:8080/api/auth/dev-login \
  -H "Content-Type: application/json" \
  -d '{"email": "alice@example.com", "role": "USER"}' \
  -c cookies.txt
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
| `204 No Content` | No token present (still clears cookie) | Empty body |

#### Example

```bash
curl -X POST http://localhost:8080/api/auth/logout \
  -b cookies.txt \
  -c cookies.txt
```

---

## Models

### `UserResponse`

Response DTO containing non-sensitive user profile data. Uses `@JsonInclude(NON_NULL)` so null fields are omitted.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `id` | `Long` | No | Auto-generated unique identifier (BIGSERIAL) |
| `username` | `String` | No | The user's username |
| `role` | `Role` | No | The user's role (e.g., `USER`, `MODERATOR`, `ADMIN`, `OWNER`) |
| `email` | `String` | Yes | The user's email address (may be null if OAuth provider did not expose it) |
| `avatarUrl` | `String` | Yes | URL to avatar image |
| `timezone` | `String` | Yes | User's timezone |
| `createdAt` | `LocalDateTime` | No | Account creation timestamp |
| `lastModifiedAt` | `LocalDateTime` | No | Last update timestamp |
| `deletedAt` | `LocalDateTime` | Yes | Soft-deletion timestamp (privileged view only) |
| `bannedAt` | `LocalDateTime` | Yes | Ban timestamp (privileged view only) |
| `usernameChosen` | `Boolean` | No | `false` for first-time OAuth users who haven't completed username selection; `true` once chosen |

### `ErrorResponse`

Standard error response body.

| Field | Type | Nullable | Description |
|---|---|---|---|
| `status` | `int` | No | HTTP status code |
| `error` | `String` | No | Error category (e.g., `"Unauthorized"`) |
| `message` | `String` | No | Human-readable error message |
| `path` | `String` | No | Request URI path |
| `timestamp` | `LocalDateTime` | No | When the error occurred |

### `Role` (Enum)

User role hierarchy (highest to lowest privilege):

| Value | Description |
|---|---|
| `OWNER` | System owner, highest privilege |
| `ADMIN` | Administrator |
| `MODERATOR` | Moderator, can bypass ownership checks |
| `USER` | Standard user (default for new provisioned accounts) |

## Database Schema: `users` Table

Post-migration schema (after `V20260412085519583__oauth_only_auth_reset`):

| Column | Type | Nullable | Default | Constraints |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | No | Auto-increment | Primary key |
| `username` | `VARCHAR(100)` | No | -- | Unique; indexed |
| `email` | `VARCHAR(255)` | Yes | `NULL` | No unique constraint (uniqueness lives in `user_identities`) |
| `avatar_url` | `VARCHAR(500)` | Yes | `NULL` | -- |
| `timezone` | `VARCHAR(50)` | Yes | `NULL` | -- |
| `created_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | -- |
| `last_modified_at` | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | -- |
| `deleted_at` | `TIMESTAMP` | Yes | `NULL` | -- |
| `banned_at` | `TIMESTAMP` | Yes | `NULL` | -- |
| `role` | `VARCHAR(20)` | No | `'USER'` | -- |
| `username_chosen` | `BOOLEAN` | No | `false` | Set to `true` once the user completes the username selection flow |

## Database Schema: `user_identities` Table

OAuth identity records linked to users:

| Column | Type | Nullable | Default | Constraints |
|---|---|---|---|---|
| `id` | `BIGSERIAL` | No | Auto-increment | Primary key |
| `user_id` | `BIGINT` | No | -- | FK → `users(id)` ON DELETE CASCADE |
| `provider` | `VARCHAR(32)` | No | -- | e.g. `"google"`, `"github"` |
| `provider_sub` | `VARCHAR(255)` | No | -- | Stable user ID from the provider |
| `email` | `VARCHAR(255)` | Yes | `NULL` | -- |
| `display_name` | `VARCHAR(255)` | Yes | `NULL` | -- |
| `avatar_url` | `VARCHAR(500)` | Yes | `NULL` | -- |
| `linked_at` | `TIMESTAMPTZ` | No | `now()` | -- |
| `last_used_at` | `TIMESTAMPTZ` | Yes | `NULL` | -- |
| `created_at` | `TIMESTAMP` | No | `now()` | -- |
| `last_modified_at` | `TIMESTAMP` | No | `now()` | -- |
| `(provider, provider_sub)` | — | — | — | UNIQUE constraint |
