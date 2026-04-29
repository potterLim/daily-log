# 프로젝트 구조

Daymark는 Spring Boot 기반의 서버 렌더링 웹 애플리케이션입니다. 핵심 흐름은 Google 계정 확인, Workspace 생성, 아침 계획, 저녁 회고, 주간 리뷰, 기록 라이브러리, Markdown/PDF 내보내기입니다.

사용자 기록은 파일이 아니라 데이터베이스에 저장합니다. 화면 미리보기와 내보내기 결과는 저장된 섹션 데이터를 다시 조합해 만듭니다.

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
├─ src
│  ├─ main
│  │  ├─ java/com/potterlim/daymark
│  │  └─ resources
│  └─ test
└─ settings.gradle
```

## 주요 패키지

| 패키지 | 역할 |
| --- | --- |
| `config` | 애플리케이션 설정, 보안 설정, 운영 준비 상태 검증 |
| `controller` | HTTP 요청 처리와 화면 모델 구성 |
| `dto` | 폼 입력, 화면 출력, 서비스 명령 객체 |
| `entity` | JPA 엔티티와 도메인 값 객체 |
| `repository` | Spring Data JPA 저장소 |
| `security` | 사용자 조회, Google OAuth 성공 처리 |
| `service` | 계정, 기록 저장, 라이브러리, 운영 요약 로직 |
| `support` | 섹션 타입, Markdown 렌더링 등 공통 지원 코드 |

## 주요 화면과 라우트

| 영역 | 라우트 | 설명 |
| --- | --- | --- |
| 홈 | `/` | 제품 홈 |
| 인증 | `/login`, `/register`, `/forgot-password`, `/oauth2/authorization/google` | 로그인, 가입, 비밀번호 도움말, Google 확인 |
| 계정 | `/account`, `/account/password` | 계정 정보와 비밀번호 변경 |
| 아침 계획 | `/daymark/morning`, `/daymark/morning/edit`, `/daymark/morning/save` | 아침 기록 목록, 편집, 저장 |
| 저녁 회고 | `/daymark/evening`, `/daymark/evening/edit`, `/daymark/evening/save` | 저녁 기록 목록, 편집, 저장 |
| 주간 리뷰 | `/daymark/week` | 월요일부터 일요일까지의 실행 요약 |
| 라이브러리 | `/daymark/library` | 날짜 범위와 키워드 기반 기록 탐색 |
| Markdown 내보내기 | `/daymark/library/export/markdown` | 선택한 기록 다운로드 |
| PDF 미리보기 | `/daymark/library/export/pdf` | 브라우저 PDF 저장용 보고서 |
| 기록 보기 | `/daymark/preview` | 날짜별 저장 기록 조회 |
| 운영 지표 | `/admin/operations` | 관리자 전용 사용/기록/내보내기 지표 |
| 상태 확인 | `/actuator/health/**` | 런타임 상태 확인 |

## 계정 흐름

### 가입

1. 사용자가 `Create Account`를 선택합니다.
2. Google 계정으로 이메일 소유를 확인합니다.
3. 확인된 Google 이메일을 기준으로 Workspace ID와 비밀번호를 만듭니다.
4. 서버는 Workspace ID, 이메일, Google subject 중복을 검증합니다.
5. 계정을 생성하고 사용자를 로그인 상태로 전환합니다.

### 로그인

사용자는 두 방식으로 로그인할 수 있습니다.

- Google 로그인
- Workspace ID 또는 이메일과 비밀번호

Google 로그인 성공 시 이미 연결된 계정이면 바로 로그인합니다. 같은 이메일로 만든 기존 계정이 있으면 Google subject를 연결한 뒤 로그인합니다. 새 Google 계정이면 Workspace 생성 화면으로 이동합니다.

### 비밀번호 도움말

비밀번호를 잊은 사용자는 Google 로그인으로 Workspace에 다시 접근할 수 있습니다. 로그인 후 계정 화면에서 비밀번호를 변경합니다.

## 데이터 모델

주요 테이블:

- `user_account`: Workspace ID, 이메일, 비밀번호 해시, Google 연결 상태, 권한
- `daymark_entry`: 사용자와 날짜 기준의 하루 기록
- `operation_usage_event`: 로그인, 기록 저장, 라이브러리 조회, 내보내기 이벤트
- `weekly_operation_metric_snapshot`: 주간 운영 지표 스냅샷

`daymark_entry`는 하루 기록을 하나의 Markdown 문자열로 저장하지 않습니다. 목표, 집중 영역, 예상 변수, 저녁 체크리스트, 성과, 개선점, 감사, 메모를 섹션별 텍스트로 저장합니다.

이 구조 덕분에 다음 동작을 안정적으로 처리할 수 있습니다.

- 빈 저장은 실제 기록으로 보이지 않음
- 미리보기에서 빈 섹션 제목을 숨김
- 라이브러리 검색과 내보내기가 같은 저장 데이터를 사용함
- 사용자별 기록이 데이터베이스 수준에서 분리됨

## 핵심 서비스

| 서비스 | 역할 |
| --- | --- |
| `UserAccountService` | Workspace 생성, 중복 확인, Google 연결, 비밀번호 변경 |
| `GoogleOAuth2AuthenticationSuccessHandler` | Google 로그인 성공 후 기존 계정 로그인 또는 신규 가입 세션 생성 |
| `DaymarkService` | 날짜별 기록 읽기, 쓰기, 주간 상태 계산 |
| `DaymarkLibraryService` | 장기 기록 검색, 타임라인, 추세, 캘린더, Markdown 내보내기 |
| `OperationUsageEventService` | 운영 이벤트 저장 |
| `WeeklyOperationsSummaryService` | 운영용 주간 지표 계산 |
| `WeeklyOperationMetricSnapshotService` | 주간 운영 지표 저장과 조회 |

## 기록 흐름

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

- 정적 파일, 인증 화면, 상태 확인, 공개 404 화면은 로그인 없이 접근할 수 있습니다.
- 제품 기능의 실제 라우트는 로그인 후 접근할 수 있습니다.
- 운영 지표 화면은 `ADMIN` 권한 계정만 접근할 수 있습니다.
- 비밀번호는 BCrypt로 저장합니다.
- Google OAuth client secret, DB 비밀번호, remember-me secret은 저장소에 커밋하지 않습니다.
- CSRF 보호를 유지합니다.
- 세션 쿠키는 HTTP-only와 `SameSite=Lax`를 사용합니다.
- 운영에서는 세션 쿠키와 remember-me 쿠키 모두 Secure를 강제합니다.

## 실행 프로필

| 프로필 | 용도 |
| --- | --- |
| 기본 | MySQL 기반 실행. 필수 환경 변수가 없으면 시작하지 않음 |
| `local` | H2 메모리 DB와 로컬 화면 확인용 |
| `production` | 운영 준비 상태 검증, 보안 쿠키 기본값, 주간 운영 요약 활성화 |

## 테스트 범위

주요 테스트는 실제 웹 흐름을 중심으로 구성되어 있습니다.

- Google 확인 이후 Workspace 생성
- Workspace ID/이메일과 비밀번호 로그인
- 비밀번호 변경
- 아침 계획과 빈 저장 방지
- 저녁 회고와 체크리스트 저장
- 월요일부터 일요일까지의 주간 리뷰
- 기록 미리보기와 빈 섹션 숨김
- 라이브러리 검색, Markdown 내보내기, PDF 미리보기
- 제품형 404 화면
- 관리자 운영 지표
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
- Google OAuth redirect URI
- 내보내기 파일명과 응답 헤더
