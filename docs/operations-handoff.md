# 운영 인수인계

이 문서는 Daymark 운영자가 AWS/GitHub 리소스 위치와 재배포 절차를 빠르게 확인하기 위한 안내입니다.

실제 secret 값은 이 문서에 적지 않습니다. 비밀번호, SMTP credential, remember-me secret, DB 접속 정보, webhook URL은 AWS 콘솔의 SSM Parameter Store 또는 Secrets Manager에서 확인하거나 재발급합니다.

## 운영 리소스 위치

| 항목 | 확인 위치 |
| --- | --- |
| 소스 코드 | GitHub `potterLim/daymark` |
| 배포 워크플로 | GitHub → `Actions` → `Deploy Production` |
| 배포 환경 변수 | GitHub → `Settings` → `Environments` → `production` |
| 컨테이너 이미지 | AWS 서울 리전 → Amazon ECR → `daymark` |
| 애플리케이션 서버 | AWS 서울 리전 → Amazon ECS → Express Mode → `daymark-production` |
| 운영 DB | AWS 서울 리전 → RDS → `daymark-production-db` |
| 운영 환경값 | AWS 서울 리전 → Systems Manager → Parameter Store → `/daymark/production` |
| 메일 발송 | AWS 서울 리전 → Amazon SES → Verified identities → `usedaymark.com` |
| DNS | AWS Route 53 → Hosted zones → `usedaymark.com` |
| 도메인 등록 | Namecheap → Domain List → `usedaymark.com` |
| 비용 알림 | AWS Billing and Cost Management → Budgets |
| 이상 비용 감지 | AWS Cost Management → Cost Anomaly Detection |
| 운영 로그 | ECS service events, CloudWatch Logs |

## 공개 접속 주소

운영 도메인:

```text
https://usedaymark.com
```

보조 도메인:

```text
https://www.usedaymark.com
```

상태 확인:

```text
https://usedaymark.com/actuator/health/readiness
```

정상 응답:

```json
{"status":"UP"}
```

## Secret 관리 원칙

GitHub에 저장해도 되는 것:

- 코드
- 공개 문서
- 예시 환경 변수 파일
- GitHub Actions 워크플로
- AWS 리소스 이름
- public URL

GitHub에 저장하면 안 되는 것:

- AWS access key
- DB 비밀번호
- SES SMTP username/password
- `DAYMARK_REMEMBER_ME_KEY`
- 실제 webhook URL
- `.env`
- 운영 로그
- DB dump와 백업 파일
- 화면 캡처

값을 잃어버렸을 때:

- GitHub Secret은 원문을 다시 볼 수 없으므로 새 값으로 재등록합니다.
- RDS 비밀번호는 원문 확인이 아니라 재설정으로 복구합니다.
- SES SMTP credential은 필요하면 새로 발급합니다.
- remember-me secret을 바꾸면 기존 remember-me 로그인은 무효화될 수 있습니다.

## 코드 수정부터 재배포까지

### 1. 최신 코드 받기

```bash
cd /Users/potterlim/Developments/Projects/daymark
git status
git pull --ff-only
```

의도하지 않은 변경이 있으면 먼저 내용을 확인합니다.

### 2. IntelliJ에서 수정

프로젝트 경로:

```text
/Users/potterlim/Developments/Projects/daymark
```

수정 기준:

- Java 코드는 코딩 표준을 따릅니다.
- 이미 운영 DB에 적용된 Flyway 파일은 수정하지 않습니다.
- DB 변경은 새 `V숫자__설명.sql`로 추가합니다.
- secret 값은 코드, 테스트, 문서에 직접 쓰지 않습니다.

### 3. 로컬 실행

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

브라우저:

```text
http://127.0.0.1:8080
```

### 4. 테스트

```bash
./gradlew test
```

Docker가 준비되어 있으면:

```bash
./gradlew mysqlIntegrationTest
```

배포 전 빌드:

```bash
./gradlew bootJar
```

### 5. 커밋과 푸시

```bash
git status
git add <changed-files>
git commit -m "type: concise message"
git push origin main
```

`main` push 후 GitHub Actions `Deploy Production`이 실행됩니다.

### 6. GitHub Actions 확인

확인 위치:

```text
GitHub → potterLim/daymark → Actions → Deploy Production
```

성공 조건:

- 테스트 성공
- JAR 빌드 성공
- Docker build 성공
- 서울 ECR push 성공

### 7. ECS 배포 확인

확인 위치:

```text
AWS Console → Amazon ECS → Express Mode → daymark-production
```

확인 항목:

- 서비스 상태가 `ACTIVE`인지 확인합니다.
- 새 revision이 최신 이미지 태그를 사용하는지 확인합니다.
- CloudWatch Logs에 시작 오류가 없는지 확인합니다.
- health check가 통과하는지 확인합니다.

## 운영 환경값 변경

환경값 기준 위치:

```text
AWS Systems Manager → Parameter Store → /daymark/production
```

일반 값은 String, 비밀번호와 secret은 SecureString으로 저장합니다. 환경값을 바꾼 뒤에는 ECS 서비스를 새 revision으로 업데이트해 컨테이너가 새 값을 읽게 합니다.

## 메일 확인

확인 위치:

```text
AWS Console → Amazon SES → Verified identities → usedaymark.com
```

확인 항목:

- Identity status
- DKIM status
- MAIL FROM status
- SPF/DMARC DNS record
- Production access status
- CloudWatch 또는 SES event에서 bounce/complaint 여부

SES sandbox 상태에서는 검증되지 않은 수신자에게 메일이 가지 않습니다.

## DB 확인

확인 위치:

```text
AWS Console → RDS → daymark-production-db
```

확인 항목:

- DB 상태
- endpoint
- 백업 보존 기간
- 보안 그룹 inbound rule
- storage 사용량
- 최근 connection/error metric

운영 DB를 직접 수정할 때는 먼저 스냅샷을 만들고, SQL을 실행한 뒤 주요 화면을 다시 확인합니다.

## 장애 대응 순서

1. `https://usedaymark.com/actuator/health/readiness`를 확인합니다.
2. ECS service event를 확인합니다.
3. CloudWatch Logs에서 애플리케이션 시작 실패를 확인합니다.
4. DB 연결 오류면 RDS 보안 그룹과 환경값을 확인합니다.
5. 메일 오류면 SES identity, sandbox, SMTP credential을 확인합니다.
6. 도메인 오류면 Route 53, ACM 인증서, ALB listener certificate, Host header rule, ALB 대상 상태를 확인합니다.
7. 새 배포 오류면 ECR image tag와 ECS revision을 확인합니다.

## 비용 확인

확인 위치:

```text
AWS Billing and Cost Management → Budgets
AWS Cost Management → Cost Explorer
AWS Cost Management → Cost Anomaly Detection
```

월 비용 알림은 10달러 단위로 확인합니다. ALB, RDS, Fargate, CloudWatch Logs가 초기 비용의 대부분입니다.
