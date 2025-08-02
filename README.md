# DailyLog

DailyLog는 하루의 목표와 회고를 기록하고, 주간 단위로 통계를 확인할 수 있는 ASP.NET Core 기반 웹 애플리케이션입니다.

## 주요 기능

- 사용자는 로그인 후 아침/저녁 일지를 작성할 수 있습니다.
- 작성된 일지는 Markdown 파일 형태로 사용자별 디렉토리에 저장됩니다.
- 일지 내용을 바탕으로 주간 목표 달성률이 시각화됩니다.

## 기술 개요

### 프레임워크 및 구조

- ASP.NET Core 8 기반의 웹 애플리케이션
- MVC와 Razor Pages 혼합 사용
- `Program.cs`에서 명시적으로 서비스 등록 및 미들웨어 구성

### 사용자 인터페이스

- Razor View + Partial View 기반 레이아웃
- Bootstrap, jQuery 활용
- `jquery-validation-unobtrusive`를 통해 클라이언트 측 유효성 검사 지원

### 인증 및 데이터베이스

- ASP.NET Identity + Entity Framework Core (Code-First)
  - 사용자 인증 및 계정 정보는 SQL Server에 저장
  - `ApplicationDbContext`를 통해 Identity 테이블 자동 관리
  - 이메일 인증/복잡한 패스워드 정책 등은 비활성화하여 간결한 인증 제공

### 데이터 처리

- 사용자의 아침/저녁 일지는 Markdown 파일로 저장됨
  - 저장 경로 구조: `logs/사용자ID/YYYY_MM_WeekN/YYYY_MM_DD_Morning.md`, `...Evening.md`
- 데이터베이스는 인증 정보만 담당하며, 일지 데이터는 파일 시스템 기반으로 관리
- `IDailyLogService` / `DailyLogService` 구조로 컨트롤러와 핵심 로직 분리
  - 일지 파일 생성, 수정, 삭제
  - Markdown 파싱 및 주간 목표 통계 계산

## 기여하기
오류를 발견했거나 수정 또는 개선을 제안하고 싶은 내용이 있다면 Pull Request 또는 Issue로 남겨주세요. 

피드백은 이 프로젝트를 더 나은 방향으로 발전시키는 데 큰 도움이 됩니다.

## 문의
프로젝트에 대해 궁금한 점이 있다면 potterLim0808@gmail.com 으로 연락주세요.
