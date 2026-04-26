# Documentation Index

This directory contains the tracked project documents that should stay aligned with the committed codebase.

## Reading Order

1. [Project Architecture](project-architecture.md)
2. [Deployment Guide](deployment.md)
3. [Release Readiness](release-readiness.md)

## Included Documents

### [Project Architecture](project-architecture.md)

Use this document when you need to understand:

- how the application is structured
- which packages own which responsibilities
- how requests move from controller to service to persistence
- how Daymark sections are stored and reconstructed
- how the record library, search, trend, calendar, and export views are assembled
- which parts are the safest extension points

### [Deployment Guide](deployment.md)

Use this document when you need to:

- run the application as an executable JAR
- deploy with Docker Compose
- configure runtime environment variables
- understand production fail-fast checks, log collection, backup, and alerting minimums
- place the app behind a reverse proxy or load balancer
- wire health checks into a VM or container deployment

### [Release Readiness](release-readiness.md)

Use this document when you need to:

- perform final product QA before a release
- confirm the screen matrix that should be checked in Chrome
- verify export outputs and not-found states
- verify copy density, header alignment, library trend language, and premium reading surfaces
- keep screenshot evidence outside Git
- understand the release acceptance checklist

## Scope Rules

- Files in this directory are part of the repository and should be kept accurate.
- Local working notes and private onboarding documents belong outside this directory.
- If `local-docs/` exists in a workspace, it is intentionally treated as local-only material and is not part of the tracked repository documentation set.
- Screenshot captures, generated PDFs, generated Markdown exports, local logs, and temporary QA fixtures should not be committed.
