# dayLog

`dayLog` is a daily planning and reflection web application that turns morning intention setting, evening review, long-term record exploration, and exportable personal archives into one focused product flow.

It is built as a multi-user Spring Boot application with:

- MySQL-backed account and daily log storage
- Flyway-managed schema changes
- server-rendered Thymeleaf pages with a polished responsive product interface
- executable JAR deployment as the primary runtime model
- Docker Compose support for app, MySQL, and backup workflows

## Highlights

- Morning planning with goals, focus areas, and anticipated challenges
- Evening reflection that reuses morning goals as a completion checklist
- Weekly review with completion counts and progress percentages
- Record library for long-term exploration across date ranges and keywords
- Timeline-first library view with structured record previews, trend bars, and a compact calendar
- Markdown export for selected library ranges
- Print-ready PDF report preview designed for browser "Save as PDF" workflows
- Read-only daily log preview rendered from reconstructed Markdown
- Product-grade empty states and a custom 404 page instead of default error output
- Account creation with username, email address, and password
- Sign-in with either username or email address
- Email ownership verification, email-based password reset, and authenticated password change
- Per-user isolation at the database level
- Public health endpoints for runtime monitoring
- Rolling application logs, Tomcat access logs, and provider-neutral alert webhook support
- Weekly operator summary logs for registrations, active writers, and completion rates

## Product Flow

### Morning planning

- Open a date.
- Write goals, focus areas, and likely challenges.
- Save only meaningful content so blank submissions do not create phantom logs.

### Evening reflection

- Reopen the same date.
- Review the morning plan in read-only form.
- Mark completed goals and capture achievements, improvements, gratitude, and notes.

### Weekly review

- Scan the current Monday-Sunday week.
- Compare total goals against completed goals.
- Open any saved day in a detailed preview.

### Record library and export

- Explore recent or custom date ranges in a timeline.
- Narrow results with keyword search.
- Use trend bars and the calendar as secondary navigation cues.
- Export the selected range as Markdown.
- Open a print-optimized report and save it as PDF from the browser.

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

- `user_account` stores identity, password hashes, role, and account status.
- `user_account` also stores the unique recovery email address and email verification state.
- `user_email_verification_token` stores one-time email verification tokens.
- `user_password_reset_token` stores one-time password reset tokens.
- `daily_log_entry` stores one entry per user per date.
- Daily log sections are persisted as database text columns.
- Preview, library, Markdown export, and PDF report pages reconstruct output from stored sections instead of reading files from disk.

### Operational shape

- Executable JAR behind Nginx, Caddy, or a managed load balancer.
- MySQL as the persistent system of record.
- Actuator health endpoints for liveness and readiness checks.
- Rolling application logs and embedded Tomcat access logs.
- Optional webhook-based operational alerts for delivery failures.
- Weekly operator summary logs in the production profile.
- Backup scripts under `ops/backup`.

## Repository Guide

### Tracked documentation

- [docs/README.md](docs/README.md)
- [docs/project-architecture.md](docs/project-architecture.md)
- [docs/deployment.md](docs/deployment.md)
- [docs/release-readiness.md](docs/release-readiness.md)

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
├─ application-production.yml
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

Screenshot QA artifacts should also stay outside the repository, for example under a timestamped Desktop directory.

## Quick Start

### Run locally with the `local` profile

macOS or Linux:

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

The `local` profile:

- uses in-memory H2 in MySQL compatibility mode
- runs Flyway migrations on startup
- disables Thymeleaf template caching
- logs diagnostic verification and recovery links when SMTP is not configured

### Run tests

macOS or Linux:

```bash
./gradlew test
```

Windows PowerShell:

```powershell
.\gradlew.bat test
```

### Run real MySQL integration tests

Docker is required because these tests use Testcontainers.

macOS or Linux:

```bash
./gradlew mysqlIntegrationTest
```

Windows PowerShell:

```powershell
.\gradlew.bat mysqlIntegrationTest
```

### Build the executable JAR

macOS or Linux:

```bash
./gradlew bootJar
```

Windows PowerShell:

```powershell
.\gradlew.bat bootJar
```

Generated artifact:

```text
build/libs/dayLog.jar
```

Run it with:

```bash
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
| `APP_PORT` | `8080`, Compose host port only |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `false` |
| `DAY_LOG_PASSWORD_RESET_TOKEN_VALIDITY_MINUTES` | `30` |
| `DAY_LOG_EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES` | `1440` |
| `DAY_LOG_MAIL_FROM_ADDRESS` | `no-reply@daylog.local` |
| `DAY_LOG_ALERT_WEBHOOK_URL` | unset |
| `DAY_LOG_WEEKLY_SUMMARY_ENABLED` | `false` |
| `DAY_LOG_WEEKLY_SUMMARY_CRON` | `0 0 9 * * MON` |
| `DAY_LOG_WEEKLY_SUMMARY_ZONE` | `Asia/Seoul` |
| `DAY_LOG_LOG_DIR` | `./logs` |
| `DAY_LOG_TOMCAT_BASE_DIR` | `./ops/runtime/tomcat` |
| `DAY_LOG_REMEMBER_ME_COOKIE_NAME` | `DAY_LOG_REMEMBER_ME` |
| `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS` | `1209600` |
| `DAY_LOG_PRODUCTION_READINESS_ENABLED` | `false` |
| `DAY_LOG_REQUIRE_SMTP` | `false` |
| `DAY_LOG_REQUIRE_ALERT_WEBHOOK` | `false` |
| `DAY_LOG_REQUIRE_SECURE_SESSION_COOKIE` | `false` |
| `DAY_LOG_MINIMUM_REMEMBER_ME_KEY_LENGTH` | `32` |
| `DAY_LOG_BACKUP_RETENTION_DAYS` | `14`, Compose backup service only |
| `DAY_LOG_BACKUP_NOTIFY_ON_SUCCESS` | `false`, Compose backup service only |
| `DAY_LOG_BACKUP_VERIFY_TABLES` | `flyway_schema_history,user_account,daily_log_entry`, Compose backup service only |
| `SPRING_MAIL_HOST` | unset |
| `SPRING_MAIL_PORT` | provider default |
| `SPRING_MAIL_USERNAME` | unset |
| `SPRING_MAIL_PASSWORD` | unset |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | provider dependent |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | provider dependent |
| `SPRING_PROFILES_ACTIVE` | unset |

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
- weekly operator summary logs in the `WEEKLY_OPERATIONS_SUMMARY` format
- MySQL backup, restore, and host scheduler helpers under `ops/backup`

## Docker Compose

The repository includes:

- `Dockerfile`
- `compose.yaml`
- `.env.example`

Typical workflow:

macOS or Linux:

```bash
cp .env.example .env
docker compose up -d --build
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Main Compose values:

- `SPRING_PROFILES_ACTIVE`
- `APP_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`
- `SERVER_SERVLET_SESSION_COOKIE_SECURE`
- `DAY_LOG_MAIL_FROM_ADDRESS`
- `DAY_LOG_ALERT_WEBHOOK_URL`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `DAY_LOG_BACKUP_RETENTION_DAYS`
- `DAY_LOG_BACKUP_NOTIFY_ON_SUCCESS`
- `DAY_LOG_BACKUP_VERIFY_TABLES`

Before exposing the service to real users, replace every example credential and secret in `.env`.

For an on-demand backup from the Compose stack:

```bash
docker compose --profile ops run --rm backup
```

For unattended backups on a Linux host, the repository also includes:

- `ops/backup/day-log-backup.service`
- `ops/backup/day-log-backup.timer`

## Security Notes

- static assets, login, registration, recovery, verification, and health checks are public
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
- default whitelabel errors are disabled and product error pages are rendered for known not-found resource cases

## Testing Snapshot

Current integration coverage focuses on the main product flows:

- registration
- email verification
- password validation
- username or email login
- password reset request and token-based password reset
- authenticated password change
- generic login failure feedback
- morning log persistence and blank-save protection
- evening reflection persistence
- weekly Monday-Sunday range rendering
- read-only preview rendering and empty-section omission
- empty preview state for dates without saved content
- record library search, timeline, Markdown export, and PDF preview routing
- custom product 404 rendering
- core page rendering for home, evening, weekly, and library views
- public health endpoint availability

Main test files:

- `src/test/java/com/potterlim/daylog/DayLogApplicationTests.java`
- `src/test/java/com/potterlim/daylog/WebFlowIntegrationTests.java`
- `src/test/java/com/potterlim/daylog/MySqlIntegrationTests.java`
- `src/test/java/com/potterlim/daylog/WeeklyOperationsSummaryServiceTests.java`

## Deployment Notes

The primary production path is executable JAR deployment on a Linux VM behind a reverse proxy. Docker Compose is also supported when packaging the app and MySQL together is more convenient.

For deployment details, use:

- [docs/deployment.md](docs/deployment.md)

For code structure and request flow details, use:

- [docs/project-architecture.md](docs/project-architecture.md)

For release QA and screenshot expectations, use:

- [docs/release-readiness.md](docs/release-readiness.md)
