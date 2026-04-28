# ECS Express Mode 이전 계획

이 문서는 Daymark를 도쿄 App Runner에서 서울 ECS Express Mode로 이전할 때의 기준 절차를 정리합니다. 목표는 앱과 데이터베이스를 서울 리전에 두고, 이미 승인 요청이 진행 중인 도쿄 SES는 그대로 사용하는 것입니다.

## 이전 결정

| 항목 | 결정 |
| --- | --- |
| 애플리케이션 실행 | ECS Express Mode |
| 앱 리전 | `ap-northeast-2` 서울 |
| 운영 DB | RDS MySQL, `ap-northeast-2` 서울 |
| 컨테이너 이미지 | ECR `daymark`, `ap-northeast-2` 서울 |
| 메일 발송 | SES SMTP, `ap-northeast-1` 도쿄 |
| 공개 도메인 | `https://usedaymark.com` |
| 기존 App Runner | 서울 ECS 검증과 DNS 전환 완료 후 정리 |

App Runner는 기존 고객으로 계속 사용할 수 있지만 새 기능 추가가 예정되어 있지 않으므로, 장기 운영 기준은 ECS Express Mode로 둡니다.

## 전체 순서

1. 서울 리전에 ECR `daymark` 저장소를 준비합니다.
2. GitHub Actions의 `Push Seoul Image` 워크플로로 서울 ECR에 이미지를 올립니다.
3. 서울 리전에 RDS MySQL을 생성합니다.
4. ECS Express Mode 서비스를 서울 리전에 생성합니다.
5. ECS 환경 변수에 `ops/aws/ecs-express-env.example` 기준 값을 입력합니다.
6. ECS 기본 URL로 `/actuator/health/readiness`를 확인합니다.
7. 회원가입, 로그인, 기록 저장, 관리자 화면을 ECS 기본 URL에서 확인합니다.
8. Route 53에서 `usedaymark.com`을 ECS/ALB 대상으로 전환합니다.
9. 운영 도메인에서 다시 전체 흐름을 확인합니다.
10. 충분히 안정화된 뒤 기존 App Runner와 도쿄 RDS 정리를 결정합니다.

리소스 생성, DNS 전환, 기존 리소스 삭제는 비용과 서비스 영향이 있으므로 작업 직전에 반드시 한 번 더 확인합니다.

## 리전 구성

서울 리전에 둘 것:

- ECS Express Mode 서비스
- RDS MySQL
- ECR `daymark`
- ALB, Target Group, 보안 그룹, Auto Scaling
- CloudWatch Logs

도쿄 리전에 유지할 것:

- SES verified identity `usedaymark.com`
- SES DKIM/SPF/DMARC/MAIL FROM 관련 DNS 레코드
- SES SMTP credentials
- SES production access 요청과 승인 상태

서울 ECS에서 도쿄 SES로 SMTP 연결을 사용합니다.

```text
SPRING_MAIL_HOST=email-smtp.ap-northeast-1.amazonaws.com
SPRING_MAIL_PORT=587
```

## GitHub Actions

기존 `Deploy Production` 워크플로는 현재 App Runner 운영을 위해 도쿄 ECR에 이미지를 푸시합니다. 서울 이전 중에는 기존 운영을 깨지 않기 위해 그대로 둡니다.

서울 ECR 이미지는 수동 워크플로로 푸시합니다.

```text
Actions → Push Seoul Image → Run workflow
```

이 워크플로는 `ap-northeast-2`의 ECR `daymark` 저장소에 다음 태그를 푸시합니다.

```text
daymark:<commit-sha>
daymark:latest
```

GitHub OIDC Role의 ECR 권한은 서울 ECR 저장소에도 접근할 수 있어야 합니다. 현재 권한이 도쿄 ECR만 허용한다면 IAM 정책에 서울 ECR `daymark` 저장소 ARN을 추가합니다.

## ECS 환경 변수

ECS Express Mode 서비스에는 `ops/aws/ecs-express-env.example`을 기준으로 값을 넣습니다. 예시 값은 그대로 사용하지 않습니다.

특히 아래 값은 서울/도쿄가 섞이므로 헷갈리지 않게 확인합니다.

| 변수 | 리전 기준 |
| --- | --- |
| `DATABASE_URL` | 서울 RDS |
| `SPRING_MAIL_HOST` | 도쿄 SES |
| `SPRING_MAIL_USERNAME` | 도쿄 SES SMTP credential |
| `SPRING_MAIL_PASSWORD` | 도쿄 SES SMTP credential |
| `DAYMARK_PUBLIC_BASE_URL` | `https://usedaymark.com` |

`production` 프로필에서는 SMTP, HTTPS 공개 URL, 보안 쿠키, remember-me key 검증이 켜져 있으므로 값이 부족하면 애플리케이션이 시작되지 않습니다.

## 네트워크와 보안

권장 기준:

- ECS 서비스는 퍼블릭 HTTPS 요청만 ALB를 통해 받습니다.
- RDS는 퍼블릭 접근을 끕니다.
- RDS 보안 그룹은 ECS 서비스에서 오는 MySQL 트래픽만 허용합니다.
- ECS 태스크는 도쿄 SES SMTP endpoint로 outbound 연결이 가능해야 합니다.
- 세션 쿠키와 remember-me 쿠키는 운영에서 Secure를 유지합니다.

## 검증 체크리스트

ECS 기본 URL에서 확인:

- `/actuator/health/readiness`가 `UP`인지
- 홈 화면이 열리는지
- 로그인하지 않은 상태에서 `Plan`, `Review`, `View Today`가 `/login`으로 정리되는지
- 회원가입과 로그인 흐름이 동작하는지
- 아침 계획, 저녁 회고, 주간 리뷰, 라이브러리가 동작하는지
- 관리자 계정만 `/admin/operations`에 접근 가능한지
- 인증/복구 메일의 링크가 `https://usedaymark.com` 기준으로 만들어지는지

도메인 전환 후 확인:

```bash
curl -sS https://usedaymark.com/actuator/health/readiness
curl -sS https://usedaymark.com/ | rg "Daymark"
```

DNS 캐시가 의심되면 공개 DNS와 권한 DNS를 따로 확인합니다.

```bash
dig @8.8.8.8 +short usedaymark.com
dig @1.1.1.1 +short usedaymark.com
dig @ns-402.awsdns-50.com +short usedaymark.com
```

## 롤백 기준

도메인 전환 전 문제가 생기면 ECS 리소스를 수정하고 다시 배포합니다. 기존 App Runner 운영 도메인은 그대로 유지합니다.

도메인 전환 후 문제가 생기면 Route 53을 기존 App Runner 대상으로 되돌립니다. 그 전까지 기존 App Runner와 도쿄 RDS는 삭제하지 않습니다.

데이터가 이미 서울 RDS에 쌓인 상태에서 롤백할 때는 데이터 기준점을 먼저 정합니다. 운영 데이터가 생긴 뒤에는 DB를 임의로 되돌리지 않습니다.

## 기존 리소스 정리

아래 정리는 서울 ECS 운영이 안정화된 뒤 별도 확인을 받고 진행합니다.

- App Runner `daymark-production` 중지 또는 삭제
- 도쿄 RDS 스냅샷 생성 후 삭제 여부 결정
- 도쿄 ECR 이미지 보존 정책 정리
- 문서와 workflow를 서울 ECS 기준으로 승격

삭제 작업은 되돌리기 어려우므로 작업 직전에 반드시 확인합니다.
