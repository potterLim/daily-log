# Deployment Guide

## Overview

The application is ready to run as a multi-user web service backed by MySQL. User account data is stored in the database, and each user's markdown logs are stored under a dedicated log root path separated by user account id.

## Runtime Model

- User accounts are stored in MySQL.
- Markdown daily logs are stored on disk.
- The log root path must be mapped to persistent storage in production.
- The remember-me key must be provided through an environment variable in production.

## Required Environment Variables

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

The application fails at startup when any of these required values are missing.

## Optional Environment Variables

- `PORT`
- `DAY_LOG_LOGS_ROOT_PATH`
- `DAY_LOG_REMEMBER_ME_COOKIE_NAME`
- `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS`

## Docker Compose Deployment

1. Copy `.env.example` to `.env`.
2. Replace the default MySQL passwords and remember-me key with secure values.
3. Run `docker compose up -d --build`.
4. Expose the application through a reverse proxy or load balancer that terminates HTTPS.

The Docker Compose `.env` file should provide:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

## Executable JAR Deployment

1. Build the JAR artifact with `.\gradlew.bat bootJar`.
2. Use the generated `build/libs/dayLog.jar` file.
3. Install Java 17 on the target server.
4. Provide the required environment variables before startup.
5. Start the application with `java -jar build/libs/dayLog.jar`.
6. Map persistent storage for the path referenced by `DAY_LOG_LOGS_ROOT_PATH`.

When you run the application as an executable JAR, embedded Tomcat starts automatically inside the Spring Boot process.

## Reverse Proxy Recommendation

For internet-facing deployment, place the application behind a reverse proxy such as Nginx, Caddy, or a managed load balancer.

Recommended proxy responsibilities:

- HTTPS termination
- HTTP to HTTPS redirect
- Request size limits
- Access logging
- Security headers

## Persistence

Production deployments should persist both of the following:

- MySQL data directory
- Application log storage directory referenced by `DAY_LOG_LOGS_ROOT_PATH`

## Local Verification

Use the local profile when MySQL is not available:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

This profile is intended for development only.
