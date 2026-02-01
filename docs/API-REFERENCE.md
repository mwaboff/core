# Daggerheart API Reference

API documentation for the Daggerheart TTRPG character management backend. This document provides everything needed to build a frontend client.

## Table of Contents

- [Overview](#overview)
- [Authentication](#authentication)
- [API Conventions](#api-conventions)
- [Core Endpoints](#core-endpoints)
- [Daggerheart Endpoints](#daggerheart-endpoints)
- [DTOs Reference](#dtos-reference)
- [Enums Reference](#enums-reference)

---

## Overview

- **Base URL**: `http://localhost:8080` (development)
- **API Prefix**: All endpoints start with `/api`
- **Content-Type**: `application/json`
- **Authentication**: JWT stored in HttpOnly cookies

---

## Authentication

### Mechanism

The API uses JWT tokens stored in HttpOnly cookies. The cookie is automatically set on successful login and cleared on logout.

- **Cookie Name**: `AUTH_TOKEN`
- **Token Expiration**: 30 days
- **Cookie Attributes**: HttpOnly, Secure (in production), SameSite=Lax

### Auth Endpoints

| Endpoint | Method | Request Body | Response | Description |
|----------|--------|--------------|----------|-------------|
| `/api/auth/register` | POST | `RegisterRequest` | `UserResponse` | Create new account |
| `/api/auth/login` | POST | `LoginRequest` | `UserResponse` | Login (sets auth cookie) |
| `/api/auth/logout` | POST | None | 204 No Content | Logout (clears cookie) |

### Request DTOs

```typescript
// LoginRequest
{
  usernameOrEmail: string;  // Required
  password: string;         // Required
}

// RegisterRequest
{
  username: string;   // Required, 3-100 chars, alphanumeric + _ -
  email: string;      // Required, valid email
  password: string;   // Required, 8-100 chars
  timezone?: string;  // Optional, e.g., "America/New_York"
  avatarUrl?: string; // Optional
}
```

### Frontend Implementation Notes

1. **Credentials**: Always include credentials in requests:
   ```typescript
   fetch(url, { credentials: 'include' })
   // or with HttpClient
   http.get(url, { withCredentials: true })
   ```

2. **CORS**: The backend allows credentials from configured origins.

3. **401 Handling**: Redirect to login page when receiving 401 Unauthorized.

---

## API Conventions

### Pagination

All list endpoints return paginated responses:

```typescript
interface PagedResponse<T> {
  content: T[];
  page: number;        // Zero-based page number
  size: number;        // Items per page
  totalElements: number;
  totalPages: number;
}
```

**Query Parameters**:
- `page`: Page number (default: 0)
- `size`: Items per page (default: 20, max: 100)

### Response Expansion

Many endpoints support expanding related objects using the `expand` query parameter:

```
GET /api/dh/character-sheets/1?expand=owner,experiences,inventoryWeapons
```

Available expand fields are documented per endpoint.

### Soft Deletion

Most entities use soft deletion. Deleted items have a `deletedAt` timestamp. Admin/Owner users can:
- Include deleted items with `?includeDeleted=true`
- Restore deleted items via `POST /{id}/restore`

### Error Responses

```typescript
interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
}
```

Common status codes:
- `400` - Bad Request (validation errors)
- `401` - Unauthorized (not logged in)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found
- `409` - Conflict (e.g., username already exists)
- `429` - Too Many Requests (rate limited)

---

## Core Endpoints

### User Management (`/api/users`)

| Endpoint | Method | Request | Response | Access | Description |
|----------|--------|---------|----------|--------|-------------|
| `/api/users/me` | GET | - | `UserResponse` | Auth | Get current user |
| `/api/users/{userId}` | GET | - | `UserResponse` | Auth | Get user by ID |
| `/api/users/me` | PATCH | `UpdateUserRequest` | `UserResponse` | Auth | Update profile |
| `/api/users/me/change-password` | POST | `ChangePasswordRequest` | 204 | Auth | Change password |
| `/api/users/me` | DELETE | - | 204 | Auth | Delete account |

```typescript
// UpdateUserRequest
{
  email?: string;
  avatarUrl?: string;
  timezone?: string;
}

// ChangePasswordRequest
{
  currentPassword: string;  // Required
  newPassword: string;      // Required, 8-100 chars
}

// UserResponse
{
  id: number;
  username: string;
  email: string;
  avatarUrl?: string;
  timezone?: string;
  role: Role;
  createdAt: string;         // ISO datetime
  lastModifiedAt: string;
  accountLockedUntil?: string;
  failedLoginAttempts?: number;
  deletedAt?: string;
  bannedAt?: string;
}
```

### Admin Endpoints (`/api/admin`)

Requires MODERATOR+ role.

| Endpoint | Method | Request | Response | Access | Description |
|----------|--------|---------|----------|--------|-------------|
| `/api/admin/login-history` | GET | - | `LoginAttemptResponse[]` | ADMIN+ | All login attempts |
| `/api/admin/login-history/user/{userId}` | GET | - | `LoginAttemptResponse[]` | ADMIN+ | User's login history |
| `/api/admin/login-history/ip/{ip}` | GET | - | `LoginAttemptResponse[]` | ADMIN+ | Login attempts by IP |
| `/api/admin/users/{userId}/ban` | POST | - | `UserResponse` | MOD+ | Ban user |
| `/api/admin/users/{userId}/unban` | POST | - | `UserResponse` | MOD+ | Unban user |
| `/api/admin/users/{userId}/change-role` | POST | `ChangeRoleRequest` | `UserResponse` | OWNER | Change user role |

```typescript
// LoginAttemptResponse
{
  id: number;
  userId?: number;
  usernameAttempted: string;
  success: boolean;
  failureReason?: string;
  ipAddress: string;
  userAgent?: string;
  createdAt: string;
}

// ChangeRoleRequest
{
  newRole: Role;  // "OWNER" | "ADMIN" | "MODERATOR" | "USER"
}
```

---

## Daggerheart Endpoints

### Character Sheets (`/api/dh/character-sheets`)

| Endpoint | Method | Request | Response | Access | Expand |
|----------|--------|---------|----------|--------|--------|
| `/` | GET | - | `PagedResponse<CharacterSheetResponse>` | MOD+ | owner, experiences, inventoryWeapons, inventoryArmors, inventoryLoot, ancestryCard, communityCard, subclassCards, domainCards |
| `/{id}` | GET | - | `CharacterSheetResponse` | Auth | (same) |
| `/` | POST | `CreateCharacterSheetRequest` | `CharacterSheetResponse` | Auth | - |
| `/{id}` | PUT | `UpdateCharacterSheetRequest` | `CharacterSheetResponse` | Owner/MOD+ | - |
| `/{id}` | DELETE | - | 204 | Owner/MOD+ | - |

**Query Parameters**:
- `page`, `size` - Pagination
- `ownerId` - Filter by owner
- `name` - Search by name (contains)
- `minLevel`, `maxLevel` - Level range filter

### Experiences (`/api/dh/experiences`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<ExperienceResponse>` | Auth |
| `/{id}` | GET | - | `ExperienceResponse` | Auth |
| `/` | POST | `CreateExperienceRequest` | `ExperienceResponse` | Auth |
| `/{id}` | PUT | `UpdateExperienceRequest` | `ExperienceResponse` | Owner/MOD+ |
| `/{id}` | DELETE | - | 204 | Owner/MOD+ |

**Query Parameters**: `characterSheetId`, `companionId`

### Companions (`/api/dh/companions`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<CompanionResponse>` | Auth |
| `/{id}` | GET | - | `CompanionResponse` | Auth |
| `/` | POST | `CreateCompanionRequest` | `CompanionResponse` | Owner/MOD+ |
| `/{id}` | PUT | `UpdateCompanionRequest` | `CompanionResponse` | Owner/MOD+ |
| `/{id}` | DELETE | - | 204 | Owner/MOD+ |

**Query Parameters**: `characterSheetId`

### Campaigns (`/api/dh/campaigns`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<CampaignResponse>` | MOD+ |
| `/{id}` | GET | - | `CampaignResponse` | Participant/MOD+ |
| `/` | POST | `CreateCampaignRequest` | `CampaignResponse` | Auth |
| `/{id}` | PUT | `UpdateCampaignRequest` | `CampaignResponse` | Creator/MOD+ |
| `/{id}` | DELETE | - | 204 | Creator/MOD+ |
| `/{id}/game-masters/{userId}` | POST | - | `CampaignResponse` | Creator/MOD+ |
| `/{id}/game-masters/{userId}` | DELETE | - | `CampaignResponse` | Creator/MOD+ |
| `/{id}/players/{userId}` | POST | - | `CampaignResponse` | Creator/GM/MOD+ |
| `/{id}/players/{userId}` | DELETE | - | `CampaignResponse` | Creator/GM/MOD+ |
| `/{id}/character-sheets/{sheetId}/submit` | POST | - | `CampaignResponse` | Sheet Owner |
| `/{id}/character-sheets/{sheetId}/approve` | POST | - | `CampaignResponse` | GM/MOD+ |
| `/{id}/character-sheets/{sheetId}/reject` | POST | - | `CampaignResponse` | GM/MOD+ |
| `/{id}/npcs/{sheetId}` | POST | - | `CampaignResponse` | GM/MOD+ |
| `/{id}/character-sheets/{sheetId}` | DELETE | - | `CampaignResponse` | GM/MOD+ |

### Adversaries (`/api/dh/adversaries`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<AdversaryResponse>` | Auth |
| `/{id}` | GET | - | `AdversaryResponse` | Auth |
| `/` | POST | `CreateAdversaryRequest` | `AdversaryResponse` | Auth |
| `/batch` | POST | `BatchCreateAdversaryRequest` | `BatchCreateAdversaryResponse` | MOD+ |
| `/{id}/copy` | POST | - | `AdversaryResponse` | Auth |
| `/{id}` | PUT | `UpdateAdversaryRequest` | `AdversaryResponse` | Creator/MOD+ |
| `/{id}` | DELETE | - | 204 | Creator/MOD+ |
| `/{id}/restore` | POST | - | `AdversaryResponse` | ADMIN+ |

**Query Parameters**: `expansionId`, `tier`, `adversaryType`, `isOfficial`, `name`
**Expand**: `expansion`, `creator`, `originalAdversary`, `experiences`, `features`

### Encounters (`/api/dh/encounters`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<EncounterResponse>` | Auth |
| `/{id}` | GET | - | `EncounterResponse` | Auth |
| `/` | POST | `CreateEncounterRequest` | `EncounterResponse` | Auth |
| `/{id}/copy` | POST | - | `EncounterResponse` | Auth |
| `/{id}` | PUT | `UpdateEncounterRequest` | `EncounterResponse` | Creator/MOD+ |
| `/{id}` | DELETE | - | 204 | Creator/MOD+ |
| `/{id}/restore` | POST | - | `EncounterResponse` | ADMIN+ |
| `/{id}/adversaries?adversaryId={id}` | POST | - | `EncounterResponse` | Creator/MOD+ |
| `/{id}/adversaries/{encounterAdversaryId}` | DELETE | - | 204 | Creator/MOD+ |

**Query Parameters**: `campaignId`, `tier`, `isOfficial`, `name`
**Expand**: `creator`, `campaign`, `originalEncounter`, `adversaryDetails`

### Weapons (`/api/dh/weapons`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<WeaponResponse>` | Auth |
| `/{id}` | GET | - | `WeaponResponse` | Auth |
| `/` | POST | `CreateWeaponRequest` | `WeaponResponse` | ADMIN+ |
| `/bulk` | POST | `CreateWeaponRequest[]` | `WeaponResponse[]` | ADMIN+ |
| `/{id}` | PUT | `UpdateWeaponRequest` | `WeaponResponse` | ADMIN+ |
| `/{id}` | DELETE | - | 204 | ADMIN+ |
| `/{id}/restore` | POST | - | `WeaponResponse` | ADMIN+ |

**Query Parameters**: `expansionId`, `isOfficial`, `trait`, `range`, `burden`, `isPrimary`

### Armor (`/api/dh/armors`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<ArmorResponse>` | Auth |
| `/{id}` | GET | - | `ArmorResponse` | Auth |
| `/` | POST | `CreateArmorRequest` | `ArmorResponse` | ADMIN+ |
| `/bulk` | POST | `CreateArmorRequest[]` | `ArmorResponse[]` | ADMIN+ |
| `/{id}` | PUT | `UpdateArmorRequest` | `ArmorResponse` | ADMIN+ |
| `/{id}` | DELETE | - | 204 | ADMIN+ |
| `/{id}/restore` | POST | - | `ArmorResponse` | ADMIN+ |

### Loot (`/api/dh/loot`)

Same pattern as Armor.

### Cards

All card endpoints follow the same CRUD pattern:

- **Ancestry Cards**: `/api/dh/cards/ancestry`
- **Community Cards**: `/api/dh/cards/community`
- **Subclass Cards**: `/api/dh/cards/subclass`
  - Additional query params: `associatedClassId`, `level`
- **Domain Cards**: `/api/dh/cards/domain`
  - Additional query params: `associatedDomainId`, `type`

### Classes (`/api/dh/classes`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<ClassResponse>` | Auth |
| `/{id}` | GET | - | `ClassResponse` | Auth |
| `/` | POST | `CreateClassRequest` | `ClassResponse` | ADMIN+ |
| `/bulk` | POST | `CreateClassRequest[]` | `ClassResponse[]` | ADMIN+ |
| `/{id}` | PUT | `UpdateClassRequest` | `ClassResponse` | ADMIN+ |
| `/{id}` | DELETE | - | 204 | ADMIN+ |
| `/{id}/restore` | POST | - | `ClassResponse` | ADMIN+ |

**Expand**: `expansion`, `associatedDomains`

### Domains (`/api/dh/domains`)

Same pattern as Classes.

### Features (`/api/dh/features`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<FeatureResponse>` | Auth |
| `/{id}` | GET | - | `FeatureResponse` | Auth |
| `/` | POST | `CreateFeatureRequest` | `FeatureResponse` | ADMIN+ |
| `/{id}` | PUT | `UpdateFeatureRequest` | `FeatureResponse` | ADMIN+ |
| `/{id}` | DELETE | - | 204 | ADMIN+ |
| `/{id}/restore` | POST | - | `FeatureResponse` | ADMIN+ |

**Query Parameters**: `expansionId`, `featureType`

### Questions (`/api/dh/questions`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<QuestionResponse>` | Auth |
| `/{id}` | GET | - | `QuestionResponse` | Auth |
| `/` | POST | `CreateQuestionRequest` | `QuestionResponse` | ADMIN+ |
| `/bulk` | POST | `CreateQuestionRequest[]` | `QuestionResponse[]` | ADMIN+ |
| `/{id}` | PUT | `UpdateQuestionRequest` | `QuestionResponse` | ADMIN+ |
| `/{id}` | DELETE | - | 204 | ADMIN+ |
| `/{id}/restore` | POST | - | `QuestionResponse` | ADMIN+ |

**Query Parameters**: `expansionId`, `questionType`

### Expansions (`/api/dh/expansions`)

| Endpoint | Method | Request | Response | Access |
|----------|--------|---------|----------|--------|
| `/` | GET | - | `PagedResponse<ExpansionResponse>` | Auth |
| `/{id}` | GET | - | `ExpansionResponse` | Auth |
| `/` | POST | `CreateExpansionRequest` | `ExpansionResponse` | ADMIN+ |
| `/{id}` | PUT | `UpdateExpansionRequest` | `ExpansionResponse` | ADMIN+ |
| `/{id}` | DELETE | - | 204 | ADMIN+ |
| `/{id}/restore` | POST | - | `ExpansionResponse` | ADMIN+ |

**Query Parameters**: `published`

---

## DTOs Reference

### Common Patterns

**Base Response Fields** (most entities include):
```typescript
{
  id: number;
  createdAt: string;       // ISO datetime
  lastModifiedAt: string;  // ISO datetime
  deletedAt?: string;      // Present if soft-deleted
}
```

**Game Content Fields** (cards, items, adversaries):
```typescript
{
  isOfficial: boolean;     // Official game content
  isPublic: boolean;       // Visible to all users
  expansionId?: number;    // Source expansion
}
```

### Character Sheet DTOs

```typescript
// CreateCharacterSheetRequest
{
  name: string;                    // Required
  pronouns?: string;
  level?: number;                  // Default: 1
  currentHitPoints?: number;
  maxHitPoints?: number;
  currentStress?: number;
  maxStress?: number;
  currentHope?: number;
  maxHope?: number;
  currentArmor?: number;
  evasion?: number;
  majorThreshold?: number;
  severeThreshold?: number;
  gold?: number;
  proficiency?: number;
  agility?: number;
  strength?: number;
  finesse?: number;
  instinct?: number;
  presence?: number;
  knowledge?: number;
  backstory?: string;
  notes?: string;
  appearance?: string;
  avatarUrl?: string;
  ancestryCardId?: number;
  communityCardId?: number;
  subclassCardIds?: number[];
  domainCardIds?: number[];
  inventoryWeaponIds?: number[];
  inventoryArmorIds?: number[];
  inventoryLootIds?: number[];
}

// CharacterSheetResponse
{
  id: number;
  name: string;
  pronouns?: string;
  level: number;
  currentHitPoints: number;
  maxHitPoints: number;
  currentStress: number;
  maxStress: number;
  currentHope: number;
  maxHope: number;
  currentArmor: number;
  evasion: number;
  majorThreshold: number;
  severeThreshold: number;
  gold: number;
  proficiency: number;
  agility: number;
  strength: number;
  finesse: number;
  instinct: number;
  presence: number;
  knowledge: number;
  backstory?: string;
  notes?: string;
  appearance?: string;
  avatarUrl?: string;
  ownerId: number;
  createdAt: string;
  lastModifiedAt: string;
  deletedAt?: string;

  // Expanded fields (when requested)
  owner?: UserResponse;
  experiences?: ExperienceResponse[];
  ancestryCard?: AncestryCardResponse;
  communityCard?: CommunityCardResponse;
  subclassCards?: SubclassCardResponse[];
  domainCards?: DomainCardResponse[];
  inventoryWeapons?: WeaponResponse[];
  inventoryArmors?: ArmorResponse[];
  inventoryLoot?: LootResponse[];
}
```

### Campaign DTOs

```typescript
// CreateCampaignRequest
{
  name: string;          // Required
  description?: string;
}

// CampaignResponse
{
  id: number;
  name: string;
  description?: string;
  creatorId: number;
  createdAt: string;
  lastModifiedAt: string;
  deletedAt?: string;

  gameMasterIds: number[];
  playerIds: number[];
  approvedCharacterSheetIds: number[];
  pendingCharacterSheetIds: number[];
  npcCharacterSheetIds: number[];

  // Expanded fields
  creator?: UserResponse;
  gameMasters?: UserResponse[];
  players?: UserResponse[];
  approvedCharacterSheets?: CharacterSheetResponse[];
  pendingCharacterSheets?: CharacterSheetResponse[];
  npcCharacterSheets?: CharacterSheetResponse[];
}
```

### Adversary DTOs

```typescript
// CreateAdversaryRequest
{
  name: string;              // Required
  description?: string;
  tier?: number;             // Default: 1
  adversaryType: AdversaryType;  // Required
  difficulty?: number;
  attack?: string;
  damage?: DamageRoll;
  hitPoints?: number;
  stress?: number;
  experience?: number;
  attackModifier?: number;
  evasion?: number;
  thresholds?: AdversaryThresholds;
  expansionId?: number;
  isOfficial?: boolean;
  isPublic?: boolean;
}

// DamageRoll
{
  diceType: DiceType;        // D4, D6, D8, D10, D12, D20
  diceCount: number;
  modifier?: number;
  damageType: DamageType;    // PHYSICAL, MAGIC
}

// AdversaryThresholds
{
  minor?: number;
  major?: number;
  severe?: number;
}
```

### Weapon DTOs

```typescript
// CreateWeaponRequest
{
  name: string;              // Required
  description?: string;
  trait: Trait;              // Required
  range: Range;              // Required
  damage: DamageRoll;        // Required
  burden: Burden;            // Required
  feature?: string;
  isPrimary?: boolean;
  expansionId?: number;
  isOfficial?: boolean;
  isPublic?: boolean;
}

// WeaponResponse
{
  id: number;
  name: string;
  description?: string;
  trait: Trait;
  range: Range;
  damage: DamageRoll;
  burden: Burden;
  feature?: string;
  isPrimary: boolean;
  expansionId?: number;
  isOfficial: boolean;
  isPublic: boolean;
  createdAt: string;
  lastModifiedAt: string;
  deletedAt?: string;

  // Expanded
  expansion?: ExpansionResponse;
}
```

---

## Enums Reference

### Role (User Permission Levels)

```typescript
type Role = "OWNER" | "ADMIN" | "MODERATOR" | "USER";
// Hierarchy: OWNER > ADMIN > MODERATOR > USER
```

### Trait (Character Attributes)

```typescript
type Trait =
  | "AGILITY"
  | "STRENGTH"
  | "FINESSE"
  | "INSTINCT"
  | "PRESENCE"
  | "KNOWLEDGE";
```

### Range (Weapon Distance)

```typescript
type Range =
  | "MELEE"
  | "VERY_CLOSE"
  | "CLOSE"
  | "FAR"
  | "VERY_FAR"
  | "OUT_OF_RANGE";
```

### Burden (Weapon Hands)

```typescript
type Burden = "ONE_HANDED" | "TWO_HANDED";
```

### DamageType

```typescript
type DamageType = "PHYSICAL" | "MAGIC";
```

### DiceType

```typescript
type DiceType = "D4" | "D6" | "D8" | "D10" | "D12" | "D20";
```

### AdversaryType

```typescript
type AdversaryType =
  | "MINION"    // 1 battle point
  | "SOCIAL"    // 1 battle point
  | "SUPPORT"   // 1 battle point
  | "HORDE"     // 2 battle points
  | "RANGED"    // 2 battle points
  | "SKULK"     // 2 battle points
  | "STANDARD"  // 2 battle points
  | "LEADER"    // 3 battle points
  | "BRUISER"   // 4 battle points
  | "SOLO";     // 5 battle points
```

### CardType

```typescript
type CardType = "ANCESTRY" | "COMMUNITY" | "SUBCLASS" | "DOMAIN";
```

### FeatureType

```typescript
type FeatureType =
  | "HOPE"
  | "ANCESTRY"
  | "CLASS"
  | "COMMUNITY"
  | "DOMAIN"
  | "OTHER";
```

### SubclassLevel

```typescript
type SubclassLevel = "FOUNDATION" | "SPECIALIZATION" | "MASTERY";
```

### DomainCardType

```typescript
type DomainCardType =
  | "SPELL"
  | "GRIMOIRE"
  | "ABILITY"
  | "TRANSFORMATION"
  | "WILD";
```

### QuestionType

```typescript
type QuestionType = "BACKGROUND" | "CONNECTION";
```

---

## Angular Implementation Tips

### HTTP Interceptor for Auth

```typescript
@Injectable()
export class AuthInterceptor implements HttpInterceptor {
  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    const authReq = req.clone({ withCredentials: true });
    return next.handle(authReq);
  }
}
```

### Environment Configuration

```typescript
// environment.ts
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api'
};
```

### Generic Service Pattern

```typescript
@Injectable({ providedIn: 'root' })
export class ApiService<T, CreateDto, UpdateDto> {
  constructor(
    private http: HttpClient,
    private endpoint: string
  ) {}

  getAll(params?: HttpParams): Observable<PagedResponse<T>> {
    return this.http.get<PagedResponse<T>>(`${environment.apiUrl}/${this.endpoint}`, { params });
  }

  getById(id: number, expand?: string[]): Observable<T> {
    let params = new HttpParams();
    if (expand?.length) {
      params = params.set('expand', expand.join(','));
    }
    return this.http.get<T>(`${environment.apiUrl}/${this.endpoint}/${id}`, { params });
  }

  create(dto: CreateDto): Observable<T> {
    return this.http.post<T>(`${environment.apiUrl}/${this.endpoint}`, dto);
  }

  update(id: number, dto: UpdateDto): Observable<T> {
    return this.http.put<T>(`${environment.apiUrl}/${this.endpoint}/${id}`, dto);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/${this.endpoint}/${id}`);
  }
}
```

### Error Handling

```typescript
@Injectable({ providedIn: 'root' })
export class ErrorInterceptor implements HttpInterceptor {
  constructor(private router: Router, private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    return next.handle(req).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          this.authService.logout();
          this.router.navigate(['/login']);
        }
        return throwError(() => error);
      })
    );
  }
}
```
