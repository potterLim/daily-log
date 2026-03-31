# Project Architecture

## Scope

`dayLog` provides daily planning, evening reflection, and weekly progress tracking in a web application built with Java and Spring Boot.

## Target Directory Layout

```text
day-log
├─ build.gradle
├─ settings.gradle
├─ gradle/wrapper
├─ src/main/java/com/potterlim/daylog
│  ├─ config
│  ├─ controller
│  ├─ dto
│  │  ├─ auth
│  │  ├─ dailylog
│  │  └─ view
│  ├─ entity
│  ├─ repository
│  ├─ security
│  ├─ service
│  └─ support
└─ src/main/resources
   ├─ static
   ├─ templates
   └─ application.yml
```

## Layered Architecture

- `controller`
  - Handles HTTP requests, validation failure routing, flash messages, and model binding.
- `dto`
  - Separates form input DTOs, page view DTOs, and internal transfer objects by responsibility.
- `service`
  - Owns authentication support, daily log file handling, weekly statistics, and preview preparation.
- `repository`
  - Owns persistence access for user accounts.
- `security`
  - Owns authentication flow, authorization rules, password encoding, and login/logout wiring.
- `support`
  - Owns reusable date, file path, and helper logic.

## Implementation Phases

1. Initialize the isolated Spring Boot project and baseline architecture.
2. Implement authentication and shared layout.
3. Implement the morning planning flow.
4. Implement the evening reflection flow with plan preview support.
5. Implement weekly statistics and detailed log preview.
6. Align styling and visible behavior across the application.
