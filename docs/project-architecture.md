# Project Architecture

## Overview

`dayLog` is a Spring Boot web application for:

- morning planning
- evening reflection
- weekly progress review

The application now uses a single relational storage model:

- MySQL stores accounts, authentication state, and day-by-day writing
- Markdown is reconstructed on demand for preview pages instead of being stored as files on disk

This keeps the product easier to back up, scale, and operate in a multi-user deployment.

## System Shape

```text
Browser
  -> Spring MVC + Thymeleaf
  -> controller layer
  -> service layer
  -> Spring Data JPA repositories
  -> MySQL
```

## Top-Level Repository Layout

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
│  │  └─ resources
│  └─ test
└─ settings.gradle
```

## Main Java Packages

```text
com.potterlim.daylog
├─ config
├─ controller
├─ dto
│  ├─ auth
│  └─ dailylog
├─ entity
├─ repository
├─ security
├─ service
└─ support
```

## Package Responsibilities

### `config`

Application-wide configuration and framework integration.

- `DayLogApplicationProperties`
  - binds `day-log.security.*` settings into a strongly typed configuration object
- `SecurityConfiguration`
  - configures authentication, authorization, remember-me, logout, public health endpoints, and password encoding
- `WebServerConfiguration`
  - holds embedded web-server customization

### `controller`

HTTP entry points and page model composition.

- `HomeController`
  - renders the home page
- `AuthController`
  - renders login and registration pages
  - coordinates registration and auto-login
  - keeps login feedback generic for authentication failures
- `DailyLogController`
  - handles morning, evening, weekly, and preview routes
  - assembles page-ready data from service output

### `dto`

DTOs are split by feature and intent.

- `dto.auth`
  - form DTOs for login and registration
  - command DTO for registration service input
- `dto.dailylog`
  - form DTOs for morning and evening flows
  - checklist item DTOs
  - weekly status and per-day progress DTOs

### `entity`

Persistence model and authenticated principal model.

- `UserAccount`
  - JPA entity
  - also implements `UserDetails`
- `DailyLogEntry`
  - one persisted daily log per user per date
  - stores the morning and evening sections as text columns
  - reconstructs Markdown when preview output is needed
- `UserAccountId`
  - value object wrapper around the account id
- `EUserRole`
  - role enum used by security and persistence

### `repository`

Data access for relational storage.

- `IUserAccountRepository`
  - JPA repository for user accounts
- `IDailyLogEntryRepository`
  - JPA repository for day entries
  - supports lookup by user/date and ordered week queries

### `security`

Spring Security integration layer.

- `SecurityUserDetailsService`
  - resolves `UserAccount` by username for authentication

### `service`

Core business logic.

- `UserAccountService`
  - validates registration input
  - checks duplicate usernames
  - hashes passwords
  - persists new accounts
- `DailyLogService`
  - reads and writes day sections
  - creates day entries on first write
  - lists weekly day status
  - extracts checked goals
  - rebuilds Markdown text for previews

### `support`

Shared helper types outside the controller and service contracts.

- `EDailyLogSectionType`
  - defines Markdown section headers and logical order
- `SimpleMarkdownRenderer`
  - renders the supported Markdown subset used by the app

## Main Resource Layout

```text
src/main/resources
├─ application.yml
├─ application-local.yml
├─ db/migration
├─ static
│  ├─ css/site.css
│  └─ js/site.js
└─ templates
   ├─ auth
   ├─ dailylog
   ├─ fragments
   └─ home
```

### Templates

- `fragments/layout.html`
  - shared frame, navigation, footer, and background system
- `auth/*`
  - login and registration screens
- `dailylog/*`
  - morning list/editor
  - evening list/editor
  - weekly review
  - read-only log preview
- `home/index.html`
  - landing page and product overview

### Static Assets

- `static/css/site.css`
  - global visual system, product surfaces, responsive layout, and interaction styling
- `static/js/site.js`
  - lightweight browser behavior such as scroll-driven header state and textarea auto-resize

### Database Migrations

- `db/migration/V1__create_user_account.sql`
  - creates the account table
- `db/migration/V2__create_daily_log_entry.sql`
  - creates the daily log entry table with one row per user and date

## Route Map

| Area | Routes | Responsibility |
| --- | --- | --- |
| Home | `/` | product landing page for authenticated users |
| Auth | `/login`, `/register` | authentication entry and account creation |
| Morning | `/daily-log/morning`, `/daily-log/morning/edit`, `/daily-log/morning/save` | create and update morning plans |
| Evening | `/daily-log/evening`, `/daily-log/evening/edit`, `/daily-log/evening/save` | complete evening reflections and goal checks |
| Weekly | `/daily-log/week` | weekly review and completion summary |
| Preview | `/daily-log/preview` | read-only view of a saved Markdown log |
| Health | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | runtime monitoring without authentication |

## Storage Model

## Relational Account Storage

MySQL stores the `user_account` table used for:

- authentication
- role lookup
- account lifecycle status
- username uniqueness

## Relational Daily Log Storage

MySQL stores the `daily_log_entry` table used for:

- one row per user per date
- morning section content
- evening section content
- day-level morning/evening completion flags
- audit timestamps

Important design notes:

- the unique key is `(user_account_id, log_date)`
- daily writing is stored as section text, not as one opaque markdown blob
- preview pages reconstruct Markdown from the stored sections when needed
- the system no longer depends on local disk files for user-written content

## Daily Log Section Model

One day's log can contain multiple ordered sections.

Morning sections:

- goals
- focus areas
- challenges and strategies

Evening sections:

- evening goal checklist
- achievements
- improvements
- gratitude
- notes

Section ordering and header text are controlled by:

- `EDailyLogSectionType`
- `DailyLogEntry`
- `DailyLogService`

## Core Request Flows

### Registration Flow

1. render registration page
2. validate form input
3. create account through `UserAccountService`
4. auto-authenticate the new account
5. redirect to home

### Login Flow

1. render login page
2. validate form input
3. authenticate through Spring Security
4. save the authenticated principal into the session
5. return a generic login error message when authentication fails

### Morning Planning Flow

1. open a date
2. load current goals, focus, and challenges from `DailyLogService`
3. submit the morning form
4. normalize goals into Markdown-style list lines
5. write updated sections into `daily_log_entry`

### Evening Reflection Flow

1. load the reconstructed morning plan preview
2. convert morning goals into checklist items
3. submit checked goals and reflection content
4. persist evening sections back into the same day entry
5. redirect to the evening list

### Weekly Review Flow

1. list the current week's saved day status
2. read total goals and checked goals per day
3. calculate totals and percentages
4. render progress cards and links to preview pages

## Security Model

Public routes:

- `/css/**`
- `/js/**`
- `/favicon.ico`
- `/login`
- `/register`
- `/actuator/health`
- `/actuator/health/**`

All other routes require authentication.

Additional notes:

- BCrypt password hashing
- remember-me support via `TokenBasedRememberMeServices`
- generic login failure feedback
- CSRF protection enabled
- HTTP-only session cookie with `SameSite=Lax`
- basic security headers for content type, referrer policy, and frame handling

## Runtime Profiles

### Default Profile

- MySQL-backed
- production-oriented
- Flyway migrations enabled
- fails fast when required environment variables are missing

### `local` Profile

- H2 in-memory database in MySQL compatibility mode
- Flyway migrations enabled
- Thymeleaf cache disabled

## Testing Strategy

The test suite focuses on live application behavior rather than only isolated units.

Current integration coverage includes:

- registration
- password validation
- generic login failure feedback
- morning log persistence
- morning list rendering
- rendering of core product pages
- public health endpoint availability

Primary test classes:

- `DayLogApplicationTests`
- `WebFlowIntegrationTests`

## Extension Guidance

Safe extension points usually start in:

- `dto.dailylog` when a page shape changes
- `DailyLogService` when entry-level business logic changes
- `DailyLogEntry` when section persistence rules change
- `site.css` when the product presentation changes

Be careful when changing:

- Markdown header text
- section ordering
- goal list formatting
- uniqueness rules on user/date

Those areas affect compatibility with already saved user data and preview rendering.
