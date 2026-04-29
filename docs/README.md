# 문서 안내

이 디렉터리는 Daymark의 공개 문서를 모아둔 곳입니다. 문서는 현재 코드와 실제 제품 흐름을 기준으로 유지합니다.

## 읽는 순서

1. [프로젝트 구조](project-architecture.md)
2. [배포 가이드](deployment.md)
3. [운영 인수인계](operations-handoff.md)
4. [출시 점검표](release-readiness.md)

## 문서별 역할

| 문서 | 내용 |
| --- | --- |
| [프로젝트 구조](project-architecture.md) | 코드 구조, 주요 패키지, 데이터 저장 방식, 핵심 요청 흐름 |
| [배포 가이드](deployment.md) | 서울 ECS Express Mode, RDS, Google OAuth, Route 53, 환경 변수 |
| [운영 인수인계](operations-handoff.md) | AWS/GitHub 운영 리소스 위치, 수정-빌드-재배포 절차 |
| [출시 점검표](release-readiness.md) | 출시 전 자동 테스트, 화면 점검, 내보내기 검증, 최종 체크리스트 |

## 관리 원칙

- 공개 문서는 한글로 작성합니다.
- 코드, 명령어, 환경 변수 이름은 실제 값과 같게 유지합니다.
- 캡처 이미지, 생성된 PDF/Markdown, 로그, 임시 테스트 자료는 커밋하지 않습니다.
- 개인 작업 메모가 필요하면 저장소 밖이나 Git에서 무시되는 로컬 경로를 사용합니다.
