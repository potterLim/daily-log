# Daymark

Daymark는 매일의 목표를 세우고 하루를 돌아보는 개인 워크스페이스입니다.

아침에는 오늘의 기준을 세우고, 저녁에는 실행 결과를 확인합니다. 쌓인 기록은 라이브러리에서 검색하고 Markdown 또는 PDF로 정리할 수 있습니다. 화면은 설명보다 행동이 먼저 보이도록 간결한 문구와 안정적인 레이아웃을 기준으로 구성합니다.

## 핵심 기능

- 아침 계획: 목표, 집중 영역, 예상 변수를 날짜별로 기록
- 저녁 회고: 아침 목표를 체크리스트로 확인하고 성과와 개선점을 정리
- 주간 리뷰: 월요일부터 일요일까지의 실행 흐름과 목표 달성률 확인
- 기록 라이브러리: 날짜 범위와 키워드로 장기 기록 탐색
- 내보내기: 선택한 기록을 Markdown으로 다운로드하거나 PDF 저장용 보고서로 확인
- 계정: Google 계정 확인, Workspace ID, 비밀번호 로그인, 비밀번호 변경
- 운영 지표: 관리자 전용 화면에서 활성 사용자, 기록 흐름, 내보내기 사용량 확인

## 기술 구성

- Java 17
- Spring Boot 3.5
- Spring MVC, Thymeleaf
- Spring Security, OAuth2 Client
- Spring Data JPA
- Flyway
- MySQL
- H2 로컬 개발 프로필
- Gradle

## 빠른 실행

로컬에서는 별도 MySQL 없이 H2 메모리 데이터베이스로 실행할 수 있습니다.

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

접속 주소:

```text
http://127.0.0.1:8080
```

로컬에서 Google 로그인을 실제로 확인하려면 Google OAuth 클라이언트를 만들고 아래 값을 환경 변수로 넣습니다.

```bash
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

테스트:

```bash
./gradlew test
```

Docker가 준비되어 있다면 MySQL 통합 테스트도 실행할 수 있습니다.

```bash
./gradlew mysqlIntegrationTest
```

배포용 JAR:

```bash
./gradlew bootJar
```

생성 결과:

```text
build/libs/daymark.jar
```

## 주요 환경 변수

기본 프로필은 MySQL과 보안 값을 요구합니다.

| 환경 변수 | 설명 |
| --- | --- |
| `DAYMARK_PUBLIC_BASE_URL` | 공개 HTTPS 주소 |
| `DATABASE_URL` | MySQL JDBC URL |
| `DATABASE_USERNAME` | MySQL 사용자 |
| `DATABASE_PASSWORD` | MySQL 비밀번호 |
| `DAYMARK_REMEMBER_ME_KEY` | remember-me 서명 키 |
| `GOOGLE_CLIENT_ID` | Google OAuth 클라이언트 ID |
| `GOOGLE_CLIENT_SECRET` | Google OAuth 클라이언트 secret |

운영 환경에서는 서울 리전 ECS Express Mode의 HTTPS 앞단 뒤에서 실행합니다. 세션 쿠키와 remember-me 쿠키는 Secure로 설정하고, 실제 secret 값은 AWS SSM Parameter Store 또는 Secrets Manager에서 관리합니다.

## 저장 구조

사용자 계정과 날짜별 기록은 MySQL에 저장됩니다. 기록 내용은 Markdown 파일이 아니라 섹션별 텍스트 컬럼으로 저장하며, 미리보기와 라이브러리, Markdown/PDF 내보내기는 같은 저장 데이터를 다시 조합해 생성합니다.

주요 테이블:

- `user_account`
- `daymark_entry`
- `operation_usage_event`
- `weekly_operation_metric_snapshot`

관리자 지표 화면은 `ADMIN` 권한 계정으로 로그인한 뒤 `/admin/operations`에서 확인합니다. 초기 운영자는 운영 DB에서 필요한 계정의 `user_role`을 `ADMIN`으로 승격해 부여합니다.

## 운영과 보안

- 가입은 Google 계정 확인 후 Workspace ID와 비밀번호를 생성하는 흐름입니다.
- 로그인은 Google 로그인 또는 Workspace ID/이메일과 비밀번호로 진행합니다.
- 비밀번호는 BCrypt로 저장합니다.
- CSRF 보호와 HTTP-only 세션 쿠키를 사용합니다.
- 운영에서는 Secure 쿠키와 HTTPS를 전제로 합니다.
- 초기 운영 비용 방어를 위해 ECS 최대 task 수는 1, RDS는 20GiB 고정, CloudWatch Logs는 7일 보관을 기준으로 합니다.
- 로그인, Google 로그인 시작, Workspace 생성, 기록 저장, 내보내기는 앱 내부 rate limit으로 보호합니다.
- 기록 입력은 한 번 저장 기준 8,000자 안에서 제한합니다.
- 상태 확인 엔드포인트는 `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`입니다.
- `.env.example`, `ops/aws/ecs-express-env.example`은 예시 값만 보관하고 실제 운영 값은 저장소 밖에서 관리합니다.
- 로그, 백업, 캡처, 생성된 PDF/Markdown 파일은 저장소에 커밋하지 않습니다.

## 문서

- [문서 안내](docs/README.md)
- [프로젝트 구조](docs/project-architecture.md)
- [배포 가이드](docs/deployment.md)
- [운영 인수인계](docs/operations-handoff.md)
- [출시 점검표](docs/release-readiness.md)
