# dayLog

`dayLog` is a daily planning and reflection web application that brings morning intention setting, evening review, and weekly progress tracking into one focused product flow.

It is built as a multi-user Spring Boot application with:

- MySQL-backed account and daily log storage
- Flyway-managed schema changes
- server-rendered pages designed for a polished product experience
- executable JAR deployment as the primary runtime model

## Highlights

- Morning planning with goals, focus areas, and anticipated challenges
- Evening reflection that reuses morning goals as a completion checklist
- Weekly review with completion counts and progress percentages
- Account creation with username, email address, and password
- Sign-in with either username or email address
- Email ownership verification, email-based password reset, and authenticated password change
- Read-only daily log preview rendered from reconstructed Markdown
- Per-user isolation at the database level
- Public health endpoints for runtime monitoring
- Rolling application logs, Tomcat access logs, and provider-neutral alert webhook support

## Product Flow

### Morning planning

- open a date
- write goals, focus areas, and likely challenges
- save the day's morning plan into the daily log entry

### Evening reflection

- reopen the same date
- review the morning plan in read-only form
- mark completed goals and capture achievements, improvements, gratitude, and notes

### Weekly review

- scan the current week's saved entries
- compare total goals against completed goals
- open any saved day in a detailed preview

## Technology Stack

- Java 17
- Spring Boot 3.5.9
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- Bean Validation
- Flyway
- MySQL
- H2 for the local development profile
- Gradle
- Executable JAR packaging

## Runtime Model

### Primary storage

- `user_account` stores identity, password hashes, role, and account status
- `user_account` also stores the unique recovery email address and email verification state
- `user_email_verification_token` stores one-time email verification tokens
- `user_password_reset_token` stores one-time password reset tokens
- `daily_log_entry` stores one entry per user per date
- the daily log sections are persisted as database text columns
- preview pages reconstruct Markdown from the stored sections instead of reading files from disk

### Operational shape

- executable JAR behind Nginx, Caddy, or a managed load balancer
- MySQL as the persistent system of record
- Actuator health endpoints for liveness and readiness checks
- rolling application logs and embedded Tomcat access logs
- optional webhook-based operational alerts for delivery failures
- backup scripts under `ops/backup`

## Repository Guide

### Tracked documentation

- [docs/README.md](docs/README.md)
- [docs/project-architecture.md](docs/project-architecture.md)
- [docs/deployment.md](docs/deployment.md)

### Main application areas

```text
src/main/java/com/potterlim/daylog
├─ config
├─ controller
├─ dto
├─ entity
├─ repository
├─ security
├─ service
└─ support
```

```text
src/main/resources
├─ application.yml
├─ application-local.yml
├─ db/migration
├─ static
└─ templates
```

```text
ops
└─ backup
```

### Local-only working documents

If your local workspace includes `local-docs/`, those files are intentionally ignored by Git and can be used for deeper private onboarding or working notes.

## Quick Start

### Run locally with the `local` profile

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

The `local` profile:

- uses in-memory H2 in MySQL compatibility mode
- runs Flyway migrations on startup
- disables Thymeleaf template caching

### Run tests

```powershell
.\gradlew.bat test
```

### Build the executable JAR

```powershell
.\gradlew.bat bootJar
```

Generated artifact:

```text
build/libs/dayLog.jar
```

Run it with:

```powershell
java -jar build/libs/dayLog.jar
```

## Configuration

### Required in the default profile

| Variable | Purpose |
| --- | --- |
| `DATABASE_URL` | MySQL JDBC URL |
| `DATABASE_USERNAME` | MySQL account name |
| `DATABASE_PASSWORD` | MySQL account password |
| `DAY_LOG_REMEMBER_ME_KEY` | remember-me signing key |

### Optional

| Variable | Default |
| --- | --- |
| `PORT` | `8080` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `false` |
| `DAY_LOG_PASSWORD_RESET_TOKEN_VALIDITY_MINUTES` | `30` |
| `DAY_LOG_EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES` | `1440` |
| `DAY_LOG_MAIL_FROM_ADDRESS` | `no-reply@daylog.local` |
| `DAY_LOG_ALERT_WEBHOOK_URL` | unset |
| `DAY_LOG_LOG_DIR` | `./logs` |
| `DAY_LOG_TOMCAT_BASE_DIR` | `./ops/runtime/tomcat` |
| `DAY_LOG_REMEMBER_ME_COOKIE_NAME` | `DAY_LOG_REMEMBER_ME` |
| `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS` | `1209600` |
| `SPRING_MAIL_HOST` | unset |
| `SPRING_MAIL_PORT` | provider default |
| `SPRING_MAIL_USERNAME` | unset |
| `SPRING_MAIL_PASSWORD` | unset |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | provider dependent |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | provider dependent |

The default profile is intentionally fail-fast. If a required runtime value is missing, the application should stop during startup instead of running in a partially configured state.

When SMTP is not configured, the application does not create a real mail sender. In the `local` and `test` flows, verification and recovery links are emitted through diagnostic logs so the account lifecycle can still be validated without external mail infrastructure.

## Health and Operations

The application exposes:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

These endpoints are public so that a reverse proxy, container platform, or load balancer can verify runtime state without authentication.

Runtime operations also include:

- rolling application logs written to `DAY_LOG_LOG_DIR`
- embedded Tomcat access logs written under `DAY_LOG_TOMCAT_BASE_DIR/logs`
- optional webhook alerts for critical mail delivery failures
- MySQL backup and restore scripts under `ops/backup`

## Docker Compose

The repository includes:

- `Dockerfile`
- `compose.yaml`
- `.env.example`

Typical workflow:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Main Compose values:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`
- `DAY_LOG_MAIL_FROM_ADDRESS`
- `DAY_LOG_ALERT_WEBHOOK_URL`

Before exposing the service to real users, replace every example credential and secret in `.env`.

For an on-demand backup from the Compose stack:

```powershell
docker compose --profile ops run --rm backup
```

## Security Notes

- static assets, login, registration, and health checks are public
- forgot-password and reset-password routes are also public
- all product pages require authentication
- passwords are stored with BCrypt hashing
- login accepts username or email address
- login failure uses a generic credential error message
- forgot-password also returns a generic success message
- newly registered accounts receive an email ownership verification link
- unverified accounts receive verification mail instead of a password reset mail
- password reset uses one-time hashed tokens with expiration
- email verification uses one-time hashed tokens with expiration
- remember-me uses `TokenBasedRememberMeServices`
- CSRF protection remains enabled
- session cookies are configured as HTTP only with `SameSite=Lax`

## Testing Snapshot

Current integration coverage focuses on the main product flows:

- registration
- email verification
- password validation
- username or email login
- password reset request and token-based password reset
- authenticated password change
- generic login failure feedback
- morning log persistence
- morning list rendering
- core page rendering for home, evening, and weekly views
- public health endpoint availability

Main test files:

- `src/test/java/com/potterlim/daylog/DayLogApplicationTests.java`
- `src/test/java/com/potterlim/daylog/WebFlowIntegrationTests.java`
- `src/test/java/com/potterlim/daylog/MySqlIntegrationTests.java`

## Deployment Notes

The primary production path is executable JAR deployment on a Linux VM behind a reverse proxy. Docker Compose is also supported when packaging the app and MySQL together is more convenient.

For deployment details, use:

- [docs/deployment.md](docs/deployment.md)

For code structure and request flow details, use:

- [docs/project-architecture.md](docs/project-architecture.md)
