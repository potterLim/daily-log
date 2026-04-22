# Deployment Guide

## Overview

`dayLog` is designed to run as a multi-user web application with:

- MySQL for account data
- local disk storage for Markdown log files
- executable JAR deployment as the primary runtime model

The repository also includes Docker Compose support for environments where running the app and MySQL together in containers is preferred.

## Deployment Options

## Option 1. Executable JAR

Recommended when:

- you are deploying to a VM or server directly
- you want to manage the process with `systemd`
- you plan to put the app behind Nginx or Caddy

## Option 2. Docker Compose

Recommended when:

- you want the app and MySQL packaged together
- you prefer containerized local or server deployment
- you want simpler environment replication across machines

## Runtime Requirements

## JAR deployment

- Java 17 installed on the target machine
- reachable MySQL database
- persistent directory for Markdown logs

## Docker Compose deployment

- Docker Engine
- Docker Compose plugin
- writable persistent volume or bind mount for logs and MySQL data

## Environment Variables

### Required

- `DATABASE_URL`
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`
- `DAY_LOG_REMEMBER_ME_KEY`

The application fails fast on startup when any required value is missing in the default profile.

### Optional

- `PORT`
- `DAY_LOG_LOGS_ROOT_PATH`
- `DAY_LOG_REMEMBER_ME_COOKIE_NAME`
- `DAY_LOG_REMEMBER_ME_TOKEN_VALIDITY_SECONDS`

## Example JDBC URL

```text
jdbc:mysql://localhost:3306/daylog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

Adjust host, port, and database name for your environment.

## Local Verification

Use the local profile when you want to verify application behavior without preparing MySQL first.

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

Local profile behavior:

- H2 in-memory database
- MySQL compatibility mode
- logs stored under `build/local-logs`

## Build the Executable JAR

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

### 2. Copy the JAR to the server

Example target:

```text
/opt/day-log/dayLog.jar
```

### 3. Prepare runtime directories

Example:

```bash
sudo mkdir -p /opt/day-log
sudo mkdir -p /var/lib/day-log/logs
```

### 4. Provide environment variables

A common pattern is to place them in a dedicated environment file.

Example:

```bash
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/daylog?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DATABASE_USERNAME=daylog
DATABASE_PASSWORD=replace-this
DAY_LOG_REMEMBER_ME_KEY=replace-this-with-a-long-random-secret
DAY_LOG_LOGS_ROOT_PATH=/var/lib/day-log/logs
PORT=8080
```

### 5. Run the application

```bash
java -jar /opt/day-log/dayLog.jar
```

## Example systemd Service

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

## Persistent Data

Production deployment should persist both:

- MySQL data
- the directory referenced by `DAY_LOG_LOGS_ROOT_PATH`

Without persistent storage for logs, user-written daily records will be lost when the server or container is recreated.

## Docker Compose Workflow

### 1. Copy the example environment file

```powershell
Copy-Item .env.example .env
```

### 2. Replace all example secrets

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

## Operational Notes

- The app uses `ddl-auto=validate`, so the schema must exist and match the entity model.
- `schema.sql` is executed on startup and is expected to initialize the `user_account` table.
- The default server port is `8080` unless overridden by `PORT`.
- The app uses graceful shutdown and a 30-minute session timeout.

## Suggested First Production Checklist

- generate a strong `DAY_LOG_REMEMBER_ME_KEY`
- use a real MySQL password, not an example value
- map persistent log storage
- place the app behind HTTPS
- confirm login, registration, morning save, and weekly review in the deployed environment

