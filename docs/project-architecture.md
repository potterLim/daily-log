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
  - binds `day-log.account.*`, `day-log.mail.*`, `day-log.operations.*`, and `day-log.security.*` settings into a strongly typed configuration object
- `ProductionReadinessValidator`
  - enforces strict runtime checks in the `production` profile
  - blocks startup when SMTP, alerting, secure cookies, or remember-me secrets do not meet the configured baseline
- `SecurityConfiguration`
  - configures authentication, authorization, remember-me, logout, public health endpoints, and password encoding
- `WebServerConfiguration`
  - holds embedded web-server customization

### `controller`

HTTP entry points and page model composition.

- `HomeController`
  - renders the home page
- `AuthController`
  - renders login, registration, forgot-password, and reset-password pages
  - coordinates registration, auto-login, and public email verification
  - keeps login and password recovery feedback generic where account existence must stay hidden
- `AccountController`
  - renders the authenticated password change page
  - resends verification mail for the signed-in user
  - verifies the current password before saving a new one
- `AuthenticatedUserModelAttributeControllerAdvice`
  - injects current account verification state into page models for the shared layout banner
- `DailyLogController`
  - handles morning, evening, weekly, and preview routes
  - assembles page-ready data from service output

### `dto`

DTOs are split by feature and intent.

- `dto.auth`
  - form DTOs for login, registration, forgot-password, reset-password, and password change
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
- `UserEmailVerificationToken`
  - one-time email ownership verification token entity bound to a user account
- `UserPasswordResetToken`
  - one-time reset token entity bound to a user account
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
  - supports lookup by username, email address, and login identifier
- `IUserEmailVerificationTokenRepository`
  - JPA repository for one-time email verification tokens
  - supports token lookup and invalidation of earlier active tokens
- `IUserPasswordResetTokenRepository`
  - JPA repository for one-time password reset tokens
  - supports token lookup and invalidation of earlier active tokens
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
  - checks duplicate usernames and email addresses
  - hashes passwords
  - persists new accounts
  - changes or resets passwords
- `EmailVerificationTokenService`
  - generates strong verification tokens
  - stores only token hashes
  - enforces token expiration and one-time consumption
- `PasswordResetTokenService`
  - generates strong reset tokens
  - stores only token hashes
  - enforces token expiration and one-time consumption
- `IAuthenticationMailService`
  - abstracts verification and password reset mail delivery
  - can be backed by SMTP or a diagnostic local mode
- `AuthenticationMailWorkflowService`
  - chooses between verification and recovery delivery flows
  - builds absolute verification and reset URLs from the current request
  - reports delivery failures to operations alerts
- `IAlertNotificationService`
  - abstracts operational alert delivery
  - can fall back to logging or send to an external webhook
- `WeeklyOperationsSummaryService`
  - calculates operator-facing weekly metrics such as total registrations, weekly active writers, writing frequency, and checklist completion rate
- `WeeklyOperationsSummaryScheduler`
  - emits the weekly operator summary log on a configurable schedule
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
  - login, registration, forgot-password, and reset-password screens
- `account/*`
  - authenticated account settings pages such as password change
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
- `db/migration/V3__add_email_to_user_account.sql`
  - adds the unique recovery email column to existing accounts
- `db/migration/V4__create_user_password_reset_token.sql`
  - creates the one-time password reset token table
- `db/migration/V5__add_email_verification_state_to_user_account.sql`
  - adds email verification state to user accounts
- `db/migration/V6__create_user_email_verification_token.sql`
  - creates the one-time email verification token table

## Route Map

| Area | Routes | Responsibility |
| --- | --- | --- |
| Home | `/` | product landing page for authenticated users |
| Auth | `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email` | authentication entry, account creation, verification, and recovery |
| Account | `/account/password`, `/account/email-verification/resend` | authenticated password maintenance and verification resend |
| Morning | `/daily-log/morning`, `/daily-log/morning/edit`, `/daily-log/morning/save` | create and update morning plans |
| Evening | `/daily-log/evening`, `/daily-log/evening/edit`, `/daily-log/evening/save` | complete evening reflections and goal checks |
| Weekly | `/daily-log/week` | weekly review and completion summary |
| Preview | `/daily-log/preview` | read-only view of a saved Markdown log |
| Health | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | runtime monitoring without authentication |

## Relational Account Storage

MySQL stores the `user_account`, `user_email_verification_token`, and `user_password_reset_token` tables used for:

- authentication
- role lookup
- account lifecycle status
- username uniqueness
- email uniqueness, ownership verification, and recovery routing
- one-time email verification token storage
- one-time password reset token storage

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
2. validate user name, email address, and password input
3. create account through `UserAccountService`
4. catch duplicate user name or duplicate email collisions
5. issue and deliver an email ownership verification link
6. auto-authenticate the new account
7. redirect to home

### Email Verification Flow

1. issue a one-time token through `EmailVerificationTokenService`
2. deliver the verification link through `IAuthenticationMailService`
3. consume the token exactly once when the user opens the link
4. mark the user account email as verified
5. remove the verification banner from subsequent page renders

### Login Flow

1. render login page
2. validate form input
3. authenticate through Spring Security with username or email
4. save the authenticated principal into the session
5. return a generic login error message when authentication fails

### Password Recovery Flow

1. render the forgot-password page
2. accept an email address and always return the same success message
3. when the account exists and the email is verified, generate a one-time token through `PasswordResetTokenService`
4. when the account exists but the email is still unverified, resend the ownership verification link instead
5. consume the password reset token exactly once when the user submits a new password

### Password Change Flow

1. require an authenticated session
2. render the password change form
3. verify the current password through `UserAccountService`
4. hash and persist the new password
5. redirect back with a success banner

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
- `/forgot-password`
- `/reset-password`
- `/verify-email`
- `/actuator/health`
- `/actuator/health/**`

All other routes require authentication.

Additional notes:

- BCrypt password hashing
- login by username or email address
- remember-me support via `TokenBasedRememberMeServices`
- generic login failure feedback
- generic forgot-password feedback
- email ownership verification with one-time hashed tokens
- one-time password reset tokens stored as hashes
- CSRF protection enabled
- HTTP-only session cookie with `SameSite=Lax`
- basic security headers for content type, referrer policy, and frame handling

## Runtime Profiles

### Default Profile

- MySQL-backed
- production-oriented
- Flyway migrations enabled
- fails fast when required environment variables are missing

### `production` Profile

- turns on strict production readiness validation
- enables weekly operator summary logging
- defaults the session cookie to `secure=true`

### `local` Profile

- H2 in-memory database in MySQL compatibility mode
- Flyway migrations enabled
- Thymeleaf cache disabled

## Testing Strategy

The test suite focuses on live application behavior rather than only isolated units.

Current integration coverage includes:

- registration
- email verification
- password validation
- username or email login
- password reset request and token-based reset
- authenticated password change
- generic login failure feedback
- morning log persistence
- morning list rendering
- rendering of core product pages
- public health endpoint availability

Primary test classes:

- `DayLogApplicationTests`
- `WebFlowIntegrationTests`
- `MySqlIntegrationTests`
- `WeeklyOperationsSummaryServiceTests`

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
