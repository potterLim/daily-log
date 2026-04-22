# Project Architecture

## Overview

`dayLog` is a Spring Boot web application for:

- morning planning
- evening reflection
- weekly progress review

The system intentionally splits storage responsibilities:

- MySQL stores accounts, authentication state, and user lookup data
- Markdown files on disk store the actual day-by-day planning and reflection content

This keeps identity and security relational while keeping personal writing easy to inspect, export, and back up.

## System Shape

```text
Browser
  -> Spring MVC + Thymeleaf
  -> controller layer
  -> service layer
  -> MySQL for accounts
  -> file system for Markdown logs
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
  - binds `day-log.*` settings into a strongly typed configuration object
  - currently covers log storage and remember-me settings
- `SecurityConfiguration`
  - configures authentication, authorization, remember-me, logout, and password encoding
- `WebServerConfiguration`
  - holds embedded web-server customization

### `controller`

HTTP entry points and page model composition.

- `HomeController`
  - renders the home page
- `AuthController`
  - renders login and registration pages
  - coordinates registration and auto-login
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
- `UserAccountId`
  - value object wrapper around the account id
- `EUserRole`
  - role enum used by security and persistence

### `repository`

Data access for relational account storage.

- `IUserAccountRepository`
  - JPA repository for user accounts

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
  - resolves log file paths by date and user
  - reads and writes Markdown sections
  - lists weekly day status
  - extracts checked goals
  - reads full log content

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
├─ schema.sql
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

## Route Map

| Area | Routes | Responsibility |
| --- | --- | --- |
| Home | `/` | product landing page for authenticated users |
| Auth | `/login`, `/register` | authentication entry and account creation |
| Morning | `/daily-log/morning`, `/daily-log/morning/edit`, `/daily-log/morning/save` | create and update morning plans |
| Evening | `/daily-log/evening`, `/daily-log/evening/edit`, `/daily-log/evening/save` | complete evening reflections and goal checks |
| Weekly | `/daily-log/week` | weekly review and completion summary |
| Preview | `/daily-log/preview` | read-only view of a saved Markdown log |

## Storage Model

## Relational Account Storage

MySQL stores the `user_account` table used for:

- authentication
- role lookup
- account lifecycle status
- username uniqueness

The base schema is initialized through `schema.sql`.

## File-Based Daily Logs

Daily logs are stored as Markdown files under the configured root path.

Example structure:

```text
logs/
└─ 15/
   └─ 2026_04_Week4/
      └─ 2026-04-23.md
```

The path is derived from:

- user account id
- year and month
- month-local week bucket
- date file name

Important note:

- `WeekN` is not ISO week numbering
- it is calculated with `(dayOfMonth - 1) / 7 + 1`

## Markdown Section Model

One day's log file can contain multiple ordered sections.

Typical sections include:

- goals
- focus areas
- challenges and strategies
- evening goal check
- achievements
- improvements
- gratitude
- notes for tomorrow

Section parsing and reconstruction are controlled by:

- `EDailyLogSectionType`
- `DailyLogService`

## Core Request Flows

### Registration Flow

1. render registration page
2. validate form input
3. create account through `UserAccountService`
4. auto-authenticate the new account
5. redirect to home

### Morning Planning Flow

1. open a date
2. load current goals, focus, and challenges
3. submit the morning form
4. normalize goals into Markdown list items
5. write updated sections through `DailyLogService`

### Evening Reflection Flow

1. load the morning plan preview
2. convert morning goals into checklist items
3. submit checked goals and reflection content
4. persist evening sections back into the same Markdown file
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

All other routes require authentication.

Additional notes:

- BCrypt password hashing
- remember-me support via `TokenBasedRememberMeServices`
- CSRF protection enabled
- HTTP-only session cookie with `SameSite=Lax`

## Runtime Profiles

### Default Profile

- MySQL-backed
- production-oriented
- fails fast when required environment variables are missing

### `local` Profile

- H2 in-memory database in MySQL compatibility mode
- log root at `build/local-logs`
- Thymeleaf cache disabled

## Testing Strategy

The test suite focuses on live application behavior rather than only isolated units.

Current integration coverage includes:

- registration
- login failure feedback
- morning log persistence
- morning list rendering
- rendering of core product pages

Primary test classes:

- `DayLogApplicationTests`
- `WebFlowIntegrationTests`

## Extension Guidance

Safe extension points usually start in:

- `dto.dailylog` when a page shape changes
- `DailyLogService` when log storage behavior changes
- `DailyLogController` when page assembly changes
- `site.css` when the product presentation changes

Be careful when changing:

- Markdown header text
- section ordering
- goal list formatting
- file path naming rules

Those areas affect compatibility with already saved user data.
