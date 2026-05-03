# ConnectSphere

A social media backend built with Spring Boot Microservices. Users can register, post content, follow each other, react to posts, comment, upload media, get notifications, and search by hashtag — all routed through a single API Gateway with JWT authentication.

This isn't a tutorial project. Every service has its own database, its own failure boundary, and communicates over either REST (Feign) or a message broker (RabbitMQ). The goal was to build something that reflects how these systems actually work at scale, not just how they look in architecture diagrams.

---

## Services

| Service | Port | Database | What it does |
|---|---|---|---|
| `service-registry` | 8761 | — | Eureka server. Every other service registers here on startup |
| `api-gateway` | 8080 | — | Single entry point. Validates JWTs, injects identity headers, routes traffic |
| `auth-service` | 8081 | `cs_auth_db` | Registration with OTP, login, JWT + refresh tokens, OAuth2 (Google/GitHub), profile management |
| `post-service` | 8082 | `cs_post_db` | Post CRUD, visibility controls (PUBLIC / PRIVATE), paginated feeds |
| `comment-service` | 8083 | `cs_comment_db` | Comments on posts. Verifies the post actually exists via Feign before saving |
| `like-service` | 8084 | `cs_like_db` | Emoji reactions (LIKE, LOVE, HAHA, WOW, SAD, ANGRY) on posts and comments |
| `follow-service` | 8085 | `cs_follow_db` | Follow graph. Blocks self-follows and duplicate follows at the service layer |
| `notification-service` | 8086 | `cs_notification_db` | Listens to RabbitMQ events, persists notifications, sends emails via Spring Mail |
| `media-service` | 8087 | `cs_media_db` | File uploads (up to 50MB), stories with 24-hour auto-expiry via a scheduled job |
| `search-service` | 8088 | `cs_search_db` | Hashtag indexing and trending, cross-service user/post search via Feign |
| `connectsphere-web` | 8090 | — | Thymeleaf frontend. Calls the backend through the gateway |

---

## How Authentication Works

Every request goes through the gateway first. The `GlobalAuthFilter` runs before routing and does the following:

1. Checks if the path is in the public routes list (login, register, public feed, trending hashtags, etc.) — if so, it passes the request through untouched.
2. For everything else, it reads the `Authorization: Bearer <token>` header, validates the JWT signature and expiry, extracts `userId`, `role`, and `username` from the claims.
3. Mutates the request to inject `X-User-Id`, `X-User-Role`, and `X-User-Username` headers downstream.

Downstream services don't touch the JWT at all. They just read the injected headers. This means the JWT secret only lives in two places: `auth-service` (issues tokens) and `api-gateway` (validates them). Both must use the same `JWT_SECRET` environment variable — if they don't, every authenticated request fails.

Access tokens expire in 24 hours. Refresh tokens are stored in the database and expire in 7 days. Logout deletes the refresh token from the database.

### OAuth2 (Google / GitHub)

The flow redirects through the gateway to `auth-service`. Spring Security handles the OAuth2 handshake, `CustomOAuth2UserService` does a DB upsert (creates the user on first login, updates profile info on subsequent ones), and `OAuth2AuthenticationSuccessHandler` generates a JWT and redirects to the frontend URL with `?token=...&userId=...` as query params. OAuth2 users have `passwordHash = null` in the database — trying to log in with a password returns an error: *"Please log in using GOOGLE OAuth."*

---

## Architecture Decisions Worth Knowing

**Why RabbitMQ for notifications instead of a direct HTTP call?**
When a user registers and requests an OTP, the last thing we want is `auth-service` waiting for an email to deliver before it can respond. `auth-service` publishes an `OtpEmailEvent` to the `connectsphere.events` Topic Exchange with routing key `notification.otp`. `notification-service` picks it up asynchronously. If the email server is slow or down, the auth flow is completely unaffected.

The notification queue has a 30-minute TTL (`x-message-ttl: 1_800_000`). There's a Dead Letter Queue for messages that fail after 3 retry attempts (with exponential backoff: 2s → 4s → 8s). The listener container is configured with `missingQueuesFatal = false`, so `notification-service` doesn't crash on startup if RabbitMQ isn't ready yet.

**Why does comment-service call post-service over HTTP?**
Before saving a comment, `comment-service` needs to verify the target post actually exists. This is a synchronous check — the result matters before we proceed. OpenFeign turns this into a clean Java interface call; Spring Cloud LoadBalancer resolves `lb://post-service` to the actual instance via Eureka.

**Why separate databases?**
`cs_auth_db`, `cs_post_db`, `cs_comment_db`, etc. are completely independent. A schema change in `post-service` doesn't require any coordination with `comment-service`. A slow query in `cs_notification_db` doesn't affect login. The cost is you can't do cross-service JOINs — but that's the point.

**Story expiry**
`media-service` has a `StoryExpiryScheduler` that runs every 5 minutes via a cron job (`@Scheduled`). It issues a bulk JPQL UPDATE to mark all stories older than 24 hours as expired. The cron expression is configurable via `app.story.cleanup-cron` without redeployment.

---

## Tech Stack

- **Java 21**, **Spring Boot 3**
- **Spring Cloud Gateway** — reactive (WebFlux), handles routing and auth filter
- **Netflix Eureka** — service registry and discovery
- **OpenFeign** — declarative HTTP clients for inter-service calls
- **Spring Security** — JWT filter chain in `auth-service`, permit-all in downstream services
- **Spring OAuth2 Client** — Google and GitHub login
- **Spring Data JPA + MySQL** — persistence in all services
- **RabbitMQ + Spring AMQP** — async event bus between `auth-service` and `notification-service`
- **Redis** — OTP storage with TTL in `auth-service`
- **Spring Mail** — email delivery in `notification-service`
- **Springdoc OpenAPI** — Swagger UI on `/swagger-ui.html` in every service
- **Spring Actuator** — `/health`, `/info`, `/metrics` in every service; `/gateway/routes` in the gateway
- **Lombok** — `@Slf4j`, `@Builder`, `@RequiredArgsConstructor` throughout
- **JUnit 5 + Mockito** — unit tests in `auth-service`
- **Thymeleaf** — server-side rendering in `connectsphere-web`

---

## Running Locally

### Prerequisites

- Java 21
- Maven 3.8+
- MySQL 8
- RabbitMQ
- Redis

### Environment Variables

These must be set before starting `auth-service` and `api-gateway`:

```bash
JWT_SECRET=your-secret-here-must-be-same-in-both-services
DATABASE_PASSWORD=your-mysql-password

# OAuth2 — only needed if you want Google/GitHub login
GOOGLE_CLIENT_ID=...
GOOGLE_CLIENT_SECRET=...
GITHUB_CLIENT_ID=...
GITHUB_CLIENT_SECRET=...
REDIRECT_URI=http://localhost:8090/oauth2/callback
```

> **Both `auth-service` and `api-gateway` must use the same `JWT_SECRET`.** This is the most common misconfiguration — tokens issued by `auth-service` will fail validation at the gateway if the secrets don't match.

### Startup Order

Services need to start in this order because they register with Eureka:

```
1. service-registry    (wait for it to be fully up)
2. auth-service
3. post-service
4. comment-service
5. like-service
6. follow-service
7. notification-service
8. media-service
9. search-service
10. api-gateway        (start last — needs services registered in Eureka)
11. connectsphere-web  (optional frontend)
```

### Starting Each Service

```bash
cd service-registry && mvn spring-boot:run
cd auth-service      && mvn spring-boot:run
# ... and so on for each service
```

Each service creates its own database automatically (`createDatabaseIfNotExist=true` in the datasource URL). You don't need to create the databases manually.

---

## API Reference

All requests go through the gateway on port `8080`. Authenticated endpoints require `Authorization: Bearer <token>`.

### Auth — `/api/v1/auth`

```
POST   /register            Send OTP to email (first step of registration)
POST   /verify-otp          Verify OTP and complete registration
POST   /login               Login with email or username + password
POST   /refresh             Get a new access token using a refresh token
POST   /logout              Invalidate refresh token  [auth required]
GET    /profile             Get your own profile      [auth required]
GET    /profile/{userId}    Get any user's public profile
PUT    /profile             Update your profile       [auth required]
PUT    /password            Change password           [auth required]
DELETE /deactivate          Deactivate your account   [auth required]
GET    /search?query=...    Search users by name or username
```

Admin endpoints (role `ADMIN` required):
```
GET    /admin/users
PUT    /admin/users/{id}/suspend
PUT    /admin/users/{id}/reactivate
DELETE /admin/users/{id}
```

### Posts — `/api/v1/posts`

```
POST   /                    Create post               [auth required]
GET    /public              Public feed (no auth)
GET    /{id}                Get a single post
PUT    /{id}                Update post               [auth required, must be author]
DELETE /{id}                Delete post               [auth required, must be author]
GET    /search?q=...        Search posts
GET    /count/{userId}      Post count for a user
```

### Comments — `/api/v1/comments`

```
POST   /                    Add a comment             [auth required]
GET    /post/{postId}       Get comments for a post   (public)
PUT    /{id}                Edit a comment            [auth required, must be author]
DELETE /{id}                Delete a comment          [auth required, must be author]
```

### Likes — `/api/v1/likes`

```
POST   /react               React to a post or comment  [auth required]
DELETE /unreact             Remove your reaction          [auth required]
GET    /summary             Reaction counts by type     (public)
GET    /count               Total reaction count        (public)
GET    /target              All reactions on a target   (public)
```

Reaction types: `LIKE`, `LOVE`, `HAHA`, `WOW`, `SAD`, `ANGRY`

### Follows — `/api/v1/follows`

```
POST   /{userId}            Follow a user             [auth required]
DELETE /{userId}            Unfollow a user           [auth required]
GET    /{userId}/followers  Follower list             (public)
GET    /{userId}/following  Following list            (public)
GET    /{userId}/counts     Follower + following counts (public)
```

### Notifications — `/api/v1/notifications`

```
GET    /                    Get your notifications    [auth required]
PUT    /{id}/read           Mark as read              [auth required]
PUT    /read-all            Mark all as read          [auth required]
```

### Media — `/api/v1/media`, `/api/v1/stories`

```
POST   /media/upload        Upload a file (max 50MB)  [auth required]
GET    /media/{id}          Get media metadata
DELETE /media/{id}          Delete media              [auth required]
GET    /files/{filename}    Serve a file              (public)

POST   /stories             Create a story            [auth required]
GET    /stories/user/{id}   Get active stories for a user (public)
DELETE /stories/{id}        Delete a story            [auth required]
```

### Search — `/api/v1/search`, `/api/v1/hashtags`

```
GET    /search/posts?q=...        Search posts by content
GET    /search/users?q=...        Search users
GET    /search/hashtags?q=...     Hashtag autocomplete  (public)
GET    /hashtags/trending         Trending hashtags     (public)
GET    /hashtags/{tag}/posts      Posts tagged with #tag
```

---

## Swagger UI

Every service exposes Swagger UI at `http://localhost:{port}/swagger-ui.html`. Useful when debugging a specific service directly:

- Auth: http://localhost:8081/swagger-ui.html
- Post: http://localhost:8082/swagger-ui.html
- Comment: http://localhost:8083/swagger-ui.html
- Like: http://localhost:8084/swagger-ui.html
- Follow: http://localhost:8085/swagger-ui.html
- Notification: http://localhost:8086/swagger-ui.html
- Media: http://localhost:8087/swagger-ui.html
- Search: http://localhost:8088/swagger-ui.html

---

## Project Structure

```
ConnectSphere/
├── service-registry/       Eureka server
├── api-gateway/            Spring Cloud Gateway + auth filter
├── auth-service/           Identity, OAuth2, OTP, JWT
├── post-service/           Posts and feeds
├── comment-service/        Comments (with Feign → post-service)
├── like-service/           Reactions (with Feign → post + comment service)
├── follow-service/         Follow graph
├── notification-service/   RabbitMQ consumer + email
├── media-service/          File storage + stories
├── search-service/         Hashtag indexing + search (Feign → auth + post)
└── connectsphere-web/      Thymeleaf frontend
```

Each service follows the same internal structure:

```
src/main/java/com/connectsphere/{service}/
├── config/         Spring beans, security config, Feign config
├── controller/     REST endpoints
├── service/        Business logic interfaces and implementations
├── repository/     Spring Data JPA repositories
├── entity/         JPA entities
├── dto/            Request and response objects
├── exception/      Custom exceptions + GlobalExceptionHandler
└── enums/          Domain enums
```

---

## License

MIT
