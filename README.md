# dayLog

`dayLog` is a focused daily planning and reflection application built with Spring Boot.
It helps users start the day with a clear plan, close the day with a structured reflection, and review weekly progress through a clean web experience.

## What dayLog does

- Create a morning plan for a selected date
- Reuse morning goals as an evening reflection checklist
- Track weekly completion progress across saved entries
- Render each day's Markdown log as a readable preview
- Separate user accounts in MySQL and daily logs on disk

## Product flow

### Morning planning

- open a date
- write goals, focus areas, and anticipated challenges
- save goals as a structured Markdown list

### Evening reflection

- reopen the same day's context
- check completed goals from the morning plan
- capture achievements, improvements, gratitude, and notes for tomorrow

### Weekly review

- calculate weekly goal totals and completion rates
- browse daily progress cards
- open any saved day in a read-only preview view

## Tech stack

- Java 17
- Spring Boot 3.5.9
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- MySQL
- H2 for local development
- Gradle
- Executable JAR packaging

## Project structure

```text
day-log
├─ build.gradle
├─ compose.yaml
├─ Dockerfile
├─ docs
├─ gradle
├─ src
│  ├─ main
│  │  ├─ java/com/potterlim/daylog
│  │  │  ├─ config
│  │  │  ├─ controller
│  │  │  ├─ dto
│  │  │  ├─ entity
│  │  │  ├─ repository
│  │  │  ├─ security
│  │  │  ├─ service
│  │  │  └─ support
│  │  └─ resources
│  │     ├─ static
│  │     ├─ templates
│  │     ├─ application.yml
│  │     ├─ application-local.yml
│  │     └─ schema.sql
│  └─ test
└─ settings.gradle
```

For additional tracked documentation, see:

- [Project Architecture](docs/project-architecture.md)
- [Deployment Guide](docs/deployment.md)

## Storage model

dayLog uses two storage layers with different responsibilities.

### Account data

- stored in MySQL
- managed through JPA
- used for authentication and authorization

### Daily logs

- stored as Markdown files on disk
- separated by user account id
- organized into date-based folders

Example log path:

```text
logs/
└─ 15/
   └─ 2026_04_Week4/
      └─ 2026-04-23.md
```

## Running locally

The recommended local workflow uses the `local` profile so you can start the application without preparing MySQL first.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Local profile behavior:

- uses in-memory H2 in MySQL compatibility mode
- disables Thymeleaf template caching
- stores Markdown logs under `build/local-logs`

## Environment variables

### Required in the default profile

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

The application is intentionally configured to fail fast if these values are missing in the default runtime profile.

### Optional

- `PORT`
- `DAY_LOG_LOGS_ROOT_PATH`
- `DAY_LOG_REMEMBER_ME_COOKIE_NAME`
- `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS`

## Build

### Run tests

```powershell
.\gradlew.bat test --offline
```

### Build executable JAR

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

## Docker Compose

This repository includes:

- `Dockerfile`
- `compose.yaml`
- `.env.example`

Typical workflow:

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Before exposing the service to real users, replace all example credentials and secrets in `.env`.

The Compose setup expects these main values:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

## Security notes

- static assets, login, and registration are public
- all application pages require authentication
- passwords are stored with BCrypt hashing
- remember-me is enabled through `TokenBasedRememberMeServices`
- CSRF protection remains enabled
- session cookies are configured as HTTP only with `SameSite=Lax`

## Testing

Integration coverage currently focuses on the main user flows:

- registration
- login failure feedback
- morning log save behavior
- morning list rendering
- core page rendering for home, evening, and weekly views

Main test files:

- `src/test/java/com/potterlim/daylog/DayLogApplicationTests.java`
- `src/test/java/com/potterlim/daylog/WebFlowIntegrationTests.java`

## Development notes

- The project is structured to open cleanly in IntelliJ IDEA.
- Java code follows the active project coding standard.
- Local-only working documents are intentionally excluded from Git through `.gitignore`.
