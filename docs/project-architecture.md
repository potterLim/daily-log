# Project Architecture

## Overview

`dayLog` is a Spring Boot web application for:

- morning planning
- evening reflection
- weekly progress review

The project combines two storage models:

- MySQL for account and authentication data
- Markdown files on disk for daily log content

This split is intentional. User identity and access control stay relational, while daily writing remains file-based and easy to inspect or back up.

## Top-Level Structure

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

Application-wide configuration.

- `DayLogApplicationProperties`
  - binds `day-log.*` configuration into a typed object
  - currently covers log storage and remember-me settings
- `SecurityConfiguration`
  - configures authentication, authorization, remember-me, logout, and password encoding
- `WebServerConfiguration`
  - holds web-server-level customization

### `controller`

Web request orchestration and view composition.

- `HomeController`
  - renders the home page
- `AuthController`
  - handles login and registration pages
  - performs registration flow and auto-login behavior
- `DailyLogController`
  - handles morning, evening, weekly, and preview routes
  - converts service data into page-ready models

### `dto`

Separated by feature and responsibility.

- `dto.auth`
  - request DTOs for login and registration
  - service command object for account registration
- `dto.dailylog`
  - page form DTOs
  - checklist item DTOs
  - weekly progress DTOs
  - per-day status DTOs

### `entity`

Persistence and authenticated user model.

- `UserAccount`
  - JPA entity
  - also implements `UserDetails`
- `UserAccountId`
  - value object for stronger typing than raw `Long`
- `EUserRole`
  - role enum used by security and persistence

### `repository`

Persistence access layer.

- `IUserAccountRepository`
  - JPA repository for user accounts

### `security`

Security integration layer.

- `SecurityUserDetailsService`
  - loads `UserAccount` by username for Spring Security

### `service`

Core business logic.

- `UserAccountService`
  - validates registration input
  - checks duplicate usernames
  - hashes passwords
  - persists new accounts
- `DailyLogService`
  - resolves file paths by user and date
  - reads and writes Markdown sections
  - lists weekly status
  - extracts checked goals
  - reads full log content

### `support`

Shared non-controller helper types.

- `EDailyLogSectionType`
  - defines Markdown section headers and section ordering
- `SimpleMarkdownRenderer`
  - renders the subset of Markdown used by the application

## Resources

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
  - shared page frame, navigation, footer, and background
- `auth/*`
  - login and registration screens
- `dailylog/*`
  - morning plan, evening reflection, weekly review, and preview pages
- `home/index.html`
  - landing page and product overview

### Static assets

- `static/css/site.css`
  - global design system and responsive layout rules
- `static/js/site.js`
  - small UI enhancements such as scroll behavior and textarea auto-resize

## Storage Model

## MySQL

MySQL stores the `user_account` table used for authentication and account lookup.

Current schema is initialized through `schema.sql`.

## Markdown logs

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

## Markdown section model

One day's file can contain multiple sections, including both morning and evening content.

Examples:

- goals
- focus
- challenges
- evening goal check
- achievements
- improvements
- gratitude
- notes

Section parsing and reconstruction are controlled by `EDailyLogSectionType` and `DailyLogService`.

## Request Flow Summary

### Registration

1. render registration page
2. validate form input
3. create account through `UserAccountService`
4. auto-authenticate
5. redirect to home

### Morning plan

1. open date
2. load goals, focus, and challenges for that date
3. submit form
4. normalize goals into Markdown list items
5. write sections back through `DailyLogService`

### Evening reflection

1. load morning plan preview
2. build checklist DTOs from morning goals
3. submit reflection form
4. write checked goals and reflection sections
5. redirect to evening list

### Weekly review

1. collect week status
2. read goals and checked goals per date
3. calculate totals and percentages
4. render daily progress cards

## Security Model

Public routes:

- `/css/**`
- `/js/**`
- `/favicon.ico`
- `/login`
- `/register`

All other routes require authentication.

Additional security notes:

- BCrypt password hashing
- remember-me token support
- CSRF protection enabled
- HTTP-only session cookie

## Runtime Profiles

### Default profile

- MySQL-backed
- production-oriented
- fails fast when required environment variables are missing

### `local` profile

- uses H2 in MySQL compatibility mode
- stores logs under `build/local-logs`
- disables Thymeleaf cache

## Testing Approach

The test suite focuses on the main application flows rather than only isolated unit logic.

Main coverage includes:

- registration
- login failure feedback
- morning log persistence
- morning list rendering
- rendering of core product pages

## Design Intent

The project is intentionally kept close to a standard Spring Boot structure so that:

- it opens naturally in IntelliJ IDEA
- new contributors can navigate it quickly
- deployment remains simple through either executable JAR or Docker Compose

