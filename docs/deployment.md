# 배포 가이드

Daymark의 운영 배포 기준은 서울 리전 ECS Express Mode입니다. 애플리케이션, 컨테이너 이미지, 데이터베이스, 메일 발송 리소스를 모두 `ap-northeast-2`에 두고, Route 53에서 `usedaymark.com`을 운영 도메인으로 연결합니다.

## 운영 결정

| 항목 | 결정 |
| --- | --- |
| 운영 리전 | `ap-northeast-2` 서울 |
| 애플리케이션 실행 | Amazon ECS Express Mode |
| 컨테이너 이미지 | Amazon ECR `daymark` |
| 배포 인증 | GitHub Actions OIDC 임시 권한 |
| 운영 DB | Amazon RDS for MySQL |
| 메일 | Amazon SES SMTP |
| 발신 주소 | `no-reply@usedaymark.com` |
| 공개 도메인 | `https://usedaymark.com` |
| HTTPS | ECS Express Mode ALB + ACM 인증서 |
| 운영 알림 | `DAYMARK_ALERT_WEBHOOK_URL` |
| 알 수 없는 공개 URL | 제품형 404 표시 |

운영 문서는 ECS Express Mode만 기준으로 합니다. 과거 배포 리소스가 남아 있더라도 새 배포와 운영 판단에는 사용하지 않습니다.

## 공개 가능한 운영 위치

아래 값은 공개 문서에 남겨도 되는 위치 정보입니다. AWS 계정 ID, RDS 엔드포인트, DB 비밀번호, SMTP credential, remember-me secret, webhook URL은 공개 문서에 적지 않습니다.

| 항목 | 값 |
| --- | --- |
| 도메인 | `usedaymark.com` |
| ECS 서비스 이름 | `daymark-production` |
| ECR 저장소 | `daymark` |
| RDS 식별자 | `daymark-production-db` |
| Route 53 Hosted Zone | `usedaymark.com` |
| SSM 파라미터 prefix | `/daymark/production` |

Namecheap의 `usedaymark.com` 네임서버는 Route 53 hosted zone의 네임서버로 위임합니다. 이후 DNS 레코드는 Namecheap이 아니라 Route 53에서 관리합니다.

## 배포 흐름

1. `main` 브랜치에 코드를 push합니다.
2. GitHub Actions `Deploy Production`이 테스트와 `bootJar`를 실행합니다.
3. Docker 이미지를 빌드해 서울 ECR `daymark` 저장소에 `latest`와 commit SHA 태그로 push합니다.
4. ECS Express Mode에서 새 이미지로 서비스를 업데이트합니다.
5. ECS 기본 URL에서 health check와 주요 화면을 확인합니다.
6. Route 53에서 `usedaymark.com`을 ECS Express Mode가 제공하는 ALB 대상으로 연결합니다.

## GitHub Actions

워크플로:

```text
.github/workflows/deploy-production.yml
```

GitHub `production` environment에는 아래 변수가 필요합니다.

| 변수 | 설명 |
| --- | --- |
| `AWS_ROLE_TO_ASSUME` | GitHub Actions가 assume할 AWS IAM role ARN |

장기 Access Key는 사용하지 않습니다. GitHub Actions는 OIDC로 배포 순간에만 임시 권한을 받습니다.

## AWS IAM

필요한 역할:

- `daymark-github-production-deploy`: GitHub Actions가 ECR에 이미지를 push할 때 사용
- `ecsTaskExecutionRole`: ECS가 ECR 이미지, CloudWatch Logs, SSM/Secrets 값을 읽을 때 사용
- `ecsInfrastructureRoleForExpressServices`: ECS Express Mode가 ALB, 보안 그룹, 오토스케일링 같은 기반 리소스를 만들 때 사용

GitHub OIDC trust policy와 ECR push policy 예시는 `ops/aws` 아래 템플릿을 기준으로 합니다.

## RDS MySQL

권장 운영값:

- 엔진: MySQL 8.0
- DB 식별자: `daymark-production-db`
- DB 이름: `daymark`
- 사용자: `daymark`
- 퍼블릭 접근: 비활성화
- 백업 보존: 7일 이상
- 보안 그룹: ECS 서비스 보안 그룹에서 오는 `3306`만 허용

운영 DB의 실제 endpoint와 비밀번호는 공개 문서에 적지 않습니다. 비밀번호는 AWS가 관리하는 secret 또는 SSM SecureString으로 관리합니다.

JDBC URL 형식:

```text
jdbc:mysql://<rds-endpoint>:3306/daymark?useSSL=true&requireSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

## SES 메일

SES도 서울 리전 `ap-northeast-2`를 기준으로 합니다.

필수 설정:

- SES domain identity: `usedaymark.com`
- Easy DKIM CNAME 3개
- custom MAIL FROM: `mail.usedaymark.com`
- MAIL FROM MX/TXT
- root SPF TXT
- DMARC TXT
- production access 요청
- SMTP credential 생성

권장 발신 주소:

```text
no-reply@usedaymark.com
```

SMTP endpoint:

```text
email-smtp.ap-northeast-2.amazonaws.com
```

SES sandbox 상태에서는 검증되지 않은 수신자에게 메일이 가지 않습니다. 실제 회원가입 인증 메일을 제품처럼 보내려면 production access 승인이 필요합니다.

## ECS 환경 변수

예시 파일:

```text
ops/aws/ecs-express-env.example
```

필수 일반 환경 변수:

| 이름 | 값 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DAYMARK_PUBLIC_BASE_URL` | `https://usedaymark.com` |
| `DATABASE_URL` | RDS JDBC URL |
| `DATABASE_USERNAME` | RDS 사용자 |
| `DAYMARK_REMEMBER_ME_COOKIE_SECURE` | `true` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` |
| `DAYMARK_MAIL_FROM_ADDRESS` | `no-reply@usedaymark.com` |
| `SPRING_MAIL_HOST` | `email-smtp.ap-northeast-2.amazonaws.com` |
| `SPRING_MAIL_PORT` | `587` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | `true` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | `true` |
| `DAYMARK_WEEKLY_SUMMARY_ENABLED` | `true` |
| `DAYMARK_WEEKLY_SUMMARY_ZONE` | `Asia/Seoul` |

필수 secret 환경 변수:

| 이름 | 권장 저장 위치 |
| --- | --- |
| `DATABASE_PASSWORD` | SSM SecureString 또는 Secrets Manager |
| `DAYMARK_REMEMBER_ME_KEY` | SSM SecureString |
| `SPRING_MAIL_USERNAME` | SSM SecureString |
| `SPRING_MAIL_PASSWORD` | SSM SecureString |
| `DAYMARK_ALERT_WEBHOOK_URL` | SSM SecureString |

`production` 프로필에서는 SMTP, 보안 쿠키, remember-me secret 같은 운영 필수값이 부족하면 애플리케이션이 시작되지 않도록 검증합니다.

## ECS Express Mode 생성값

권장 초기값:

| 항목 | 값 |
| --- | --- |
| Service name | `daymark-production` |
| Image | 서울 ECR `daymark:latest` |
| Container port | `8080` |
| Health check path | `/actuator/health/readiness` |
| CPU/Memory | 초기 소규모 트래픽 기준 최소값부터 시작 |
| Scaling | 초기 최소 1, 최대 2 |
| Network | RDS와 같은 VPC의 subnet/security group |

ECS Express Mode는 Fargate 기반 ECS 서비스, ALB, HTTPS, 오토스케일링, CloudWatch 로그를 함께 구성합니다. Express Mode 자체 추가 비용은 없고, 생성되는 Fargate, ALB, 로그, 데이터 전송 비용을 지불합니다.

## 도메인 연결

ECS Express Mode 서비스는 기본 `.on.aws` URL과 ALB를 생성합니다. 운영 도메인을 붙일 때는 ACM 인증서를 만든 뒤, ALB HTTPS listener에 인증서를 추가하고 listener rule의 Host header 조건에 운영 도메인을 추가합니다.

현재 운영 연결 방식:

- `usedaymark.com`: Route 53 A Alias로 ECS Express Mode ALB에 연결
- `www.usedaymark.com`: CNAME으로 같은 ALB DNS에 연결
- ALB listener Host header: ECS 기본 URL, `usedaymark.com`, `www.usedaymark.com`

`www.usedaymark.com`에 기존 CNAME이 있을 수 있으므로, `www`는 A Alias로 새로 만들기보다 기존 CNAME을 ALB DNS로 `UPSERT`하는 편이 안전합니다.

연결 후 아래 순서로 확인합니다.

```bash
dig @8.8.8.8 +short usedaymark.com
dig @1.1.1.1 +short usedaymark.com
curl -I https://usedaymark.com/
curl -I https://www.usedaymark.com/
curl -sS https://usedaymark.com/actuator/health/readiness
```

정상 health 응답:

```json
{"status":"UP"}
```

## 배포 후 기능 확인

- 홈 화면
- 로그인, 회원가입, 이메일 인증, 비밀번호 재설정
- 아침 계획 저장
- 저녁 회고 저장
- 오늘 기록 보기
- 주간 리뷰
- 기록 라이브러리 검색
- Markdown 다운로드
- PDF 저장용 미리보기
- 계정 화면과 비밀번호 변경
- 관리자 운영 지표
- 404 화면

## 장애 대응

- 앱이 뜨지 않으면 ECS service event와 CloudWatch Logs를 먼저 확인합니다.
- DB 연결 오류는 RDS endpoint, 보안 그룹, `DATABASE_URL`, `DATABASE_PASSWORD`를 확인합니다.
- 메일 오류는 SES identity, production access, SMTP credential, `SPRING_MAIL_HOST`를 확인합니다.
- 도메인 오류는 Route 53 record, ALB target, ACM 인증서 상태를 확인합니다.
- 새 화면이 안 보이면 ECR 이미지 태그와 ECS 배포 revision이 최신인지 확인합니다.

## 공개 저장소 주의

GitHub에 커밋하면 안 되는 값:

- AWS access key
- RDS endpoint가 포함된 실제 JDBC URL
- DB 비밀번호
- SES SMTP username/password
- remember-me secret
- 실제 webhook URL
- 운영 DB dump, 로그, 백업 파일
- 화면 캡처와 내보내기 결과물
