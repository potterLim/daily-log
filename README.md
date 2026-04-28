# Daymark

Daymark는 하루의 계획과 회고를 한 흐름으로 정리하는 개인 기록 웹 애플리케이션입니다.

아침에는 오늘의 기준을 세우고, 저녁에는 실행 결과를 체크하며, 쌓인 기록은 라이브러리에서 검색하고 내보낼 수 있습니다. 화면은 설명을 길게 읽히기보다 바로 사용할 수 있도록 간결한 문구와 안정적인 레이아웃을 기준으로 다듬었습니다.

## 핵심 기능

- 아침 계획: 목표, 집중 영역, 예상 변수를 날짜별로 기록
- 저녁 회고: 아침 목표를 체크리스트로 다시 확인하고 성과와 개선점을 정리
- 주간 리뷰: 월요일부터 일요일까지의 실행 흐름과 목표 달성률 확인
- 기록 라이브러리: 날짜 범위와 키워드로 장기 기록 탐색
- 내보내기: 선택한 기록을 Markdown으로 다운로드하거나 PDF 저장용 보고서로 확인
- 계정 보안: 회원가입, 로그인, 이메일 인증, 비밀번호 재설정, 비밀번호 변경
- 운영 지표: 관리자 전용 화면에서 활성 사용자, 인증 흐름, 내보내기 사용량 확인

## 기술 구성

- Java 17
- Spring Boot 3.5
- Spring MVC, Thymeleaf
- Spring Security
- Spring Data JPA
- Flyway
- MySQL
- H2 로컬 개발 프로필
- Gradle

## 빠른 실행

로컬에서는 별도 MySQL 없이 H2 메모리 데이터베이스로 바로 실행할 수 있습니다.

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

실행 후 브라우저에서 접속합니다.

```text
http://127.0.0.1:8080
```

테스트는 다음 명령으로 실행합니다.

```bash
./gradlew test
```

Docker가 준비되어 있다면 MySQL 통합 테스트도 실행할 수 있습니다.

```bash
./gradlew mysqlIntegrationTest
```

배포용 JAR는 다음 명령으로 생성합니다.

```bash
./gradlew bootJar
```

생성 결과:

```text
build/libs/daymark.jar
```

## 실행 설정

기본 프로필은 MySQL과 필수 보안 값을 요구합니다.

| 환경 변수 | 설명 |
| --- | --- |
| `DAYMARK_PUBLIC_BASE_URL` | 인증/복구 링크에 사용할 공개 HTTPS 주소 |
| `DATABASE_URL` | MySQL JDBC URL |
| `DATABASE_USERNAME` | MySQL 사용자 |
| `DATABASE_PASSWORD` | MySQL 비밀번호 |
| `DAYMARK_REMEMBER_ME_KEY` | remember-me 서명 키 |

운영 환경에서는 서울 리전 ECS Express Mode의 HTTPS 앞단 뒤에서 실행하고, 세션 쿠키/remember-me 쿠키 보안 설정과 SES SMTP 설정을 함께 준비합니다.

주요 선택 설정:

| 환경 변수 | 기본값 |
| --- | --- |
| `PORT` | `8080` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `false` |
| `DAYMARK_REMEMBER_ME_COOKIE_SECURE` | `false` |
| `DAYMARK_MAIL_FROM_ADDRESS` | `no-reply@daymark.local` |
| `DAYMARK_SUPPORT_CONTACT_EMAIL` | `potterLim0808@gmail.com` |
| `DAYMARK_ALERT_WEBHOOK_URL` | 없음 |
| `DAYMARK_WEEKLY_SUMMARY_ENABLED` | `false` |
| `DAYMARK_LOG_DIR` | `./logs` |

AWS 배포 설정은 [배포 문서](docs/deployment.md)를 확인하세요.

## 저장 구조

사용자 계정, 인증 토큰, 날짜별 기록은 MySQL에 저장됩니다. 기록 내용은 파일로 저장하지 않고 섹션별 텍스트 컬럼에 보관하며, 미리보기와 라이브러리, Markdown/PDF 내보내기는 저장된 섹션을 다시 조합해 생성합니다.

주요 테이블:

- `user_account`
- `user_email_verification_token`
- `user_password_reset_token`
- `daymark_entry`
- `operation_usage_event`
- `weekly_operation_metric_snapshot`

관리자 지표 화면은 `ADMIN` 권한 계정으로 로그인한 뒤 `/admin/operations`에서 확인합니다. 초기 운영자는 운영 DB에서 필요한 계정의 `user_role`을 `ADMIN`으로 승격해 부여합니다.

## 운영과 보안

- 비밀번호는 BCrypt로 저장합니다.
- 이메일 인증과 비밀번호 재설정 토큰은 해시로 저장하고 1회만 사용할 수 있습니다.
- 로그인 실패와 비밀번호 찾기 응답은 계정 존재 여부를 노출하지 않도록 일반화합니다.
- CSRF 보호와 HTTP-only 세션 쿠키를 사용합니다.
- 상태 확인 엔드포인트는 `/actuator/health`, `/actuator/health/liveness`, `/actuator/health/readiness`입니다.
- `.env.example`, `ops/aws/ecs-express-env.example`은 예시 값만 보관하고, 실제 운영 환경값은 AWS 콘솔/SSM Parameter Store/Secrets Manager/로컬 `.env`처럼 저장소 밖에서 관리합니다.
- 운영 지표 코드는 저장소에 포함하지만, 실제 통계 데이터와 운영 secret은 저장소에 커밋하지 않습니다.
- 로그, 백업, 캡처, 생성된 PDF/Markdown 파일은 저장소에 커밋하지 않습니다.

## 문서

- [문서 안내](docs/README.md)
- [프로젝트 구조](docs/project-architecture.md)
- [배포 가이드](docs/deployment.md)
- [운영 인수인계](docs/operations-handoff.md)
- [출시 점검표](docs/release-readiness.md)
