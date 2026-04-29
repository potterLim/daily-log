# 운영 인수인계

이 문서는 Daymark 운영자가 AWS/GitHub 리소스 위치와 재배포 절차를 빠르게 확인하기 위한 안내입니다.

실제 secret 값은 이 문서에 적지 않습니다. DB 비밀번호, Google OAuth secret, remember-me secret, webhook URL은 AWS 콘솔의 SSM Parameter Store 또는 Secrets Manager에서 확인하거나 재발급합니다.

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
| Google OAuth | Google Cloud Console → APIs & Services → Credentials |
| DNS | AWS Route 53 → Hosted zones → `usedaymark.com` |
| 도메인 등록 | Namecheap → Domain List → `usedaymark.com` |
| 비용 알림 | AWS Billing and Cost Management → Budgets |
| 이상 비용 감지 | AWS Cost Management → Cost Anomaly Detection |
| 운영 로그 | ECS service events, CloudWatch Logs |

## 비용 방어 운영값

Daymark 초기 운영은 정상 사용자 20명 안팎을 기준으로 합니다. 서버가 순간적으로 느려지거나 429 응답을 반환하는 것은 허용하고, 자동 확장으로 비용이 커지는 상황을 우선 방어합니다.

| 항목 | 기준 |
| --- | --- |
| ECS service task | 최소 1, 최대 1 |
| CloudWatch Logs retention | 7일 |
| RDS storage | 20 GiB |
| RDS storage autoscaling | 끄기 |
| RDS storage 알림 | 70%, 85% |
| RDS backup retention | 7일 |
| 로그인 제한 | IP 10회/10분, Workspace ID 5회/10분 |
| Google 로그인 시작 제한 | IP 20회/10분 |
| Workspace 생성 제한 | IP 5회/1시간 |
| 기록 저장 제한 | 사용자 30회/10분 |
| 내보내기 제한 | 사용자 10회/10분, 50회/1일 |
| 일반 화면 조회 제한 | IP 120회/1분 |
| 기록 입력 제한 | 한 번 저장 기준 8,000자 |

AWS 콘솔 확인 위치:

- ECS 최대 task 수: AWS 서울 리전 → Amazon ECS → `daymark-production` → service scaling
- 로그 보관: AWS 서울 리전 → CloudWatch → Log groups → Daymark log group → Retention `7 days`
- RDS 저장공간: AWS 서울 리전 → RDS → `daymark-production-db` → Storage autoscaling disabled
- 비용 알림: AWS Billing and Cost Management → Budgets → 10달러, 20달러, 30달러, 50달러, 100달러 알림

앱 내부 rate limit은 단일 ECS task 기준으로 동작합니다. 초기에는 최대 task 수를 1로 고정하기 때문에 이 방식이 비용 없이 가장 단순합니다. task 수를 늘리는 시점에는 AWS WAF 또는 공유 저장소 기반 rate limit을 다시 검토합니다.

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
- 운영자 Workspace ID 목록

GitHub에 저장하면 안 되는 것:

- AWS access key
- DB 비밀번호
- Google OAuth client secret
- `DAYMARK_REMEMBER_ME_KEY`
- 실제 webhook URL
- `.env`
- 운영 로그
- DB dump와 백업 파일
- 화면 캡처

값을 잃어버렸을 때:

- GitHub Secret은 원문을 다시 볼 수 없으므로 새 값으로 재등록합니다.
- RDS 비밀번호는 원문 확인이 아니라 재설정으로 복구합니다.
- Google OAuth client secret은 Google Cloud Console에서 새 secret을 발급합니다.
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

로컬에서 Google 로그인을 확인하려면 `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`을 환경 변수로 넣고 Google OAuth redirect URI에 아래 주소를 등록합니다.

```text
http://127.0.0.1:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/google
```

### 4. 테스트와 빌드

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
- ECS service 업데이트 성공
- ECS service stable 대기 성공

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

## Google OAuth 확인

확인 위치:

```text
Google Cloud Console → APIs & Services → Credentials
```

운영 redirect URI:

```text
https://usedaymark.com/login/oauth2/code/google
https://www.usedaymark.com/login/oauth2/code/google
```

확인 항목:

- OAuth client type이 Web application인지 확인합니다.
- 운영 redirect URI가 정확히 등록되어 있는지 확인합니다.
- `GOOGLE_CLIENT_ID`와 `GOOGLE_CLIENT_SECRET`이 ECS 환경값에 들어갔는지 확인합니다.
- Google 로그인 후 신규 사용자가 Workspace 생성 화면으로 이동하는지 확인합니다.
- 기존 사용자가 Google 로그인으로 바로 홈에 들어가는지 확인합니다.

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
5. Google 로그인 오류면 redirect URI, client ID, client secret, HTTPS 도메인을 확인합니다.
6. 도메인 오류면 Route 53, ACM 인증서, ALB listener certificate, Host header rule, ALB `80 -> 443` 리디렉션, ALB 대상 상태를 확인합니다.
7. 새 배포 오류면 ECR image tag와 ECS revision을 확인합니다.

공용 DNS는 정상인데 특정 기기에서만 `usedaymark.com`이 Namecheap URL Forward로 보이면, 그 기기의 DNS 캐시가 이전 값을 들고 있는 상태입니다.

## 비용 확인

확인 위치:

```text
AWS Billing and Cost Management → Budgets
AWS Cost Management → Cost Explorer
AWS Cost Management → Cost Anomaly Detection
```

월 비용 알림은 10달러 단위로 확인합니다. ALB, RDS, Fargate, CloudWatch Logs가 초기 비용의 대부분입니다.
