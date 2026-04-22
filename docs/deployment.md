# Deployment Guide

## Overview

`dayLog` is designed to run as a multi-user web application with:

- MySQL for account data
- local disk storage for Markdown log files
- executable JAR deployment as the primary runtime model

The repository also includes Docker Compose support for environments where packaging the app and MySQL together is preferred.

## Supported Deployment Models

### Option 1. Executable JAR on a VM

Recommended when:

- you are deploying to a Linux VM or server directly
- you want to manage the process with `systemd`
- you plan to place the app behind Nginx or Caddy

This is the preferred deployment model for compute-based hosting such as Oracle Cloud Compute, VPS providers, or self-managed Linux servers.

### Option 2. Docker Compose

Recommended when:

- you want the app and MySQL defined together
- you prefer containerized deployment
- you want an easy local-to-server runtime match

## Runtime Requirements

## Executable JAR Deployment

- Java 17 installed on the target machine
- reachable MySQL database
- writable persistent directory for Markdown logs

## Docker Compose Deployment

- Docker Engine
- Docker Compose plugin
- writable persistent storage for MySQL data and Markdown logs

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
| `DAY_LOG_LOGS_ROOT_PATH` | `logs` | root directory for Markdown logs |
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
- logs stored under `build/local-logs`

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
sudo mkdir -p /var/lib/day-log/logs
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
DAY_LOG_LOGS_ROOT_PATH=/var/lib/day-log/logs
PORT=8080
```

### 5. Start the application manually once

```bash
java -jar /opt/day-log/dayLog.jar
```

Use this first run to verify:

- database connectivity
- schema initialization
- log directory permissions
- login and registration flow

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

## Reverse Proxy Recommendation

For internet-facing deployment, place the application behind a reverse proxy such as:

- Nginx
- Caddy
- a managed load balancer

Recommended responsibilities of the proxy layer:

- HTTPS termination
- HTTP to HTTPS redirect
- security headers
- access logs
- forwarding traffic to the application port

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

### 5. Confirm persistence

The Compose file expects persistent volumes for:

- MySQL data
- Markdown logs

If those are not persisted, user records or daily logs can be lost when containers are recreated.

## Operational Notes

- The app uses `ddl-auto=validate`, so the schema must match the entity model
- `schema.sql` is executed on startup and is expected to initialize `user_account`
- the default server port is `8080` unless overridden by `PORT`
- graceful shutdown is enabled
- HTTP session timeout is 30 minutes

## Backup Considerations

Production backup should cover both:

- MySQL data
- the directory referenced by `DAY_LOG_LOGS_ROOT_PATH`

Database backup alone is not enough because user-written day logs live on disk as Markdown files.

## Suggested Post-Deploy Smoke Test

After deployment, confirm all of the following:

- home page loads after authentication
- registration creates a new account
- login failure shows expected feedback
- morning plan can be saved
- evening reflection can be saved
- weekly review renders without errors
- Markdown files appear under the configured log root

## Recommended First Production Hardening Steps

- generate a strong `DAY_LOG_REMEMBER_ME_KEY`
- use a real MySQL password, never an example value
- ensure persistent storage for logs and database data
- place the app behind HTTPS
- restrict direct database exposure
- set up regular backups for both MySQL and Markdown log storage
