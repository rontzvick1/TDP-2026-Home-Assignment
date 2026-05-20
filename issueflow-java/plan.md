# IssueFlow – Full Development Plan

> **Stack:** Java 21 · Spring Boot 3.4.2 · PostgreSQL · Spring Security + JWT · Spring Data JPA (Hibernate) · Apache Commons CSV · Lombok · H2 (test)
>
> **Schema strategy:** `ddl-auto: update` — Hibernate manages DDL automatically.
>
> **Package style:** Layer-based (`controller`, `service`, `repository`, `entity`, `dto`, `security`, `aop`, `exception`, `config`).

---

## Package Structure

```
com.att.tdp.issueflow
├── IssueFlowApplication.java
├── config/
│   ├── SecurityConfig.java          # Spring Security filter chain + RBAC rules
│   └── SchedulerConfig.java         # @EnableScheduling
├── controller/
│   ├── AuthController.java
│   ├── UserController.java
│   ├── ProjectController.java
│   ├── TicketController.java         # also handles CSV export/import
│   ├── CommentController.java
│   ├── AuditLogController.java
│   ├── DependencyController.java
│   ├── AttachmentController.java
│   └── WorkloadController.java
├── service/
│   ├── AuthService.java
│   ├── UserService.java
│   ├── ProjectService.java
│   ├── TicketService.java
│   ├── CommentService.java
│   ├── AuditLogService.java
│   ├── DependencyService.java
│   ├── AttachmentService.java
│   ├── CsvService.java
│   ├── MentionService.java
│   ├── AutoAssignmentService.java
│   └── EscalationScheduler.java
├── repository/
│   ├── UserRepository.java
│   ├── ProjectRepository.java
│   ├── TicketRepository.java
│   ├── CommentRepository.java
│   ├── AuditLogRepository.java
│   ├── DependencyRepository.java
│   └── AttachmentRepository.java
├── entity/
│   ├── User.java
│   ├── Project.java
│   ├── Ticket.java
│   ├── Comment.java
│   ├── AuditLog.java
│   ├── Dependency.java
│   └── Attachment.java
├── dto/
│   ├── request/
│   │   ├── LoginRequest.java
│   │   ├── CreateUserRequest.java
│   │   ├── UpdateUserRequest.java
│   │   ├── CreateProjectRequest.java
│   │   ├── UpdateProjectRequest.java
│   │   ├── CreateTicketRequest.java
│   │   ├── UpdateTicketRequest.java
│   │   ├── CreateCommentRequest.java
│   │   ├── UpdateCommentRequest.java
│   │   └── AddDependencyRequest.java
│   └── response/
│       ├── AuthResponse.java
│       ├── UserResponse.java
│       ├── ProjectResponse.java
│       ├── TicketResponse.java
│       ├── CommentResponse.java
│       ├── AuditLogResponse.java
│       ├── DependencyResponse.java
│       ├── AttachmentResponse.java
│       ├── WorkloadResponse.java
│       ├── MentionedUserResponse.java
│       ├── MentionPageResponse.java
│       └── CsvImportResultResponse.java
├── exception/
│   ├── NotFoundException.java
│   ├── ConflictException.java
│   ├── ValidationException.java
│   ├── ForbiddenException.java
│   └── GlobalExceptionHandler.java
├── security/
│   ├── JwtTokenProvider.java
│   ├── JwtAuthenticationFilter.java
│   └── UserDetailsServiceImpl.java
└── aop/
    ├── Auditable.java               # custom annotation
    └── AuditAspect.java             # intercepts @Auditable methods
```

---

## Phase 1 — Project Setup & Infrastructure

### Step 1.1 — Add Missing Dependencies to `pom.xml`

Add the following dependencies:

```xml
<!-- Spring Security -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>

<!-- JWT (jjwt 0.12.x) -->
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>

<!-- AOP for audit aspect -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-aop</artifactId>
</dependency>

<!-- Spring Security Test -->
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

### Step 1.2 — Update `application.yaml`

Add JWT configuration block and scheduling flag:

```yaml
app:
  jwt:
    secret: "your-256-bit-secret-key-here-must-be-long-enough"
    expiration-ms: 3600000   # 1 hour

spring:
  jpa:
    hibernate:
      ddl-auto: update
  scheduling:
    enabled: true
```

### Step 1.3 — Scaffold All Packages

Create all package directories under `com.att.tdp.issueflow`:
`config`, `controller`, `service`, `repository`, `entity`, `dto/request`, `dto/response`, `exception`, `security`, `aop`

### Step 1.4 — Global Exception Handling

**Files to create:**
- `exception/NotFoundException.java` → extends `RuntimeException`
- `exception/ConflictException.java` → extends `RuntimeException`
- `exception/ValidationException.java` → extends `RuntimeException`
- `exception/ForbiddenException.java` → extends `RuntimeException`
- `exception/GlobalExceptionHandler.java` → `@RestControllerAdvice`

Error response body format (returned for all errors):
```json
{
  "error": "NOT_FOUND",
  "message": "Ticket with id 42 not found",
  "timestamp": "2026-05-20T10:00:00Z"
}
```

HTTP status mapping:
| Exception | HTTP Status |
|---|---|
| `NotFoundException` | 404 Not Found |
| `ConflictException` | 409 Conflict |
| `ValidationException` | 400 Bad Request |
| `ForbiddenException` | 403 Forbidden |
| `MethodArgumentNotValidException` | 400 Bad Request (Bean Validation) |
| Any other `Exception` | 500 Internal Server Error |

---

## Phase 2 — Entity Design & JPA Mapping

All entities use `@Entity`, `@Table`, Lombok `@Data`/`@Builder`/`@NoArgsConstructor`/`@AllArgsConstructor`.  
Hibernate auto-creates/updates tables via `ddl-auto: update`.

### Step 2.1 — `User` Entity

```
Table: users
Columns:
  id          BIGSERIAL PRIMARY KEY
  username    VARCHAR UNIQUE NOT NULL
  email       VARCHAR UNIQUE NOT NULL
  full_name   VARCHAR NOT NULL
  role        VARCHAR NOT NULL   -- Enum: ADMIN, DEVELOPER
  password    VARCHAR NOT NULL   -- BCrypt hashed
  created_at  TIMESTAMP
```

Enum: `UserRole { ADMIN, DEVELOPER }`

### Step 2.2 — `Project` Entity

```
Table: projects
Columns:
  id          BIGSERIAL PRIMARY KEY
  name        VARCHAR NOT NULL
  description TEXT
  owner_id    BIGINT REFERENCES users(id)
  deleted_at  TIMESTAMP   -- NULL = active, non-NULL = soft-deleted
```

Repository must filter `deleted_at IS NULL` on all standard queries.

### Step 2.3 — `Ticket` Entity

```
Table: tickets
Columns:
  id           BIGSERIAL PRIMARY KEY
  title        VARCHAR NOT NULL
  description  TEXT
  status       VARCHAR NOT NULL   -- Enum: TODO, IN_PROGRESS, DONE, CANCELLED
  priority     VARCHAR NOT NULL   -- Enum: LOW, MEDIUM, HIGH, CRITICAL
  type         VARCHAR NOT NULL   -- Enum: BUG, FEATURE, TASK
  project_id   BIGINT REFERENCES projects(id)
  assignee_id  BIGINT REFERENCES users(id)  -- nullable
  due_date     TIMESTAMP WITH TIME ZONE      -- nullable
  deleted_at   TIMESTAMP                     -- soft delete
  created_at   TIMESTAMP
  updated_at   TIMESTAMP
```

Enums: `TicketStatus`, `TicketPriority`, `TicketType`

> `isOverdue` is **NOT** a DB column. It is computed in the service layer:
> `dueDate != null && dueDate.isBefore(now()) && status != DONE && status != CANCELLED`

### Step 2.4 — `Comment` Entity

```
Table: comments
Columns:
  id          BIGSERIAL PRIMARY KEY
  ticket_id   BIGINT REFERENCES tickets(id)
  author_id   BIGINT REFERENCES users(id)
  content     TEXT NOT NULL
  created_at  TIMESTAMP
  updated_at  TIMESTAMP

Join table: comment_mentions
Columns:
  comment_id  BIGINT REFERENCES comments(id)
  user_id     BIGINT REFERENCES users(id)
  PRIMARY KEY (comment_id, user_id)
```

JPA mapping: `@ManyToMany` on `Comment.mentionedUsers` with `@JoinTable(name="comment_mentions")`.

### Step 2.5 — `AuditLog` Entity

```
Table: audit_logs
Columns:
  id            BIGSERIAL PRIMARY KEY
  action        VARCHAR NOT NULL   -- Enum: CREATE, UPDATE, DELETE
  entity_type   VARCHAR NOT NULL   -- Enum: TICKET, PROJECT, COMMENT, USER, ATTACHMENT, DEPENDENCY
  entity_id     BIGINT
  performed_by  BIGINT REFERENCES users(id)  -- nullable (SYSTEM actions)
  actor         VARCHAR NOT NULL   -- Enum: USER, SYSTEM
  timestamp     TIMESTAMP NOT NULL
```

### Step 2.6 — `Dependency` Entity

```
Table: ticket_dependencies
Columns:
  id          BIGSERIAL PRIMARY KEY
  ticket_id   BIGINT REFERENCES tickets(id)  -- the blocked ticket
  blocked_by  BIGINT REFERENCES tickets(id)  -- the blocker
  UNIQUE (ticket_id, blocked_by)
```

### Step 2.7 — `Attachment` Entity

```
Table: attachments
Columns:
  id            BIGSERIAL PRIMARY KEY
  ticket_id     BIGINT REFERENCES tickets(id)
  filename      VARCHAR NOT NULL
  content_type  VARCHAR NOT NULL
  data          BYTEA NOT NULL          -- actual file binary
  uploaded_at   TIMESTAMP NOT NULL
```

---

## Phase 3 — Security & JWT Authentication

### Step 3.1 — `JwtTokenProvider`

Responsibilities:
- **Generate token:** on successful login, sign a JWT with `username` + `role` claims using HS256 + configured secret key. Set expiry to `app.jwt.expiration-ms`.
- **Validate token:** parse and verify signature; catch `JwtException` and return false.
- **Extract claims:** `getUsernameFromToken(token)`, `getRoleFromToken(token)`.

### Step 3.2 — `JwtAuthenticationFilter`

Extends `OncePerRequestFilter`:
1. Read `Authorization` header → extract `Bearer <token>`.
2. Call `JwtTokenProvider.validateToken(token)`.
3. If valid, load `UserDetails` via `UserDetailsServiceImpl`, set `UsernamePasswordAuthenticationToken` in `SecurityContextHolder`.
4. Call `filterChain.doFilter(...)`.

### Step 3.3 — `UserDetailsServiceImpl`

Implements `UserDetailsService`:
- `loadUserByUsername(username)` → queries `UserRepository.findByUsername()`.
- Maps `User.role` to Spring Security `GrantedAuthority` (e.g., `ROLE_ADMIN`, `ROLE_DEVELOPER`).

### Step 3.4 — `SecurityConfig`

```
@Configuration + @EnableWebSecurity + @EnableMethodSecurity
```

Rules:
- `SessionCreationPolicy.STATELESS`
- `POST /auth/login` → **permitAll**
- All other requests → **authenticated**
- Register `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- Register `BCryptPasswordEncoder` as a `@Bean`

Role-specific rules (enforced via `@PreAuthorize` on controller methods):
| Endpoint Pattern | Required Role |
|---|---|
| `GET /tickets/deleted`, `POST /tickets/:id/restore` | `ADMIN` |
| `GET /projects/deleted`, `POST /projects/:id/restore` | `ADMIN` |
| `DELETE /users/:id`, `POST /users/update/:id` | `ADMIN` |
| All other | any authenticated user |

### Step 3.5 — `AuthController` + `AuthService`

**`POST /auth/login`**
- Accept `{ username, password }`
- Load user → verify password with `BCryptPasswordEncoder.matches()`
- If valid → generate JWT → return `{ accessToken, tokenType: "Bearer", expiresIn: 3600 }`
- If invalid → throw `ValidationException` → 400

**`POST /auth/logout`**
- Stateless: return 200 OK (token is client-side discarded)
- Optional: maintain an in-memory revoked token set

**`GET /auth/me`**
- Extract username from `SecurityContextHolder`
- Return `UserResponse` for the authenticated user

---

## Phase 4 — Users API

### Files
- `controller/UserController.java`
- `service/UserService.java`
- `repository/UserRepository.java`
- `dto/request/CreateUserRequest.java`, `UpdateUserRequest.java`
- `dto/response/UserResponse.java`

### Endpoints

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| GET | `/users` | Any | List all users |
| GET | `/users/:userId` | Any | Get user by ID or 404 |
| POST | `/users` | Any | Create user (hash password, validate unique username + email) |
| POST | `/users/update/:userId` | ADMIN | Update `fullName` and/or `role` |
| DELETE | `/users/:userId` | ADMIN | Hard-delete user |

### Validation
- `username`: `@NotBlank`
- `email`: `@NotBlank` + `@Email`
- `fullName`: `@NotBlank`
- `role`: `@NotNull` (must be `ADMIN` or `DEVELOPER`)
- `password`: `@NotBlank` (min 6 chars recommended)
- Uniqueness check: if username or email already exists → `ConflictException` (409)

---

## Phase 5 — Projects API

### Files
- `controller/ProjectController.java`
- `service/ProjectService.java`
- `repository/ProjectRepository.java`
- `dto/request/CreateProjectRequest.java`, `UpdateProjectRequest.java`
- `dto/response/ProjectResponse.java`, `WorkloadResponse.java`

### Endpoints

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| GET | `/projects` | Any | List all non-deleted projects |
| GET | `/projects/:projectId` | Any | Get project by ID (non-deleted) or 404 |
| POST | `/projects` | Any | Create project (validate `ownerId` exists) |
| PATCH | `/projects/:projectId` | Any | Update name/description |
| DELETE | `/projects/:projectId` | Any | Soft-delete (set `deletedAt = now()`) |
| GET | `/projects/deleted` | ADMIN | List soft-deleted projects |
| POST | `/projects/:projectId/restore` | ADMIN | Restore (clear `deletedAt`) |
| GET | `/projects/:projectId/workload` | Any | Return open ticket counts per user in project |

### Workload Endpoint Logic
Query: for all tickets in project where `status NOT IN (DONE, CANCELLED)` and `deletedAt IS NULL`, group by `assigneeId` and count.
Return: `[{ userId, username, openTicketCount }]`

---

## Phase 6 — Tickets API (Core)

### Files
- `controller/TicketController.java`
- `service/TicketService.java`
- `service/AutoAssignmentService.java`
- `repository/TicketRepository.java`
- `dto/request/CreateTicketRequest.java`, `UpdateTicketRequest.java`
- `dto/response/TicketResponse.java` (includes `isOverdue` boolean)

### Endpoints

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| GET | `/tickets?projectId=:id` | Any | List non-deleted tickets for project (with `isOverdue`) |
| GET | `/tickets/:ticketId` | Any | Get ticket by ID or 404 (with `isOverdue`) |
| POST | `/tickets` | Any | Create ticket (auto-assign if no `assigneeId`) |
| PATCH | `/tickets/:ticketId` | Any | Partial update |
| DELETE | `/tickets/:ticketId` | Any | Soft-delete |
| GET | `/tickets/deleted?projectId=:id` | ADMIN | List soft-deleted tickets |
| POST | `/tickets/:ticketId/restore` | ADMIN | Restore soft-deleted ticket |

> **Routing Note:** `/tickets/deleted` must be registered **before** `/tickets/:ticketId` in the controller to avoid Spring treating `"deleted"` as a path variable.

### Auto-Assignment Logic (`AutoAssignmentService`)

Triggered when `assigneeId` is `null` on ticket creation:

```
1. Find all users WHERE role = 'DEVELOPER'
2. For each developer, count tickets WHERE assignee_id = developer.id
                                     AND status NOT IN ('DONE','CANCELLED')
                                     AND deleted_at IS NULL
3. Select developer with minimum count (tie-break: lowest id)
4. If no developers exist → leave assigneeId null
5. Set ticket.assigneeId = selected developer id
```

### `isOverdue` Computation (in `TicketService.toResponse()`)

```java
boolean isOverdue = ticket.getDueDate() != null
    && ticket.getDueDate().isBefore(OffsetDateTime.now())
    && ticket.getStatus() != TicketStatus.DONE
    && ticket.getStatus() != TicketStatus.CANCELLED;
```

### Validation
- `title`: `@NotBlank`
- `status`: must be valid `TicketStatus` enum value
- `priority`: must be valid `TicketPriority` enum value
- `type`: must be valid `TicketType` enum value
- `projectId`: required, project must exist and not be deleted
- `assigneeId`: optional; if provided, user must exist

---

## Phase 7 — Comments & Mentions API

### Files
- `controller/CommentController.java`
- `service/CommentService.java`
- `service/MentionService.java`
- `repository/CommentRepository.java`
- `dto/request/CreateCommentRequest.java`, `UpdateCommentRequest.java`
- `dto/response/CommentResponse.java`, `MentionedUserResponse.java`, `MentionPageResponse.java`

### Comments Endpoints

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| GET | `/tickets/:ticketId/comments` | Any | List all comments with `mentionedUsers` |
| POST | `/tickets/:ticketId/comments` | Any | Create comment, parse + persist mentions |
| PATCH | `/tickets/:ticketId/comments/:commentId` | Any | Update content, re-parse mentions |
| DELETE | `/tickets/:ticketId/comments/:commentId` | Any | Hard delete (cascades `comment_mentions` rows) |

### Mention Parsing Flow (`MentionService`)

```
1. Apply regex:  @([a-zA-Z0-9_]+)  to comment content
2. For each captured username:
   a. Call UserRepository.findByUsername(username)
   b. If found → add to mentionedUsers list
   c. If not found → silently ignore (no error)
3. Save/update comment_mentions join table entries
4. Return mentionedUsers in CommentResponse
```

### Mentions Read Endpoint (on UserController)

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| GET | `/users/:userId/mentions?page=&pageSize=` | Any | Paginated list of comments where user is mentioned |

- Default: `page=1`, `pageSize=10`
- Use Spring Data `Pageable`
- Response: `{ data: [...], total: N, page: P }`

---

## Phase 8 — Audit Log (AOP-based)

### Files
- `aop/Auditable.java` — custom annotation
- `aop/AuditAspect.java` — intercepts annotated methods
- `service/AuditLogService.java`
- `controller/AuditLogController.java`
- `repository/AuditLogRepository.java`
- `dto/response/AuditLogResponse.java`

### `@Auditable` Annotation

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    AuditAction action();      // CREATE, UPDATE, DELETE
    EntityType entityType();   // TICKET, PROJECT, COMMENT, USER, ATTACHMENT, DEPENDENCY
}
```

### `AuditAspect` (After-Returning Advice)

```
@Around / @AfterReturning on methods annotated with @Auditable:
1. Invoke the target method
2. Extract returned entity ID from result
3. Resolve authenticated user from SecurityContextHolder
   - If user found → actor = USER, performedBy = user.id
   - If no user (scheduler context) → actor = SYSTEM, performedBy = null
4. Save AuditLog entry
```

Apply `@Auditable` on service methods that mutate state:
- `UserService.createUser()` → `CREATE / USER`
- `UserService.deleteUser()` → `DELETE / USER`
- `ProjectService.createProject()` → `CREATE / PROJECT`
- `ProjectService.softDeleteProject()` → `DELETE / PROJECT`
- `TicketService.createTicket()` → `CREATE / TICKET`
- `TicketService.updateTicket()` → `UPDATE / TICKET`
- `TicketService.softDeleteTicket()` → `DELETE / TICKET`
- `CommentService.createComment()` → `CREATE / COMMENT`
- `CommentService.updateComment()` → `UPDATE / COMMENT`
- `CommentService.deleteComment()` → `DELETE / COMMENT`
- `AttachmentService.uploadAttachment()` → `CREATE / ATTACHMENT`
- `AttachmentService.deleteAttachment()` → `DELETE / ATTACHMENT`
- `DependencyService.addDependency()` → `CREATE / DEPENDENCY`
- `DependencyService.removeDependency()` → `DELETE / DEPENDENCY`
- `EscalationScheduler.escalate()` (each ticket update) → `UPDATE / TICKET` with `actor=SYSTEM`

### Audit Log Read API

| Method | Endpoint | Auth | Query Params |
|---|---|---|---|
| GET | `/audit-logs` | Any | `entityType`, `entityId`, `action`, `actor` (all optional) |

Filter using JPQL with optional parameters (or JPA Specifications).  
Return list of `AuditLogResponse`.

---

## Phase 9 — Ticket Dependencies API

### Files
- `controller/DependencyController.java`
- `service/DependencyService.java`
- `repository/DependencyRepository.java`
- `dto/request/AddDependencyRequest.java`
- `dto/response/DependencyResponse.java`

### Endpoints

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| POST | `/tickets/:ticketId/dependencies` | Any | Add `{ blockedBy: 42 }` (with cycle detection) |
| GET | `/tickets/:ticketId/dependencies` | Any | List all tickets that block this ticket |
| DELETE | `/tickets/:ticketId/dependencies/:blockerId` | Any | Remove dependency |

### Validation & DFS Cycle Detection

**Before inserting** a new dependency `(ticketId → blockedBy)`:

```
Step 1: if ticketId == blockedBy → ValidationException("A ticket cannot block itself")
Step 2: if dependency already exists → ConflictException("Dependency already exists")
Step 3: DFS from `blockedBy`, following existing `blockedBy` edges:
          visited = {}
          queue = [blockedBy]
          while queue not empty:
            current = queue.pop()
            if current == ticketId → ValidationException("Would create a circular dependency")
            if current not in visited:
              visited.add(current)
              queue.addAll(DependencyRepository.findBlockerIds(current))
Step 4: No cycle detected → save dependency
```

---

## Phase 10 — Attachments API

### Files
- `controller/AttachmentController.java`
- `service/AttachmentService.java`
- `repository/AttachmentRepository.java`
- `dto/response/AttachmentResponse.java`

### Endpoints

| Method | Endpoint | Auth | Action |
|---|---|---|---|
| POST | `/tickets/:ticketId/attachments` | Any | Upload file (multipart/form-data). Store binary in DB. |
| DELETE | `/tickets/:ticketId/attachments/:attachmentId` | Any | Delete attachment |

### Upload Logic
1. Validate ticket exists and is not deleted → 404 if not
2. Read `MultipartFile`: extract `originalFilename`, `contentType`, `bytes`
3. Save `Attachment` entity with `data = file.getBytes()`
4. Return `AttachmentResponse`: `{ id, ticketId, filename, contentType }` — **no binary in response**

Max file size: 10MB (already configured in `application.yaml`).

---

## Phase 11 — CSV Export / Import API

### Files
- `service/CsvService.java`
- Endpoints added to `TicketController.java`

### Export — `GET /tickets/export?projectId=:id`

```
1. Query non-deleted tickets for projectId
2. Build CSV using Apache Commons CSV:
   Headers: id, title, description, status, priority, type, assigneeId
3. Set response headers:
   Content-Type: text/csv
   Content-Disposition: attachment; filename="tickets-{projectId}.csv"
4. Write CSV bytes to HttpServletResponse output stream
```

### Import — `POST /tickets/import` (multipart: `file` + `projectId`)

```
1. Parse projectId form field → validate project exists
2. Parse CSV with Commons CSV (withFirstRecordAsHeader)
3. For each row:
   a. Map columns to CreateTicketRequest
   b. Validate required fields (title, status, priority, type)
   c. Call TicketService.createTicket() (triggers auto-assignment if no assigneeId)
   d. On success → increment createdCount
   e. On any exception → increment failedCount, record error message with row number
4. Return: { "created": N, "failed": M, "errors": ["Row 3: title is blank", ...] }
```

---

## Phase 12 — Auto-Escalation Scheduler

### Files
- `config/SchedulerConfig.java` — `@EnableScheduling`
- `service/EscalationScheduler.java` — `@Component` + `@Scheduled`

### Priority Escalation Ladder

```
LOW → MEDIUM → HIGH → CRITICAL  (CRITICAL stays CRITICAL)
```

### Scheduler Logic

Runs every **5 minutes**: `@Scheduled(fixedRate = 300_000)`

```
1. Query all tickets WHERE:
   - deleted_at IS NULL
   - status NOT IN ('DONE', 'CANCELLED')
   - due_date IS NOT NULL
   - due_date < NOW()
   - priority != 'CRITICAL'
2. For each ticket:
   a. Set priority = nextPriority(current)
   b. Save ticket
   c. Write AuditLog { action=UPDATE, entityType=TICKET, entityId=ticket.id,
                        performedBy=null, actor=SYSTEM }
3. Log: "Escalated N tickets due to overdue dueDate"
```

Note: `isOverdue` is a computed field — it is recalculated in every ticket response based on current time, independent of the scheduler.

---

## Phase 13 — Tests

**Test setup:**
- `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)`
- `MockMvc` (or `TestRestTemplate`) for HTTP layer
- H2 in-memory database (already in `pom.xml` with `scope=test`)
- `application-test.yaml` for test-specific config (`ddl-auto: create-drop`, H2 URL)
- `@WithMockUser` or real JWT tokens for auth tests

### Test Classes

| Test Class | Key Scenarios |
|---|---|
| `AuthControllerTest` | Login success, wrong password → 400, get /auth/me |
| `UserControllerTest` | Create user, get all, get by id, update (ADMIN only), delete (ADMIN only), duplicate username → 409 |
| `ProjectControllerTest` | Create, get, update, soft-delete, restore (ADMIN), list deleted (ADMIN), workload |
| `TicketControllerTest` | Create (with/without assignee), get, update, soft-delete, restore, isOverdue computation |
| `CommentControllerTest` | Create with `@mention`, get comments (verify mentionedUsers), update, delete |
| `MentionControllerTest` | GET /users/:id/mentions with pagination |
| `AuditLogControllerTest` | Create ticket → verify audit log entry exists, filter by entityType/action |
| `DependencyControllerTest` | Add dependency, list, remove, self-reference → 400, circular → 400 |
| `AttachmentControllerTest` | Upload file, delete, ticket not found → 404 |
| `CsvControllerTest` | Export → verify CSV headers + row count; Import valid CSV; Import with bad rows → partial results |
| `EscalationSchedulerTest` | Create overdue ticket with LOW priority, invoke scheduler directly, verify priority = MEDIUM |
| `AutoAssignmentTest` | Create ticket without assigneeId, verify DEVELOPER with fewest tickets was assigned |

---

## Phase 14 — Documentation Files

### `run.md` (required by README)

Document the exact steps to:
1. Install prerequisites: Java 21+, Maven, Docker Desktop
2. Start the database: `docker compose up -d`
3. Build the project: `./mvnw clean package -DskipTests`
4. Run the application: `./mvnw spring-boot:run`
5. Run all tests: `./mvnw test`
6. Access the API: `http://localhost:8080`

### `prompts.md` (required by README)

Document all AI prompts and agent interactions used during development, including:
- The /grill-me design interview transcript
- Key prompts used during code generation
- Any debugging or refinement prompts

---

## Implementation Order & Dependencies

```
Phase 1 (Setup)
    └─► Phase 2 (Entities)
            └─► Phase 3 (Security + Auth)   ← must complete before testing any secured endpoint
                    └─► Phase 4 (Users API)
                    └─► Phase 5 (Projects API)
                    └─► Phase 6 (Tickets + Auto-Assign)
                            └─► Phase 7 (Comments + Mentions)
                            └─► Phase 8 (Audit Log + AOP)
                            └─► Phase 9 (Dependencies)
                            └─► Phase 10 (Attachments)
                            └─► Phase 11 (CSV Export/Import)
                            └─► Phase 12 (Escalation Scheduler)
                    └─► Phase 13 (Integration Tests)
                    └─► Phase 14 (Docs)
```

---

## Key Design Constraints & Notes

1. **Soft Delete pattern:** All standard repository methods for `Ticket` and `Project` must append `WHERE deleted_at IS NULL`. Use custom `@Query` annotations or Spring Data derived method names.

2. **`/tickets/deleted` route ordering:** In `TicketController`, declare `@GetMapping("/deleted")` **before** `@GetMapping("/{ticketId}")` to avoid path variable collision.

3. **`/projects/deleted` route ordering:** Same pattern applies in `ProjectController`.

4. **Attachment response:** Never return the binary `data` field in `AttachmentResponse`. Only return `id`, `ticketId`, `filename`, `contentType`.

5. **Audit log actor:** The `AuditAspect` must distinguish between request context (has `Authentication` in `SecurityContext`) and scheduler context (no `Authentication`) to set `actor = USER` vs `actor = SYSTEM`.

6. **CSV import atomicity:** Import is **not** transactional at the file level — each row is attempted independently. Partial success (some rows created, some failed) is the expected behaviour.

7. **DFS cycle check:** Load all dependency edges lazily from the DB during DFS. For large graphs this could be slow — acceptable for a homework scope.

8. **Password in requests:** `CreateUserRequest` includes a `password` field. It must **never** be returned in any response DTO.

9. **Test isolation:** Each test class uses `@Transactional` (rollback) or `@BeforeEach` cleanup to ensure a clean DB state.

10. **H2 compatibility:** Hibernate will generate compatible DDL for H2 in tests because `ddl-auto: create-drop` is used in the test profile. No manual SQL scripts needed.
