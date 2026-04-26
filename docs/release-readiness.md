# Release Readiness

This document defines the final product QA checklist for Daymark.

It is intentionally focused on user-facing readiness: real navigation, visual stability, responsive behavior, account lifecycle flows, record-writing flows, long-term exploration, and export outputs.

## Current Release Baseline

As of the 2026-04-24 release polish pass, the product includes:

- account registration, login, logout, email verification, password recovery, and authenticated password change
- authenticated home, morning planning, evening reflection, weekly review, record library, daily preview, and account pages
- copy-light product surfaces with short button labels and reduced instructional text
- unauthenticated header layout with brand content on the left and account actions on the right
- record library search by date range and keyword
- timeline-first record library cards with structured previews
- explicit goal-completion trend bars and a compact calendar side panel
- Markdown export for selected library records
- print-ready PDF report preview with readable daily cards for browser PDF saving
- polished reading surfaces for daily preview, evening reference, library previews, and PDF report content
- product empty states for no records, no search results, and blank previews
- product 404 page for unknown routes

## Required Automated Checks

Run these before release:

```bash
./gradlew test
```

Run this when MySQL and Docker are available:

```bash
./gradlew mysqlIntegrationTest
```

Recommended pre-commit hygiene:

```bash
git diff --check
```

## Required Browser

Final visual QA should be performed in Google Chrome, not the in-app browser.

Use Chrome because the export workflow depends on real browser print behavior and because final UI verification should match the browser most likely to be used during manual QA.

## Screenshot Policy

Screenshots and generated export artifacts are evidence, not source code.

Keep them outside the repository, for example:

```text
~/Desktop/daymark-final-qa-YYYYMMDD-HHMMSS
```

Do not commit:

- PNG screenshots
- generated PDF exports
- generated Markdown exports
- QA JSON reports
- temporary users or fixture dumps

## Desktop Screen Matrix

Capture and inspect at least these desktop states:

- login
- login validation error
- registration
- registration validation error
- forgot-password form
- forgot-password success
- forgot-password success with one-line generic delivery copy
- reset-password valid token
- reset-password validation error
- reset-password invalid token
- authenticated home with verified account
- home with unverified-account banner
- account password page
- account password validation error
- account page with unverified-account banner
- morning list with data
- morning edit with data
- morning empty state
- evening list with data
- evening list week navigation with previous and next date ranges
- evening edit with data
- evening empty state
- weekly review with data
- weekly empty state
- record library with data
- record library keyword result
- record library empty search result
- record library empty account state
- daily preview with content
- daily preview for a blank or missing date
- PDF export preview
- unknown route 404
- long username/header stress case

## Mobile Screen Matrix

Capture and inspect at least these mobile states:

- login
- registration
- authenticated home
- home with unverified-account banner
- morning edit
- evening edit
- weekly review
- record library
- daily preview
- PDF export preview

## UX Checks

For every captured state, verify:

- no unintended horizontal scrolling
- no primary action button text wraps awkwardly
- no navigation item collapses into vertical letters
- long usernames truncate gracefully instead of breaking layout
- mobile helper links stack deliberately rather than wrapping unpredictably
- page copy is purposeful and short enough that screens do not feel like documentation
- auth showcase panels do not stretch just because the form column contains validation or success copy
- guest header actions are visually separated from the Daymark brand block
- empty states explain what happened and offer a useful next action
- library cards show digestible previews instead of raw Markdown walls
- library trend labels clearly describe goal-completion rate, not a vague "flow"
- preview pages feel like a reading surface, not debug output
- PDF export preview has report structure, summary metrics, and readable daily cards instead of raw Markdown output
- custom 404 page gives a clear recovery path

## Export Checks

### Markdown

Verify that the Markdown export:

- respects the selected `from`, `to`, and `keyword` filters
- downloads as `text/markdown; charset=UTF-8`
- includes date range metadata
- includes search keyword metadata when a keyword is present
- includes record count
- orders exported entries chronologically
- includes reconstructed daily sections without empty section headers

### PDF preview

Verify that the PDF preview:

- respects the same filters as the library page
- renders a report cover/header
- shows completion, record count, and goal summary metrics
- shows selected filter metadata
- renders one readable card per exported day
- opens Chrome print/save-to-PDF cleanly

## Functional Acceptance Checklist

Before calling the product release-ready, confirm:

- a new user can register and is signed in
- the unverified email banner appears until verification
- verification links can be consumed once
- login works with username and email address
- failed login feedback remains generic
- forgot-password feedback remains generic
- verified accounts receive reset links
- unverified accounts receive verification links instead of reset links
- password reset tokens are one-time use
- authenticated password change requires the current password
- morning plans persist and re-render
- blank morning saves do not create visible logs
- evening reflections persist and re-render
- blank evening saves do not create visible logs
- weekly review uses a Monday-Sunday range
- daily preview omits empty section headers
- blank daily preview shows a product empty state
- record library only shows meaningful saved entries
- record library keyword search works across reconstructed content
- Markdown export and PDF preview match the selected library criteria
- unknown routes render the product 404 page

## Release Notes Guidance

When writing release notes, group changes by product outcome:

- planning and reflection workflow
- long-term record exploration
- export and portability
- account security
- operations and deployment
- release QA and visual polish

Avoid listing every template or CSS class unless the audience is internal engineering.
