# dayLog

`dayLog` is a web application for writing daily plans, reflections, and weekly progress summaries.

## Goals

- Provide a clean and focused daily logging experience.
- Keep the project structure practical for long-term maintenance.
- Follow `java-coding-standard.md` rules strictly during implementation.

## Stack

- Java 17
- Spring Boot 3.5.9
- Spring MVC + Thymeleaf
- Spring Security
- Spring Data JPA
- PostgreSQL
- Gradle

## Notes

- Authentication data will live in PostgreSQL.
- Daily log documents will remain file-based under the `logs` directory.
- This project is intentionally structured as a standard Gradle project so it opens naturally in IntelliJ IDEA.

## Run Locally

Use the local profile when you want to run the application without preparing PostgreSQL first.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

The local profile uses an in-memory H2 database and stores markdown logs under `build/local-logs`.

## Production Deployment

The default configuration is intended for PostgreSQL-backed deployment.

Required environment variables:

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

The application intentionally fails fast at startup when these required production values are missing.

Optional environment variables:

- `PORT`
- `DAY_LOG_LOGS_ROOT_PATH`
- `DAY_LOG_REMEMBER_ME_COOKIE_NAME`
- `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS`

## Docker Compose

The repository includes `Dockerfile`, `compose.yaml`, and `.env.example` for container-based deployment.

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Replace the example secrets in `.env` before exposing the service to real users.

The application persists user accounts in PostgreSQL and markdown logs in the mounted `daylog-logs` volume.
