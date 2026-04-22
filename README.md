# dayLog

`dayLog` is a daily planning and reflection web application that connects morning intention setting, evening review, and weekly progress tracking in one focused experience.

It is built as a multi-user Spring Boot product with:

- account and authentication data in MySQL
- daily writing stored as Markdown files on disk
- server-rendered pages designed for a polished, product-style workflow

## Highlights

- Morning planning with goals, focus areas, and anticipated challenges
- Evening reflection that reuses morning goals as a completion checklist
- Weekly review with completion counts and progress percentages
- Read-only daily log preview rendered from Markdown
- Per-user isolation for both account data and log file storage

## Product Flow

### Morning planning

- open a date
- write goals, focus areas, and likely challenges
- save structured sections back into the day's Markdown log

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
- MySQL
- H2 for the local development profile
- Gradle
- Executable JAR packaging

## Runtime Model

`dayLog` intentionally uses two storage layers.

### Account data

- stored in MySQL
- used for authentication, authorization, and account lookup

### Daily logs

- stored as Markdown files on disk
- separated by user account id
- grouped into month-local week buckets

Example path:

```text
logs/
└─ 15/
   └─ 2026_04_Week4/
      └─ 2026-04-23.md
```

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
├─ schema.sql
├─ static
└─ templates
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
- disables Thymeleaf template caching
- stores Markdown logs under `build/local-logs`

### Run tests

```powershell
.\gradlew.bat test --offline
```

### Build the executable JAR

```powershell
.\gradlew.bat bootJar --offline
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
| `DAY_LOG_LOGS_ROOT_PATH` | `logs` |
| `DAY_LOG_REMEMBER_ME_COOKIE_NAME` | `DAY_LOG_REMEMBER_ME` |
| `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS` | `1209600` |

The default profile is intentionally fail-fast. If a required runtime value is missing, the application should stop during startup instead of running in a partially configured state.

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

Before exposing the service to real users, replace every example credential and secret in `.env`.

## Security Notes

- static assets, login, and registration are public
- all application pages require authentication
- passwords are stored with BCrypt hashing
- remember-me uses `TokenBasedRememberMeServices`
- CSRF protection remains enabled
- session cookies are configured as HTTP only with `SameSite=Lax`

## Testing Snapshot

Current integration coverage focuses on the main product flows:

- registration
- login failure feedback
- morning log save behavior
- morning list rendering
- core page rendering for home, evening, and weekly views

Main test files:

- `src/test/java/com/potterlim/daylog/DayLogApplicationTests.java`
- `src/test/java/com/potterlim/daylog/WebFlowIntegrationTests.java`

## Deployment Notes

The primary production path is executable JAR deployment on a Linux VM with a reverse proxy in front. Docker Compose is also supported when packaging the app and MySQL together is more convenient.

For deployment details, use:

- [docs/deployment.md](docs/deployment.md)

For code structure and request flow details, use:

- [docs/project-architecture.md](docs/project-architecture.md)
