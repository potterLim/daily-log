# Deployment Guide

## Overview

`dayLog` is designed to run as a multi-user web application with:

- MySQL for account data and daily log content
- executable JAR deployment as the primary runtime model
- Actuator health endpoints for readiness and liveness checks
- rolling application logs and embedded Tomcat access logs
- provider-neutral webhook alerts for operational failures
- weekly operator summary logs in the production profile
- Markdown export and print-ready PDF report pages generated from database content
- Chrome-verified product UI surfaces for account, writing, library, preview, and export flows

The repository also includes Docker Compose support for environments where packaging the app, MySQL, and backup job together is preferred.

## Supported Deployment Models

### Option 1. Executable JAR on a VM

Recommended when:

- you are deploying to a Linux VM or server directly
- you want to manage the process with `systemd`
- you plan to place the app behind Nginx, Caddy, or a managed load balancer
- you want provider-neutral deployment that can later fit AWS, a VPS, or another compute host

### Option 2. Docker Compose

Recommended when:

- you want the app and MySQL defined together
- you prefer containerized deployment
- you want an easy local-to-server runtime match
- you want to use the included Compose backup profile

## Runtime Requirements

### Executable JAR Deployment

- Java 17 installed on the target machine
- reachable MySQL database
- reverse proxy or load balancer for HTTPS in internet-facing environments
- SMTP credentials when password recovery and verification mail should reach real users

### Docker Compose Deployment

- Docker Engine
- Docker Compose plugin
- persistent storage for MySQL data
- persistent storage for `ops/runtime` logs and backups

## Configuration Reference

### Required Environment Variables

| Variable | Description |
| --- | --- |
| `DATABASE_URL` | MySQL JDBC URL used by the app |
| `DATABASE_USERNAME` | MySQL account for the app |
| `DATABASE_PASSWORD` | MySQL password for the app |
| `DAY_LOG_REMEMBER_ME_KEY` | remember-me signing key |

The application is intentionally fail-fast in the default profile. Missing required values should stop startup immediately.

If SMTP values are not provided, verification and password reset requests still return safe user-facing responses, but no real email will be delivered. In the `local` and `test` profiles, the application logs generated verification and recovery links for validation instead.

### Optional Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | application HTTP port |
| `APP_PORT` | `8080` | Compose host port mapped to the app container |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `false` | marks the session cookie as secure when TLS is terminated before the app |
| `DAY_LOG_PASSWORD_RESET_TOKEN_VALIDITY_MINUTES` | `30` | password reset link lifetime in minutes |
| `DAY_LOG_EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES` | `1440` | email verification link lifetime in minutes |
| `DAY_LOG_MAIL_FROM_ADDRESS` | `no-reply@daylog.local` | sender address used for verification and password reset email |
| `DAY_LOG_ALERT_WEBHOOK_URL` | unset | webhook endpoint for operational failure alerts |
| `DAY_LOG_WEEKLY_SUMMARY_ENABLED` | `false` | enables scheduled weekly operator summary logging |
| `DAY_LOG_WEEKLY_SUMMARY_CRON` | `0 0 9 * * MON` | cron for the weekly operator summary job |
| `DAY_LOG_WEEKLY_SUMMARY_ZONE` | `Asia/Seoul` | time zone for the weekly operator summary job |
| `DAY_LOG_LOG_DIR` | `./logs` | application log output directory |
| `DAY_LOG_TOMCAT_BASE_DIR` | `./ops/runtime/tomcat` | base directory for embedded Tomcat access logs |
| `DAY_LOG_REMEMBER_ME_COOKIE_NAME` | `DAY_LOG_REMEMBER_ME` | remember-me cookie name |
| `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS` | `1209600` | remember-me lifetime in seconds |
| `DAY_LOG_PRODUCTION_READINESS_ENABLED` | `false` | turns on strict production fail-fast validation |
| `DAY_LOG_REQUIRE_SMTP` | `false` | requires SMTP configuration when production readiness validation is enabled |
| `DAY_LOG_REQUIRE_ALERT_WEBHOOK` | `false` | requires an alert webhook when production readiness validation is enabled |
| `DAY_LOG_REQUIRE_SECURE_SESSION_COOKIE` | `false` | requires secure session cookies when production readiness validation is enabled |
| `DAY_LOG_MINIMUM_REMEMBER_ME_KEY_LENGTH` | `32` | minimum allowed remember-me key length when production readiness validation is enabled |
| `DAY_LOG_BACKUP_RETENTION_DAYS` | `14` | backup retention for the Compose backup service |
| `DAY_LOG_BACKUP_NOTIFY_ON_SUCCESS` | `false` | whether successful Compose backups send webhook notifications |
| `DAY_LOG_BACKUP_VERIFY_TABLES` | `flyway_schema_history,user_account,daily_log_entry` | comma-separated table list verified by the Compose backup service |
| `SPRING_MAIL_HOST` | unset | SMTP host for verification and password reset delivery |
| `SPRING_MAIL_PORT` | provider default | SMTP port |
| `SPRING_MAIL_USERNAME` | unset | SMTP account username |
| `SPRING_MAIL_PASSWORD` | unset | SMTP account password |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | provider dependent | whether SMTP auth is enabled |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | provider dependent | whether STARTTLS is enabled |
| `SPRING_PROFILES_ACTIVE` | unset | active Spring profile such as `production` |

## Example JDBC URL

```text
jdbc:mysql://localhost:3306/daylog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

Adjust host, port, and database name for your own environment.

## Local Verification Before Deployment

Use the local profile when you want to validate application behavior without preparing MySQL first.

macOS or Linux:

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

Windows PowerShell:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Local profile behavior:

- H2 in-memory database
- MySQL compatibility mode
- Flyway migrations run on startup
- Thymeleaf template caching disabled
- diagnostic verification and recovery links in logs when SMTP is absent

For real MySQL-backed integration verification, the repository includes Testcontainers-based tests. Run them explicitly with:

macOS or Linux:

```bash
./gradlew mysqlIntegrationTest
```

Windows PowerShell:

```powershell
.\gradlew.bat mysqlIntegrationTest
```

## Build the Executable Artifact

macOS or Linux:

```bash
./gradlew test bootJar
```

Windows PowerShell:

```powershell
.\gradlew.bat test bootJar
```

Generated artifact:

```text
build/libs/dayLog.jar
```

## JAR Deployment Workflow

### 1. Build the artifact

```bash
./gradlew bootJar
```

### 2. Copy the JAR to the target server

Example target:

```text
/opt/day-log/dayLog.jar
```

### 3. Prepare runtime directories

Example:

```bash
sudo mkdir -p /opt/day-log
sudo mkdir -p /etc/day-log
sudo mkdir -p /var/log/day-log/app
sudo mkdir -p /var/log/day-log/tomcat
```

### 4. Create an environment file

Example file:

```text
/etc/day-log/day-log.env
```

Example contents:

```bash
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/daylog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DATABASE_USERNAME=daylog
DATABASE_PASSWORD=replace-this
DAY_LOG_REMEMBER_ME_KEY=replace-this-with-a-long-random-secret
DAY_LOG_PASSWORD_RESET_TOKEN_VALIDITY_MINUTES=30
DAY_LOG_EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES=1440
DAY_LOG_MAIL_FROM_ADDRESS=no-reply@example.com
DAY_LOG_ALERT_WEBHOOK_URL=https://example.com/alerts/day-log
DAY_LOG_WEEKLY_SUMMARY_ENABLED=true
DAY_LOG_PRODUCTION_READINESS_ENABLED=true
DAY_LOG_REQUIRE_SMTP=true
DAY_LOG_REQUIRE_ALERT_WEBHOOK=true
DAY_LOG_REQUIRE_SECURE_SESSION_COOKIE=true
DAY_LOG_LOG_DIR=/var/log/day-log/app
DAY_LOG_TOMCAT_BASE_DIR=/var/log/day-log/tomcat
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=mailer@example.com
SPRING_MAIL_PASSWORD=replace-this
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
PORT=8080
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
SPRING_PROFILES_ACTIVE=production
```

### 5. Start the application manually once

```bash
set -a
source /etc/day-log/day-log.env
set +a
java -jar /opt/day-log/dayLog.jar
```

Use this first run to verify:

- database connectivity
- Flyway migration success
- login and registration flow
- email verification links can be issued and consumed
- forgot-password requests can issue either verification or reset links as expected
- morning and evening records can be saved
- record library loads the selected range
- Markdown export downloads with expected content
- PDF export preview opens and browser "Save as PDF" works
- `/actuator/health/readiness` returns `UP`

## Example `systemd` Service

Example file:

```text
/etc/systemd/system/day-log.service
```

Example contents:

```ini
[Unit]
Description=dayLog application
After=network.target

[Service]
User=daylog
WorkingDirectory=/opt/day-log
EnvironmentFile=/etc/day-log/day-log.env
ExecStart=/usr/bin/java -jar /opt/day-log/dayLog.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

Then:

```bash
sudo systemctl daemon-reload
sudo systemctl enable day-log
sudo systemctl start day-log
sudo systemctl status day-log
```

## Health Endpoints

The application exposes:

- `/actuator/health`
- `/actuator/health/liveness`
- `/actuator/health/readiness`

Recommended usage:

- load balancer readiness check -> `/actuator/health/readiness`
- container or process liveness check -> `/actuator/health/liveness`
- manual smoke checks -> `/actuator/health`

## Reverse Proxy Recommendation

For internet-facing deployment, place the application behind a reverse proxy such as:

- Nginx
- Caddy
- a managed load balancer

Recommended responsibilities of the proxy layer:

- HTTPS termination
- HTTP to HTTPS redirect
- forwarding traffic to the application port
- access logs
- optional rate limiting or WAF integration

## Docker Compose Workflow

### 1. Copy the example environment file

macOS or Linux:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

### 2. Replace every example secret

Update:

- `SPRING_PROFILES_ACTIVE`
- `APP_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`
- `SERVER_SERVLET_SESSION_COOKIE_SECURE`
- `DAY_LOG_MAIL_FROM_ADDRESS`
- `DAY_LOG_ALERT_WEBHOOK_URL` when you want webhook alerts
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`
- `DAY_LOG_BACKUP_RETENTION_DAYS` when the default backup retention is not enough
- `DAY_LOG_BACKUP_NOTIFY_ON_SUCCESS` when successful backup notifications are required
- `DAY_LOG_BACKUP_VERIFY_TABLES` when schema-critical tables change

### 3. Start the stack

```bash
docker compose up -d --build
```

### 4. Verify the containers

```bash
docker compose ps
docker compose logs -f app
```

### 5. Confirm health

The Compose file waits for:

- MySQL health from `mysqladmin ping`
- application readiness from `/actuator/health/readiness`

Manual check:

```bash
curl -fsS http://127.0.0.1:${APP_PORT:-8080}/actuator/health/readiness
```

### 6. Run an on-demand backup

```bash
docker compose --profile ops run --rm backup
```

## Operational Notes

- Flyway manages schema changes.
- `ddl-auto=validate` keeps the entity model aligned with the migrated schema.
- The default server port is `8080` unless overridden by `PORT`.
- Compose exposes the app through `APP_PORT`, defaulting to `8080`.
- Graceful shutdown is enabled.
- HTTP session timeout is 30 minutes.
- Application logs roll under `DAY_LOG_LOG_DIR`.
- Embedded Tomcat access logs roll under `DAY_LOG_TOMCAT_BASE_DIR/logs`.
- Delivery failures in verification or recovery mail can emit webhook alerts through `DAY_LOG_ALERT_WEBHOOK_URL`.
- The production profile logs `WEEKLY_OPERATIONS_SUMMARY` once per week by default.
- Markdown export is generated directly from the selected library range.
- PDF export is a print-optimized HTML report intended for browser PDF saving.

## Backup Considerations

Production backup should cover:

- MySQL data
- application environment secrets stored outside Git
- generated operational logs when they are needed for audit or support

Because day logs live in MySQL now, there is no separate file storage requirement for user-written content or exports.

Repository-provided helpers:

- `ops/backup/mysql-backup.sh`
- `ops/backup/mysql-restore.sh`
- `ops/backup/day-log-backup.service`
- `ops/backup/day-log-backup.timer`

Recommended production pattern:

- run `mysql-backup.sh` from cron or a systemd timer
- write backups to persistent storage
- keep checksum files next to backup archives
- periodically rehearse `mysql-restore.sh` against a non-production database
- let backup failures emit alert webhooks through `DAY_LOG_ALERT_WEBHOOK_URL` when possible

## Suggested Post-Deploy Smoke Test

After deployment, confirm all of the following:

- `/actuator/health/readiness` returns `UP`
- home page loads after authentication
- registration creates a new account
- email verification link flow succeeds
- verification banner appears for unverified accounts and disappears after verification
- login failure shows expected generic feedback
- password reset mail delivery is configured when SMTP values are present
- password change works for an authenticated account
- morning plan can be saved
- blank morning or evening submissions do not create visible records
- evening reflection can be saved
- weekly review renders the intended Monday-Sunday range
- daily preview renders saved content and a clear empty state for blank dates
- record library searches by date range and keyword
- record library trend labels clearly show goal-completion rate over time
- Markdown export downloads selected records
- PDF export preview opens, uses readable daily cards, and can be saved from Chrome
- unknown routes render the product 404 page
- desktop and mobile layouts have no unintended horizontal overflow or awkward control wrapping
- public auth pages keep header actions aligned away from the brand block
- password recovery success copy remains generic, concise, and visually stable

## Recommended First Production Hardening Steps

- generate a strong `DAY_LOG_REMEMBER_ME_KEY`
- use a real MySQL password, never an example value
- place the app behind HTTPS
- set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` when TLS is terminated before the app
- configure real SMTP credentials before exposing password recovery to users
- configure a real alert webhook before relying on unattended mail delivery
- restrict direct database exposure
- enable scheduled MySQL backups
- watch readiness and liveness endpoints from your hosting platform
- keep final release screenshots, generated PDFs, and generated Markdown exports outside Git
