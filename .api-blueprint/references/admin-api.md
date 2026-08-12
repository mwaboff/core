# Admin API

Base path: `/api/admin`

Authentication: JWT token via `AUTH_TOKEN` HttpOnly cookie. All endpoints require authentication; individual endpoints enforce role restrictions via `@PreAuthorize`.

---

## User Manager surface (MODERATOR+)

The following endpoints back the admin User Manager UI. All are open to `MODERATOR`, `ADMIN`, and `OWNER` (with the one deprecated exception noted below). Mutation endpoints additionally require the actor's role to be **strictly higher** than the target user's current role (via `RoleHierarchyService.canModifyUser`).

Every mutation writes a durable `admin_action_log` row in the same transaction as the state change. Bans and role changes also revoke all of the target's active JWT tokens.

### GET `/api/admin/users`

Paged list of non-deleted users for the admin user-list page.

**Required Role:** OWNER, ADMIN, or MODERATOR

**Query parameters**

| Name        | Type     | Default | Description                                                                 |
|-------------|----------|---------|-----------------------------------------------------------------------------|
| `page`      | Integer  | `0`     | Zero-based page number.                                                     |
| `size`      | Integer  | `50`    | Page size. Clamped server-side to `[1, 100]`.                               |
| `isBanned`  | Boolean  | —       | `true` returns only banned users; `false` returns only non-banned; omitted returns both. |
| `role`      | `Role`   | —       | Restrict to a specific role (`OWNER`, `ADMIN`, `MODERATOR`, `USER`).        |
| `username`  | String   | —       | Case-insensitive substring match on username.                               |
| `email`     | String   | —       | Case-insensitive substring match on email.                                  |
| `sort`      | String   | `id`    | One of `id`, `createdAt`, `lastSeenAt`, `username`. Other values return 400. |
| `ascending` | Boolean  | `false` | Sort direction.                                                             |

**Response:** `200 OK`

```json
{
  "content": [
    {
      "id": 42,
      "username": "alice",
      "avatarUrl": "https://cdn.example.com/u/42.png",
      "role": "USER",
      "banned": false,
      "createdAt": "2026-04-20T12:05:11",
      "lastSeenAt": "2026-04-23T15:42:08"
    },
    {
      "id": 43,
      "username": "bob",
      "role": "USER",
      "banned": true,
      "bannedAt": "2026-04-22T09:00:00",
      "banReason": "spam",
      "createdAt": "2026-04-18T08:05:00"
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 50
}
```

`AdminUserSummaryResponse` uses `@JsonInclude(NON_NULL)` — absent fields mean null.

**Errors**

| Status | Condition                                             |
|--------|-------------------------------------------------------|
| 400    | Invalid `sort` value                                  |
| 401    | No `AUTH_TOKEN` cookie                                |
| 403    | Caller's role is `USER`                               |

**curl**

```bash
# First page of USERs, newest-seen first
curl -s -b "AUTH_TOKEN=<jwt>" \
  "http://localhost:8080/api/admin/users?role=USER&sort=lastSeenAt&ascending=false"

# Search banned users whose email contains "spam"
curl -s -b "AUTH_TOKEN=<jwt>" \
  "http://localhost:8080/api/admin/users?isBanned=true&email=spam&size=100"
```

---

### GET `/api/admin/users/{userId}`

Detailed admin view of a single user. Identities are always populated; histories are opt-in via `?expand=`.

**Required Role:** OWNER, ADMIN, or MODERATOR

**Path parameters**

| Name     | Type | Required | Description      |
|----------|------|----------|------------------|
| `userId` | Long | Yes      | Target user id.  |

**Query parameters**

| Name     | Type   | Description                                                                                                                |
|----------|--------|----------------------------------------------------------------------------------------------------------------------------|
| `expand` | String | Comma-separated set of optional collections to include: `loginEvents`, `usernameHistory`, `adminActions`. Use `all` for every collection. |

**Response:** `200 OK` — `AdminUserDetailResponse`

```json
{
  "user": {
    "id": 42,
    "username": "alice",
    "role": "USER",
    "email": "alice@example.com",
    "avatarUrl": "https://cdn.example.com/u/42.png",
    "timezone": "America/New_York",
    "createdAt": "2026-04-20T12:05:11",
    "lastModifiedAt": "2026-04-23T09:11:00",
    "usernameChosen": true,
    "bannedAt": null,
    "banReason": null,
    "lastSeenAt": "2026-04-23T15:42:08"
  },
  "identities": [
    {
      "provider": "google",
      "displayName": "Alice Example",
      "linkedAt": "2026-04-20T12:05:11",
      "lastUsedAt": "2026-04-23T15:41:03"
    }
  ],
  "loginEvents": [
    {
      "id": 901,
      "provider": "google",
      "ipAddress": "198.51.100.14",
      "deviceInfo": "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/...",
      "createdAt": "2026-04-23T15:41:03"
    }
  ],
  "usernameHistory": [
    {
      "previousUsername": "user-temp-aa12",
      "newUsername": "alice",
      "changedByUserId": 42,
      "changedByUsername": "alice",
      "changedAt": "2026-04-20T12:06:00"
    }
  ],
  "adminActions": [
    {
      "id": 311,
      "actorUserId": 3,
      "actorUsername": "owner",
      "action": "USER_USERNAME_CHANGED",
      "details": "previous_username=alice; new_username=alice2",
      "ipAddress": "203.0.113.7",
      "createdAt": "2026-04-23T09:11:00"
    }
  ]
}
```

Each expanded collection returns at most the 50 most recent rows.

**Errors**

| Status | Condition                      |
|--------|--------------------------------|
| 401    | No `AUTH_TOKEN` cookie         |
| 403    | Caller's role is `USER`        |
| 404    | No user with this id           |

**curl**

```bash
# Core detail only
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/users/42"

# Everything — useful for the User Manager edit page
curl -s -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/users/42?expand=all"

# Just login events + admin actions
curl -s -b "AUTH_TOKEN=<jwt>" \
  "http://localhost:8080/api/admin/users/42?expand=loginEvents,adminActions"
```

---

### PATCH `/api/admin/users/{userId}`

Partial update of a user's admin-editable fields. Each changed field writes its own `admin_action_log` row and any of `username`, `role`, or `avatarUrl` may be supplied in the same request.

**Required Role:** OWNER, ADMIN, or MODERATOR.

Additional constraint — the unified role-change rule: for a `role` change, the actor must be strictly higher than **both** the target's current role and the proposed role. Consequence: `ADMIN` can promote `USER` → `MODERATOR` but cannot grant `ADMIN` (only `OWNER` can).

**Request body:** `application/json` — `UpdateAdminUserRequest`. Fields are optional; `null`/missing means "leave unchanged".

| Field       | Type   | Validation                     | Description                                    |
|-------------|--------|--------------------------------|------------------------------------------------|
| `username`  | String | `@Size(min=3, max=100)`         | New username. Also sets `usernameChosen=true`. |
| `avatarUrl` | String | `@Size(max=500)`                | New avatar URL.                                |
| `role`      | `Role` | must be a valid `Role` value   | New role. Revokes all active tokens on success.|
| `accessAllExpansions` | Boolean | — | Grants (`true`) or revokes (`false`) the target's non-SRD (paid expansion) content override, independent of `role`. `null`/omitted leaves the existing grant unchanged — never defaulted to `false`, so a PATCH that only edits `username` or `role` cannot silently revoke it. |

**Response:** `200 OK` — `AdminUserDetailResponse` with all collections populated (as if called with `?expand=all`).

**Errors**

| Status | Condition                                                                 |
|--------|---------------------------------------------------------------------------|
| 400    | Validation failure (e.g. username too short)                              |
| 401    | No `AUTH_TOKEN` cookie                                                    |
| 403    | Caller is `USER`, or not strictly higher than target, or trying to grant a role the caller is not strictly above |
| 404    | No user with this id                                                      |
| 409    | `username` already taken (case-insensitive)                               |

**Side effects**

- `username` change: writes one row to `username_history` (attributed to the admin) and one `USER_USERNAME_CHANGED` row to `admin_action_log`.
- `avatarUrl` change: writes one `USER_AVATAR_CHANGED` row.
- `role` change: writes one `USER_ROLE_CHANGED` row and revokes **all** active JWT tokens for the target user via `invalidateAllUserTokens`.
- `accessAllExpansions` change (only when it actually differs from the current value): writes one `USER_EXPANSION_ACCESS_CHANGED` row recording the previous and new value.

**curl**

```bash
# Change username only
curl -s -X PATCH -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"username":"alice2"}' \
  "http://localhost:8080/api/admin/users/42"

# Promote to MODERATOR and update avatar in one request
curl -s -X PATCH -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"role":"MODERATOR","avatarUrl":"https://cdn.example.com/u/42-v2.png"}' \
  "http://localhost:8080/api/admin/users/42"
```

---

### POST `/api/admin/users/{userId}/ban`

Ban a user. Optionally record a human-readable reason. Revokes all of the target's active JWT tokens.

**Required Role:** OWNER, ADMIN, or MODERATOR (strictly higher than target).

**Request body (optional):** `application/json` — `BanUserRequest`

| Field    | Type   | Validation          | Description                    |
|----------|--------|---------------------|--------------------------------|
| `reason` | String | `@Size(max=500)`    | Human-readable ban reason. Persisted on the user record and shown in the admin UI. |

Omitting the body (or sending `{}`) bans the user with no reason.

**Response:** `200 OK` — `AdminUserDetailResponse` (all collections populated). `user.banReason` reflects the stored value (or is omitted when null, per `@JsonInclude(NON_NULL)`).

**Errors**

| Status | Condition                                             |
|--------|-------------------------------------------------------|
| 400    | `reason` exceeds 500 characters                       |
| 401    | No `AUTH_TOKEN` cookie                                |
| 403    | Caller is `USER`, or not strictly higher than target  |
| 404    | No user with this id                                  |

**Side effects**

- Writes a `USER_BANNED` row to `admin_action_log` (`details=reason=…` or `details=reason=`).
- Revokes all active JWT tokens for the target user.

**curl**

```bash
# With a reason
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"reason":"persistent rulebreaking"}' \
  "http://localhost:8080/api/admin/users/42/ban"

# No reason — legacy behavior
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  "http://localhost:8080/api/admin/users/42/ban"
```

---

### POST `/api/admin/users/{userId}/unban`

Unban a user. Clears `bannedAt` and `banReason`.

**Required Role:** OWNER, ADMIN, or MODERATOR (strictly higher than target).

**Request body:** none.

**Response:** `200 OK` — `AdminUserDetailResponse`.

**Side effects:** writes a `USER_UNBANNED` row to `admin_action_log`.

**curl**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  "http://localhost:8080/api/admin/users/42/unban"
```

---

### POST `/api/admin/users/{userId}/change-role` (deprecated)

> **Deprecated.** New clients should use `PATCH /api/admin/users/{userId}` with `{"role": "..."}`. Retained only so existing OWNER-only callers continue to work until they migrate.

**Required Role:** OWNER only (kept at `@PreAuthorize("hasRole('OWNER')")` for backwards compatibility). Internally delegates to the same service method as `PATCH`, so the unified role-change rule still applies at the service layer.

**Request body:** `application/json`

```json
{ "newRole": "ADMIN" }
```

**Response:** `200 OK` — `AdminUserDetailResponse` (same shape as `PATCH`).

**curl**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"newRole":"ADMIN"}' \
  "http://localhost:8080/api/admin/users/7/change-role"
```

---

## PATCH `/api/admin/content/srd`

Bulk-flags or unflags game content as SRD-licensed. This is the only way `srd` is ever set `true` for content — every row was backfilled to `srd = false` when the column was added. Backs the admin bulk SRD-flagging tool.

**Required Role:** OWNER or ADMIN (deliberately not MODERATOR — SRD licensing is a legal classification of the content, not a moderation judgment call).

**Request body:** `application/json` — `BulkSrdUpdateRequest`

| Field  | Type      | Validation                    | Description                                                                                     |
|--------|-----------|--------------------------------|---------------------------------------------------------------------------------------------------|
| `type` | String    | `@NotNull`                     | A `SearchableEntityType` constant name (e.g. `"WEAPON"`, `"DOMAIN"`), case-insensitive. `EXPANSION` and `SUBCLASS_CARD` are rejected — see below. |
| `ids`  | `Long[]`  | `@NotEmpty`                    | The ids to flag or unflag. Ids that don't exist for `type` are reported back, not rejected.       |
| `srd`  | Boolean   | `@NotNull`                     | `true` to mark the matched rows SRD, `false` to unmark them.                                      |

`type=EXPANSION` is rejected (a sourcebook is not content; carries no `srd` column). `type=SUBCLASS_CARD` is rejected with a redirect hint — a subclass card's `srd` is always re-derived from its subclass path, so flagging cards directly would either be silently overwritten on their next save or drift out of sync with the path; flag `type=SUBCLASS_PATH` instead, and its three Foundation/Specialization/Mastery cards follow automatically in the same transaction.

**Response:** `200 OK` — `BulkSrdUpdateResponse`

```json
{
  "type": "DOMAIN",
  "srd": true,
  "updatedIds": [1, 2],
  "unknownIds": [999]
}
```

Unknown ids are a **partial success**, not an error — the ids that did resolve are still updated, and `unknownIds` lets the admin UI show a partial-success summary rather than failing the whole batch.

**Errors**

| Status | Condition                                                                 |
|--------|-----------------------------------------------------------------------------|
| 400    | Validation failure (missing `type`/`ids`/`srd`), unrecognized `type` string, or `type` is `EXPANSION`/`SUBCLASS_CARD` |
| 401    | No `AUTH_TOKEN` cookie                                                    |
| 403    | Caller's role is below `ADMIN`                                           |

**Side effects:** writes one `CONTENT_SRD_CHANGED` row to `admin_action_log` for the whole batch (not one per id), with `details=type=…; srd=…; requested=N; updated=M; unknown=[...]`. For `type=SUBCLASS_PATH`, every matched path's cards are re-derived from the path's new `srd` in the same transaction.

**curl**

```bash
# Flag two domains as SRD
curl -s -X PATCH -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"type":"DOMAIN","ids":[1,2],"srd":true}' \
  "http://localhost:8080/api/admin/content/srd"

# Unflag a subclass path (cascades to its Foundation/Specialization/Mastery cards)
curl -s -X PATCH -b "AUTH_TOKEN=<jwt>" \
  -H "Content-Type: application/json" \
  -d '{"type":"SUBCLASS_PATH","ids":[7],"srd":false}' \
  "http://localhost:8080/api/admin/content/srd"
```

---

## POST `/api/admin/search/reindex`

Rebuild the full-text search index by clearing all entries and re-indexing every active entity from the source repositories. An expensive admin-only operation intended for recovery scenarios. Requires `OWNER` role.

**Query parameters**

| Parameter | Type                   | Required | Description                                                                 |
|-----------|------------------------|----------|-----------------------------------------------------------------------------|
| `type`    | `SearchableEntityType` | No       | If provided, only that entity type is rebuilt. If omitted, all types are rebuilt. |

**Response**: `200 OK` with a JSON map: `{ "scope": "ALL"|"<TYPE>", "indexed": N }`.

**cURL**

```bash
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/search/reindex"
curl -s -X POST -b "AUTH_TOKEN=<jwt>" "http://localhost:8080/api/admin/search/reindex?type=WEAPON"
```

---

## Models

### `AdminUserSummaryResponse`

| Field        | Type          | Notes                                               |
|--------------|---------------|-----------------------------------------------------|
| `id`         | Long          |                                                     |
| `username`   | String        |                                                     |
| `avatarUrl`  | String        | Omitted when null.                                  |
| `role`       | `Role`        | `OWNER` / `ADMIN` / `MODERATOR` / `USER`.            |
| `banned`     | Boolean       | Derived from `bannedAt` presence.                   |
| `bannedAt`   | LocalDateTime | Omitted when null.                                  |
| `banReason`  | String        | Omitted when null.                                  |
| `createdAt`  | LocalDateTime |                                                     |
| `lastSeenAt` | LocalDateTime | Updated by the JWT filter, throttled to 5 minutes.  |

### `AdminUserDetailResponse`

| Field             | Type                              | Notes                                                      |
|-------------------|-----------------------------------|------------------------------------------------------------|
| `user`            | `UserResponse`                    | Always populated with privileged fields.                   |
| `identities`      | `UserIdentityResponse[]`          | Always populated. Typically 1–3 rows.                      |
| `loginEvents`     | `LoginEventResponse[]`            | Present only when `?expand=loginEvents` or `all`.          |
| `usernameHistory` | `UsernameHistoryResponse[]`       | Present only when `?expand=usernameHistory` or `all`.      |
| `adminActions`    | `AdminActionResponse[]`           | Present only when `?expand=adminActions` or `all`.         |

### `UserResponse` (additions for privileged callers)

| New field     | Type          | Notes                                                         |
|---------------|---------------|---------------------------------------------------------------|
| `banReason`   | String        | Populated only for privileged callers. Omitted when null.     |
| `lastSeenAt`  | LocalDateTime | Populated only for privileged callers. Omitted when null.     |
| `accessAllExpansions` | Boolean | Whether this user is manually granted non-SRD content visibility, independent of `role`. Always present here since this table backs the admin (privileged) detail view. On the general-purpose `UserResponse` used by `users-api.md`/`auth-api.md`, this field is gated self-or-privileged like `email`/`timezone` — an arbitrary other USER cannot see it. Omitted when null. |

### `UserIdentityResponse`

| Field         | Type          |
|---------------|---------------|
| `provider`    | String        |
| `displayName` | String        |
| `linkedAt`    | LocalDateTime |
| `lastUsedAt`  | LocalDateTime |

Note: `providerSub` and identity-level `email` are deliberately omitted.

### `LoginEventResponse`

| Field        | Type          |
|--------------|---------------|
| `id`         | Long          |
| `provider`   | String        |
| `ipAddress`  | String        |
| `deviceInfo` | String        |
| `createdAt`  | LocalDateTime |

### `UsernameHistoryResponse`

| Field              | Type          |
|--------------------|---------------|
| `previousUsername` | String        |
| `newUsername`      | String        |
| `changedByUserId`  | Long          |
| `changedByUsername`| String        |
| `changedAt`        | LocalDateTime |

### `AdminActionResponse`

| Field          | Type                 |
|----------------|----------------------|
| `id`           | Long                 |
| `actorUserId`  | Long (nullable)      |
| `actorUsername`| String (nullable)    |
| `action`       | `AdminActionType`    |
| `details`      | String               |
| `ipAddress`    | String               |
| `createdAt`    | LocalDateTime        |

### `UpdateAdminUserRequest`

| Field       | Type   | Validation                     |
|-------------|--------|--------------------------------|
| `username`  | String | `@Size(min=3, max=100)`         |
| `avatarUrl` | String | `@Size(max=500)`                |
| `role`      | `Role` | must be a valid `Role` value   |

### `BanUserRequest`

| Field    | Type   | Validation          |
|----------|--------|---------------------|
| `reason` | String | `@Size(max=500)`    |

### `BulkSrdUpdateRequest`

| Field  | Type      | Validation      |
|--------|-----------|-----------------|
| `type` | String    | `@NotNull`      |
| `ids`  | `Long[]`  | `@NotEmpty`     |
| `srd`  | Boolean   | `@NotNull`      |

### `BulkSrdUpdateResponse`

| Field        | Type      | Notes                                        |
|--------------|-----------|-----------------------------------------------|
| `type`       | String    | The resolved `SearchableEntityType` name.      |
| `srd`        | Boolean   | The srd value the batch applied.               |
| `updatedIds` | `Long[]`  | Ids that were found and updated.                |
| `unknownIds` | `Long[]`  | Ids from the request that don't exist for `type`. Not an error. |

---

## Enums

### Role

Hierarchy (highest to lowest): `OWNER > ADMIN > MODERATOR > USER`. Database constraint: `VARCHAR(20) NOT NULL DEFAULT 'USER'` on `users.role`.

### AdminActionType

Values written to `admin_action_log.action`. A PostgreSQL `CHECK` constraint enforces this set, so adding a value requires both an enum change and a Flyway migration.

| Value                     | When written                                               |
|---------------------------|------------------------------------------------------------|
| `USER_BANNED`             | `POST /api/admin/users/{id}/ban`                           |
| `USER_UNBANNED`           | `POST /api/admin/users/{id}/unban`                         |
| `USER_ROLE_CHANGED`       | `PATCH /api/admin/users/{id}` (role change) or `/change-role` |
| `USER_USERNAME_CHANGED`   | `PATCH /api/admin/users/{id}` (username change)             |
| `USER_AVATAR_CHANGED`     | `PATCH /api/admin/users/{id}` (avatarUrl change)            |
| `USER_EXPANSION_ACCESS_CHANGED` | `PATCH /api/admin/users/{id}` (accessAllExpansions change) |
| `CONTENT_SRD_CHANGED`     | `PATCH /api/admin/content/srd`. Unlike every other value, this one has no target user — one row per batch, not per id. |

---

## Authorization Summary

| Endpoint                                        | OWNER | ADMIN     | MODERATOR | USER     |
|-------------------------------------------------|-------|-----------|-----------|----------|
| `GET /api/admin/users`                          | Yes   | Yes       | Yes       | No (403) |
| `GET /api/admin/users/{id}`                     | Yes   | Yes       | Yes       | No (403) |
| `PATCH /api/admin/users/{id}`                   | Yes   | Yes\*     | Yes\*     | No (403) |
| `POST /api/admin/users/{id}/ban`                | Yes   | Yes\*     | Yes\*     | No (403) |
| `POST /api/admin/users/{id}/unban`              | Yes   | Yes\*     | Yes\*     | No (403) |
| `POST /api/admin/users/{id}/change-role` (deprecated) | Yes | No (403) | No (403) | No (403) |
| `PATCH /api/admin/content/srd`                  | Yes   | Yes       | No (403)  | No (403) |
| `POST /api/admin/search/reindex`                | Yes   | No (403)  | No (403)  | No (403) |

\* Mutations additionally require the actor's role to be strictly higher than the target user's role. Role changes further require the actor to be strictly higher than the proposed new role — so an ADMIN cannot grant `ADMIN` even though ADMIN is an allowed caller on `PATCH`.
