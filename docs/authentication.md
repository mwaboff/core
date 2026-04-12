# Authentication

This application uses OAuth2 exclusively for authentication. Password-based login has been removed.

---

## OAuth2 Flow

```
SPA                    Backend                      Google
 |                        |                             |
 |-- GET /oauth2/         |                             |
 |   authorization/google |                             |
 |                        |-- redirect to Google ------>|
 |                        |                             |
 |                        |<-- callback with auth code -|
 |                        |   /login/oauth2/code/google |
 |                        |                             |
 |                        | exchange code (scope: openid, email)
 |                        | find-or-create User +       |
 |                        | UserIdentity via            |
 |                        | OAuth2UserProvisioningService
 |                        |                             |
 |                        | issue JWT, set AUTH_TOKEN   |
 |                        | HttpOnly cookie             |
 |<-- redirect to         |                             |
 |   /choose-username     |   (first-time user)         |
 |   OR FRONTEND_BASE_URL |   (returning user)          |
```

**First-time users** receive a temporary `user-<random>` username and `usernameChosen: false`. The backend redirects them to `${FRONTEND_BASE_URL}/choose-username`. The SPA calls `POST /api/auth/choose-username` with the desired username — no re-login is needed since the JWT cookie was already set.

**Returning users** are redirected straight to `${FRONTEND_BASE_URL}`.

On failure (e.g., user denies consent), the backend redirects to `${FRONTEND_BASE_URL}/login?error`.

The JWT is stored in an `AUTH_TOKEN` HttpOnly cookie and read by `JwtAuthenticationFilter` on every subsequent request.

---

## Testing Lanes

### Local development & integration tests — `dev-login`

When running with `SPRING_PROFILES_ACTIVE=dev`, the `DevAuthController` exposes:

```
POST /api/auth/dev-login
Content-Type: application/json

{"email": "alice@example.com", "role": "USER", "username": "alice"}
```

This creates or retrieves a user by email (with `provider='dev'`), issues a JWT, and sets the cookie. Both `role` and `username` are optional — `role` defaults to `USER`, and `username` is generated from the email local-part if omitted. Dev users always have `usernameChosen: true`, so they skip the choose-username flow. Integration tests use this endpoint to authenticate without hitting Google.

### Staging / production — real Google OAuth

Point a browser at `/oauth2/authorization/google`. Requires `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET` to be configured (see GCP Setup below).

---

## GCP Setup (new developer)

1. **Create a GCP project** at [console.cloud.google.com](https://console.cloud.google.com).

2. **Configure the OAuth consent screen**
   - APIs & Services → OAuth consent screen
   - User type: External (or Internal for a Workspace org)
   - Fill in app name, support email, developer contact

3. **Create an OAuth Client ID**
   - APIs & Services → Credentials → Create Credentials → OAuth client ID
   - Application type: **Web application**

4. **Add authorized redirect URIs**
   ```
   http://localhost:8080/login/oauth2/code/google
   https://<your-production-domain>/login/oauth2/code/google
   ```

5. **Set environment variables** in `.env`:
   ```
   GOOGLE_CLIENT_ID=<client-id from GCP>
   GOOGLE_CLIENT_SECRET=<client-secret from GCP>
   FRONTEND_BASE_URL=http://localhost:5173
   ```

---

## Role Promotion

New OAuth users are provisioned with the `USER` role. To promote yourself to `OWNER`:

```sql
UPDATE users SET role = 'OWNER' WHERE email = '<your-email>';
```

Run this via `./scripts/connect-db.sh`.

---

## Adding Future Providers (Discord, Steam, etc.)

The schema is already provider-agnostic. Each OAuth identity is a row in `user_identities` with `(provider, provider_sub)` as a unique key. Multiple providers can be linked to a single `User`.

To add a new provider:

1. Add the Spring OAuth2 client registration to `application.yaml`:
   ```yaml
   spring.security.oauth2.client.registration.discord:
     client-id: ${DISCORD_CLIENT_ID}
     client-secret: ${DISCORD_CLIENT_SECRET}
     scope: identify,email
   ```

2. Add any provider-specific user info mapping in `OAuth2UserProvisioningService` if the attribute names differ from the standard `sub` / `email` / `name` / `picture` fields.

3. The redirect URI `GET /oauth2/authorization/discord` is registered automatically by Spring Security.

No schema changes are needed — new provider logins create new `user_identities` rows and optionally link to an existing `User` (future account-linking feature).
