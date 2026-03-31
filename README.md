# dayLog

`dayLog` is a web application for writing daily plans, reflections, and weekly progress summaries.

## Goals

- Provide a clean and focused daily logging experience.
- Keep the project structure practical for long-term maintenance.
- Follow `java-coding-standard.md` rules strictly during implementation.

## Stack

- Java 17
- Spring Boot 3.5.9
- Spring MVC + Thymeleaf
- Spring Security
- Spring Data JPA
- PostgreSQL
- Gradle

## Notes

- Authentication data will live in PostgreSQL.
- Daily log documents will remain file-based under the `logs` directory.
- This project is intentionally structured as a standard Gradle project so it opens naturally in IntelliJ IDEA.
