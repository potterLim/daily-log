# Deployment Guide

## Overview

`dayLog` is designed to run as a multi-user web application with:

- MySQL for account data and daily log content
- executable JAR deployment as the primary runtime model
- Actuator health endpoints for readiness and liveness checks

The repository also includes Docker Compose support for environments where packaging the app and MySQL together is preferred.

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

## Runtime Requirements

### Executable JAR Deployment

- Java 17 installed on the target machine
- reachable MySQL database
- reverse proxy or load balancer for HTTPS in internet-facing environments

### Docker Compose Deployment

- Docker Engine
- Docker Compose plugin
- persistent storage for MySQL data

## Configuration Reference

### Required Environment Variables

| Variable | Description |
| --- | --- |
| `DATABASE_URL` | MySQL JDBC URL used by the app |
| `DATABASE_USERNAME` | MySQL account for the app |
| `DATABASE_PASSWORD` | MySQL password for the app |
| `DAY_LOG_REMEMBER_ME_KEY` | remember-me signing key |

The application is intentionally fail-fast in the default profile. Missing required values should stop startup immediately.

### Optional Environment Variables

| Variable | Default | Description |
| --- | --- | --- |
| `PORT` | `8080` | application HTTP port |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `false` | marks the session cookie as secure when TLS is terminated before the app |
| `DAY_LOG_REMEMBER_ME_COOKIE_NAME` | `DAY_LOG_REMEMBER_ME` | remember-me cookie name |
| `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS` | `1209600` | remember-me lifetime in seconds |

## Example JDBC URL

```text
jdbc:mysql://localhost:3306/daylog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

Adjust host, port, and database name for your own environment.

## Local Verification Before Deployment

Use the local profile when you want to validate application behavior without preparing MySQL first.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Local profile behavior:

- H2 in-memory database
- MySQL compatibility mode
- Flyway migrations run on startup

## Build the Executable Artifact

```powershell
.\gradlew.bat test bootJar --offline
```

Generated artifact:

```text
build/libs/dayLog.jar
```

## JAR Deployment Workflow

### 1. Build the artifact

```powershell
.\gradlew.bat bootJar --offline
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
PORT=8080
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
```

### 5. Start the application manually once

```bash
source /etc/day-log/day-log.env
java -jar /opt/day-log/dayLog.jar
```

Use this first run to verify:

- database connectivity
- Flyway migration success
- login and registration flow
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

```powershell
Copy-Item .env.example .env
```

### 2. Replace every example secret

Update:

- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

### 3. Start the stack

```powershell
docker compose up -d --build
```

### 4. Verify the containers

```powershell
docker compose ps
docker compose logs -f app
```

### 5. Confirm health

The Compose file waits for:

- MySQL health from `mysqladmin ping`
- application readiness from `/actuator/health/readiness`

## Operational Notes

- Flyway manages schema changes
- `ddl-auto=validate` keeps the entity model aligned with the migrated schema
- the default server port is `8080` unless overridden by `PORT`
- graceful shutdown is enabled
- HTTP session timeout is 30 minutes

## Backup Considerations

Production backup should cover:

- MySQL data
- application environment secrets stored outside Git

Because day logs live in MySQL now, there is no separate file storage requirement for user-written content.

## Suggested Post-Deploy Smoke Test

After deployment, confirm all of the following:

- `/actuator/health/readiness` returns `UP`
- home page loads after authentication
- registration creates a new account
- login failure shows expected generic feedback
- morning plan can be saved
- evening reflection can be saved
- weekly review renders without errors

## Recommended First Production Hardening Steps

- generate a strong `DAY_LOG_REMEMBER_ME_KEY`
- use a real MySQL password, never an example value
- place the app behind HTTPS
- set `SERVER_SERVLET_SESSION_COOKIE_SECURE=true` when TLS is terminated before the app
- restrict direct database exposure
- enable scheduled MySQL backups
- watch readiness and liveness endpoints from your hosting platform
