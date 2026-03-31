# Deployment Guide

## Overview

The application is ready to run as a multi-user web service backed by PostgreSQL. User account data is stored in the database, and each user's markdown logs are stored under a dedicated log root path separated by user account id.

## Runtime Model

- User accounts are stored in PostgreSQL.
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
2. Replace the default passwords and remember-me key with secure values.
3. Run `docker compose up -d --build`.
4. Expose the application through a reverse proxy or load balancer that terminates HTTPS.

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

- PostgreSQL data directory
- Application log storage directory referenced by `DAY_LOG_LOGS_ROOT_PATH`

## Local Verification

Use the local profile when PostgreSQL is not available:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

This profile is intended for development only.
