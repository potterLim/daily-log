# Project Architecture

## Overview

`daymark` is a Spring Boot web application for:

- morning planning
- evening reflection
- weekly progress review
- long-term record exploration
- Markdown and print-ready PDF export

The application uses a single relational storage model:

- MySQL stores accounts, authentication state, and day-by-day writing.
- Markdown is reconstructed on demand for preview, library, and export pages instead of being stored as files on disk.
- Library views derive timeline, trend, calendar, search, and export models from the same persisted day sections.
- The Thymeleaf presentation layer renders reconstructed content through product-grade reading surfaces instead of raw debug-style Markdown blocks.

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
daymark
├─ build.gradle
├─ compose.yaml
├─ Dockerfile
├─ docs
├─ gradle
├─ ops
├─ src
│  ├─ main
│  │  ├─ java/com/potterlim/daymark
│  │  └─ resources
│  └─ test
└─ settings.gradle
```

## Main Java Packages

```text
com.potterlim.daymark
├─ config
├─ controller
├─ dto
│  ├─ auth
│  └─ daymark
├─ entity
├─ repository
├─ security
├─ service
└─ support
```

## Package Responsibilities

### `config`

Application-wide configuration and framework integration.

- `ApplicationClockConfiguration`
  - provides the application `Clock` used by date-sensitive views and tests
- `DaymarkApplicationProperties`
  - binds `daymark.account.*`, `daymark.mail.*`, `daymark.operations.*`, and `daymark.security.*` settings into a strongly typed configuration object
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
  - renders the product home page
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
- `ProductErrorControllerAdvice`
  - renders the product 404 page for missing static or resource-backed routes
- `DaymarkController`
  - handles morning, evening, weekly, preview, library, Markdown export, and PDF export-preview routes
  - assembles page-ready data from service output, including week navigation ranges and export report models

### `dto`

DTOs are split by feature and intent.

- `dto.auth`
  - form DTOs for login, registration, forgot-password, reset-password, and password change
  - command DTO for registration service input
- `dto.daymark`
  - form DTOs for morning and evening flows
  - checklist item DTOs
  - weekly status and per-day progress DTOs
  - library search criteria, timeline items, structured preview blocks, goal preview rows, trend items, and calendar days

### `entity`

Persistence model and authenticated principal model.

- `UserAccount`
  - JPA entity
  - also implements `UserDetails`
- `UserEmailVerificationToken`
  - one-time email ownership verification token entity bound to a user account
- `UserPasswordResetToken`
  - one-time reset token entity bound to a user account
- `DaymarkEntry`
  - one persisted Daymark entry per user per date
  - stores the morning and evening sections as text columns
  - derives morning/evening presence from actual non-blank section content
  - reconstructs Markdown when preview, library, or export output is needed
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
- `IDaymarkEntryRepository`
  - JPA repository for day entries
  - supports lookup by user/date, ordered week queries, and date-range library queries

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
- `DaymarkService`
  - reads and writes day sections
  - creates day entries on first write
  - lists weekly day status
  - extracts checked goals
  - rebuilds Markdown text for previews
- `DaymarkLibraryService`
  - searches long-term records by date range and keyword
  - builds timeline items, goal previews, structured content blocks, trend bars, and calendar days
  - builds chronological Markdown exports for selected library ranges

### `support`

Shared helper types outside the controller and service contracts.

- `EDaymarkSectionType`
  - defines Markdown section headers and logical order
- `SimpleMarkdownRenderer`
  - renders the supported Markdown subset used by the app

## Main Resource Layout

```text
src/main/resources
├─ application.yml
├─ application-local.yml
├─ application-production.yml
├─ db/migration
├─ static
│  ├─ css/site.css
│  └─ js/site.js
└─ templates
   ├─ account
   ├─ auth
   ├─ daymark
   ├─ error
   ├─ fragments
   └─ home
```

### Templates

- `fragments/layout.html`
  - shared frame, navigation, footer, verification banner, and background system
- `auth/*`
  - login, registration, forgot-password, and reset-password screens
- `account/*`
  - authenticated account settings pages such as password change
- `daymark/*`
  - morning list/editor
  - evening list/editor
  - weekly review
  - long-term record library
  - Markdown export endpoint support through controller output
  - print-optimized PDF export preview
  - read-only log preview and empty preview state
- `error/404.html`
  - product-styled not-found page
- `home/index.html`
  - landing page and product overview

### Static Assets

- `static/css/site.css`
  - global visual system, product surfaces, responsive layout, print report styling, empty states, and interaction styling
- `static/js/site.js`
  - lightweight browser behavior such as scroll-driven header state and textarea auto-resize

### Database Migrations

- `db/migration/V1__create_user_account.sql`
  - creates the account table
- `db/migration/V2__create_daymark_entry.sql`
  - creates the Daymark entry table with one row per user and date
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
| Morning | `/daymark/morning`, `/daymark/morning/edit`, `/daymark/morning/save` | create and update morning plans |
| Evening | `/daymark/evening`, `/daymark/evening/edit`, `/daymark/evening/save` | complete evening reflections and goal checks |
| Weekly | `/daymark/week` | Monday-Sunday weekly review and completion summary |
| Library | `/daymark/library` | long-term date-range search, keyword search, timeline, trend, and calendar view |
| Markdown export | `/daymark/library/export/markdown` | downloads selected library records as Markdown |
| PDF preview | `/daymark/library/export/pdf` | renders a print-optimized report for browser PDF saving |
| Preview | `/daymark/preview` | read-only view of a saved record or a product empty state for blank dates |
| Error | missing product/resource routes through Spring error handling | product not-found experience |
| Health | `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness` | runtime monitoring without authentication |

## Presentation Baseline

The UI layer is intentionally copy-light and product-oriented:

- shared headers keep brand, navigation, and account actions in separate visual zones
- public authentication pages use concise security and workflow labels instead of explanatory paragraphs
- writing pages avoid duplicate helper text when the adjacent guide already explains the section
- preview, evening reference, library card, and PDF report content use the same polished reading language
- library trend labels explicitly describe goal-completion rate over time
- export preview output is optimized for Chrome print/save-to-PDF rather than plain browser text output

## Relational Account Storage

MySQL stores the `user_account`, `user_email_verification_token`, and `user_password_reset_token` tables used for:

- authentication
- role lookup
- account lifecycle status
- username uniqueness
- email uniqueness, ownership verification, and recovery routing
- one-time email verification token storage
- one-time password reset token storage

## Relational Daymark Storage

MySQL stores the `daymark_entry` table used for:

- one row per user per date
- morning section content
- evening section content
- day-level morning/evening completion flags derived from non-blank content
- audit timestamps

Important design notes:

- the unique key is `(user_account_id, entry_date)`
- daily writing is stored as section text, not as one opaque Markdown blob
- preview, library, and export pages reconstruct Markdown from stored sections when needed
- empty section headers are omitted from reconstructed Markdown
- blank saves do not produce visible library or review records
- the system does not depend on local disk files for user-written content

## Daymark Section Model

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

- `EDaymarkSectionType`
- `DaymarkEntry`
- `DaymarkController` preview normalization for browser rendering

## Core Request Flows

### Registration Flow

1. Render registration page.
2. Validate username, email address, and password input.
3. Create account through `UserAccountService`.
4. Catch duplicate username or duplicate email collisions.
5. Issue and deliver an email ownership verification link.
6. Auto-authenticate the new account.
7. Redirect to home.

### Email Verification Flow

1. Issue a one-time token through `EmailVerificationTokenService`.
2. Deliver the verification link through `IAuthenticationMailService`.
3. Consume the token exactly once when the user opens the link.
4. Mark the user account email as verified.
5. Remove the verification banner from subsequent page renders.

### Login Flow

1. Render login page.
2. Validate form input.
3. Authenticate through Spring Security with username or email.
4. Save the authenticated principal into the session.
5. Return a generic login error message when authentication fails.

### Password Recovery Flow

1. Render the forgot-password page.
2. Accept an email address and always return the same success message.
3. When the account exists and the email is verified, generate a one-time token through `PasswordResetTokenService`.
4. When the account exists but the email is still unverified, resend the ownership verification link instead.
5. Consume the password reset token exactly once when the user submits a new password.

### Password Change Flow

1. Require an authenticated session.
2. Render the password change form.
3. Verify the current password through `UserAccountService`.
4. Hash and persist the new password.
5. Redirect back with a success banner.

### Morning Planning Flow

1. Open a date.
2. Load current goals, focus, and challenges from `DaymarkService`.
3. Submit the morning form.
4. Normalize goals into Markdown-style list lines.
5. Write updated sections into `daymark_entry`.
6. Recompute day presence from actual section content.

### Evening Reflection Flow

1. Load the reconstructed morning plan preview as a structured read-only card surface.
2. Convert morning goals into checklist items.
3. Submit checked goals and reflection content.
4. Persist evening sections back into the same day entry.
5. Recompute day presence from actual section content.
6. Redirect to the evening list.

### Weekly Review Flow

1. List the current Monday-Sunday week.
2. Read total goals and checked goals per day.
3. Calculate totals and percentages.
4. Render progress cards and links to preview pages.

### Record Library Flow

1. Build default search criteria for the recent 90-day range.
2. Accept optional `from`, `to`, and `keyword` query parameters.
3. Query only the authenticated user's entries inside the selected range.
4. Ignore entries that have no meaningful saved content.
5. Filter by keyword across date, flow label, excerpt, and reconstructed Markdown.
6. Render newest-first timeline items with structured previews.
7. Render goal-completion trend and calendar side panels as supporting context.

### Markdown Export Flow

1. Reuse the same library search criteria.
2. Build matching records in chronological order.
3. Prepend export metadata such as date range, keyword, and record count.
4. Return `text/markdown; charset=UTF-8` with a download filename.

### PDF Preview Flow

1. Reuse the same library search criteria.
2. Render a print-optimized Thymeleaf report with cover, summary metrics, selected filters, and polished daily record cards.
3. Let the browser save the print view as PDF.

### Product Error Flow

1. Disable the default whitelabel error page.
2. Convert missing resource routes into a product-styled 404 view.
3. Offer clear navigation back to home or the record library.

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
- diagnostic verification and recovery links when SMTP is not configured

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
- morning log persistence and blank-save protection
- evening reflection persistence
- weekly Monday-Sunday range rendering
- read-only preview rendering and empty-section omission
- empty preview state for dates without saved content
- record library search, timeline, Markdown export, and PDF preview routing
- product copy and polished page-rendering expectations for home, library, and export surfaces
- custom product 404 rendering
- core page rendering for home, evening, weekly, and library views
- public health endpoint availability

Primary test classes:

- `DaymarkApplicationTests`
- `WebFlowIntegrationTests`
- `MySqlIntegrationTests`
- `WeeklyOperationsSummaryServiceTests`

## Extension Guidance

Safe extension points usually start in:

- `dto.daymark` when a page shape changes
- `DaymarkService` when entry-level business logic changes
- `DaymarkLibraryService` when long-term exploration, export, trend, or calendar behavior changes
- `DaymarkEntry` when section persistence rules change
- `site.css` when the product presentation changes

Be careful when changing:

- Markdown header text
- section ordering
- goal list formatting
- date-range semantics
- uniqueness rules on user/date
- export filename or content-disposition behavior

Those areas affect compatibility with already saved user data, previews, library search, and exported records.
