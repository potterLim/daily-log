# 프로젝트 구조

Daymark는 Spring Boot 기반의 서버 렌더링 웹 애플리케이션입니다. 핵심 흐름은 아침 계획, 저녁 회고, 주간 리뷰, 기록 라이브러리, Markdown/PDF 내보내기입니다.

사용자 기록은 파일이 아니라 데이터베이스에 저장합니다. 화면에 필요한 미리보기와 내보내기 결과는 저장된 섹션 데이터를 다시 조합해 만듭니다.

## 전체 흐름

```text
브라우저
  -> Spring MVC + Thymeleaf
  -> Controller
  -> Service
  -> Repository
  -> MySQL
```

## 저장소 구조

```text
.
├─ build.gradle
├─ compose.yaml
├─ Dockerfile
├─ docs
├─ ops
│  └─ backup
├─ src
│  ├─ main
│  │  ├─ java/com/potterlim/daymark
│  │  └─ resources
│  └─ test
└─ settings.gradle
```

## 주요 패키지

```text
com.potterlim.daymark
├─ config
├─ controller
├─ dto
├─ entity
├─ repository
├─ security
├─ service
└─ support
```

| 패키지 | 역할 |
| --- | --- |
| `config` | 애플리케이션 설정, 보안 설정, 운영 준비 상태 검증 |
| `controller` | HTTP 요청 처리와 화면 모델 구성 |
| `dto` | 폼 입력, 화면 출력, 서비스 명령 객체 |
| `entity` | JPA 엔티티와 도메인 값 객체 |
| `repository` | Spring Data JPA 저장소 |
| `security` | Spring Security 사용자 조회 |
| `service` | 계정, 인증 메일, 기록 저장, 라이브러리, 운영 요약 로직 |
| `support` | 섹션 타입, 간단한 Markdown 렌더링 등 공통 지원 코드 |

## 리소스 구조

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

## 주요 화면과 라우트

| 영역 | 라우트 | 설명 |
| --- | --- | --- |
| 홈 | `/` | 제품 홈 |
| 인증 | `/login`, `/register`, `/forgot-password`, `/reset-password`, `/verify-email` | 로그인, 가입, 인증, 복구 |
| 계정 | `/account/password`, `/account/email-verification/resend` | 비밀번호 변경, 인증 메일 재전송 |
| 아침 계획 | `/daymark/morning`, `/daymark/morning/edit`, `/daymark/morning/save` | 아침 기록 목록, 편집, 저장 |
| 저녁 회고 | `/daymark/evening`, `/daymark/evening/edit`, `/daymark/evening/save` | 저녁 기록 목록, 편집, 저장 |
| 주간 리뷰 | `/daymark/week` | 월요일부터 일요일까지의 실행 요약 |
| 라이브러리 | `/daymark/library` | 날짜 범위와 키워드 기반 기록 탐색 |
| Markdown 내보내기 | `/daymark/library/export/markdown` | 선택한 기록 다운로드 |
| PDF 미리보기 | `/daymark/library/export/pdf` | 브라우저 PDF 저장용 보고서 |
| 기록 보기 | `/daymark/preview` | 날짜별 저장 기록 조회 |
| 상태 확인 | `/actuator/health/**` | 런타임 상태 확인 |

## 데이터 모델

주요 테이블:

- `user_account`: 사용자 계정, 비밀번호 해시, 이메일 인증 상태
- `user_email_verification_token`: 이메일 인증용 1회성 토큰
- `user_password_reset_token`: 비밀번호 재설정용 1회성 토큰
- `daymark_entry`: 사용자와 날짜 기준의 하루 기록

`daymark_entry`는 하루 기록을 하나의 Markdown 문자열로 저장하지 않습니다. 목표, 집중 영역, 예상 변수, 저녁 체크리스트, 성과, 개선점, 감사, 메모를 섹션별 텍스트로 저장합니다.

이 구조 덕분에 다음 동작을 안정적으로 처리할 수 있습니다.

- 빈 저장은 실제 기록으로 보이지 않음
- 미리보기에서 빈 섹션 제목을 숨김
- 라이브러리 검색과 내보내기가 같은 저장 데이터를 사용함
- 사용자별 기록이 데이터베이스 수준에서 분리됨

## 핵심 서비스

| 서비스 | 역할 |
| --- | --- |
| `UserAccountService` | 회원가입, 중복 확인, 비밀번호 변경과 재설정 |
| `EmailVerificationTokenService` | 이메일 인증 토큰 생성, 검증, 소비 |
| `PasswordResetTokenService` | 비밀번호 재설정 토큰 생성, 검증, 소비 |
| `AuthenticationMailWorkflowService` | 인증/복구 메일 흐름과 실패 알림 연결 |
| `DaymarkService` | 날짜별 기록 읽기, 쓰기, 주간 상태 계산 |
| `DaymarkLibraryService` | 장기 기록 검색, 타임라인, 추세, 캘린더, Markdown 내보내기 |
| `WeeklyOperationsSummaryService` | 운영용 주간 지표 계산 |

## 주요 요청 흐름

### 회원가입과 인증

1. 사용자가 이름, 이메일, 비밀번호를 입력합니다.
2. 서버가 입력값과 중복 계정을 검증합니다.
3. 계정을 생성하고 이메일 인증 토큰을 발급합니다.
4. 인증 링크를 메일로 보내고 사용자를 로그인 상태로 전환합니다.
5. 인증 링크가 소비되면 인증 배너가 사라집니다.

### 비밀번호 복구

1. 사용자는 이메일을 입력합니다.
2. 화면에는 항상 같은 성공 메시지를 보여 계정 존재 여부를 숨깁니다.
3. 인증된 계정이면 비밀번호 재설정 링크를 보냅니다.
4. 미인증 계정이면 인증 링크를 다시 보냅니다.
5. 재설정 토큰은 한 번만 사용할 수 있습니다.

### 아침 계획

1. 날짜를 선택합니다.
2. 목표, 집중 영역, 예상 변수를 입력합니다.
3. 서버가 내용을 정리해 섹션별로 저장합니다.
4. 실제 내용이 없는 저장은 기록으로 집계하지 않습니다.

### 저녁 회고

1. 같은 날짜의 아침 계획을 읽기 좋은 카드로 보여줍니다.
2. 아침 목표를 체크리스트로 변환합니다.
3. 완료 여부와 회고 내용을 저장합니다.
4. 저장 후 주간 리뷰와 라이브러리에서 같은 데이터를 사용합니다.

### 기록 라이브러리와 내보내기

1. 기본 최근 90일 범위를 구성합니다.
2. 날짜 범위와 키워드 조건으로 기록을 조회합니다.
3. 의미 있는 내용이 있는 기록만 타임라인에 표시합니다.
4. 같은 조건으로 Markdown 다운로드와 PDF 미리보기를 생성합니다.

## 보안 기준

- 정적 파일, 인증 화면, 이메일 인증, 상태 확인, 알 수 없는 공개 URL의 404 화면은 로그인 없이 접근할 수 있습니다.
- 제품 기능의 실제 라우트는 로그인 후 접근할 수 있습니다.
- 비밀번호는 BCrypt로 저장합니다.
- 인증/재설정 토큰은 원문이 아니라 해시로 저장합니다.
- 로그인 실패와 비밀번호 찾기 응답은 일반화합니다.
- CSRF 보호를 유지합니다.
- 세션 쿠키는 HTTP-only와 `SameSite=Lax`를 사용합니다.
- 운영에서는 세션 쿠키와 remember-me 쿠키 모두 Secure를 강제합니다.

## 실행 프로필

| 프로필 | 용도 |
| --- | --- |
| 기본 | MySQL 기반 실행. 필수 환경 변수가 없으면 시작하지 않음 |
| `local` | H2 메모리 DB와 진단용 인증/복구 링크 로그. 로컬 개발용 |
| `production` | 운영 준비 상태 검증, 보안 쿠키 기본값, 주간 운영 요약 활성화 |

## 테스트 범위

주요 테스트는 실제 웹 흐름을 중심으로 구성되어 있습니다.

- 회원가입, 로그인, 이메일 인증
- 비밀번호 찾기, 재설정, 변경
- 아침 계획과 빈 저장 방지
- 저녁 회고와 체크리스트 저장
- 월요일부터 일요일까지의 주간 리뷰
- 기록 미리보기와 빈 섹션 숨김
- 라이브러리 검색, Markdown 내보내기, PDF 미리보기
- 제품형 404 화면
- 상태 확인 엔드포인트

주요 테스트 파일:

- `DaymarkApplicationTests`
- `WebFlowIntegrationTests`
- `MySqlIntegrationTests`
- `WeeklyOperationsSummaryServiceTests`

## 확장 시 주의할 부분

다음 영역은 저장 데이터, 검색, 내보내기에 함께 영향을 줄 수 있으므로 신중히 변경합니다.

- 섹션 타입과 순서
- Markdown 헤더 문구
- 목표 목록 정규화 방식
- 날짜 범위 계산
- 사용자와 날짜의 유일성 제약
- 내보내기 파일명과 응답 헤더
