# 운영 인수인계

이 문서는 Daymark 운영자가 배포 리소스, 민감 설정 위치, 재빌드와 재배포 절차를 빠르게 확인하기 위한 안내입니다.

비밀번호, SMTP 비밀번호, remember-me secret, DB 접속 비밀번호, AWS access key 같은 실제 secret 값은 이 문서에 적지 않습니다. 값이 필요하면 아래에 적힌 콘솔 위치에서 확인하거나, 확인할 수 없는 secret은 안전하게 재발급합니다.

## 운영 리소스 위치

| 항목 | 확인 위치 | 비고 |
| --- | --- | --- |
| 소스 코드 | GitHub `potterLim/daymark` 저장소 | 코드, 공개 문서, GitHub Actions 워크플로를 관리합니다. |
| 배포 워크플로 | GitHub 저장소 `Actions` 탭의 `Deploy Production` | `main` push 또는 수동 실행으로 테스트, JAR 빌드, Docker 이미지 빌드, ECR push를 수행합니다. |
| GitHub 배포 환경 | GitHub 저장소 `Settings` → `Environments` → `production` | `AWS_ROLE_TO_ASSUME` 같은 배포 환경 변수를 확인합니다. Secret 값은 생성 후 다시 볼 수 없으므로 필요하면 재등록합니다. |
| 컨테이너 이미지 | AWS Console → Amazon ECR → `daymark` repository | 커밋 SHA 태그와 `latest` 태그가 저장됩니다. |
| 애플리케이션 서버 | AWS Console → App Runner → `daymark-production` service | 서비스 상태, 배포 이력, 환경 변수, 도메인, 로그 연결을 확인합니다. |
| 운영 DB | AWS Console → RDS → Databases → `daymark-production-db` | DB 데이터는 GitHub에 없습니다. 비밀번호는 콘솔에 원문으로 다시 표시되지 않으므로 분실 시 재설정합니다. |
| 운영 환경값 | AWS Console → Systems Manager → Parameter Store → `/daymark/production` | DB URL, 사용자명, secret 등 운영 환경값의 기준 위치입니다. SecureString은 복호화 권한이 있어야 값 확인이 가능합니다. |
| 런타임 환경 변수 | AWS Console → App Runner → `daymark-production` → Configuration | App Runner가 실제 실행에 사용하는 값입니다. SSM 값과 일치하는지 확인합니다. |
| 도메인 DNS | AWS Console → Route 53 → Hosted zones → `usedaymark.com` | App Runner 연결, SES 인증, DKIM, SPF, DMARC 레코드를 관리합니다. |
| 도메인 등록기관 | Namecheap → Domain List → `usedaymark.com` | 네임서버가 Route 53 네임서버로 지정되어 있는지 확인합니다. |
| 메일 발송 | AWS Console → Amazon SES → Verified identities → `usedaymark.com` | SES identity, DKIM, MAIL FROM, sandbox 해제 상태를 확인합니다. |
| 비용 알림 | AWS Console → Billing and Cost Management → Budgets → `Daymark Monthly Cost Guard` | 월간 실제 비용이 10달러 단위로 넘어갈 때 알림을 보냅니다. |
| 이상 비용 감지 | AWS Console → Cost Management → Cost Anomaly Detection | AWS가 자동 구성한 이상 비용 감지 상태를 확인합니다. |
| 운영 로그 | AWS Console → App Runner 또는 CloudWatch Logs | 애플리케이션 시작 실패, health check 실패, 요청 로그를 확인합니다. |

## 현재 공개 접속 주소

임시 App Runner 주소:

```text
https://xefgmam2t3.ap-northeast-1.awsapprunner.com
```

운영 도메인:

```text
https://usedaymark.com
```

도메인 인증과 DNS 전파가 끝나기 전에는 임시 App Runner 주소로 먼저 상태를 확인합니다.

상태 확인 주소:

```text
https://usedaymark.com/actuator/health/readiness
https://xefgmam2t3.ap-northeast-1.awsapprunner.com/actuator/health/readiness
```

정상 응답 예:

```json
{"status":"UP"}
```

## Secret 관리 원칙

실제 secret 값은 GitHub 코드나 문서에 저장하지 않습니다.

GitHub에 저장해도 되는 것:

- 코드
- 공개 문서
- 예시 환경 변수 파일
- GitHub Actions 워크플로
- AWS 리소스 이름과 public URL

GitHub에 저장하면 안 되는 것:

- DB 비밀번호
- SES SMTP 사용자명과 비밀번호
- `DAYMARK_REMEMBER_ME_KEY`
- 실제 webhook URL
- AWS access key
- `.env`
- 운영 로그, DB dump, 백업 파일, 캡처 이미지

값을 잃어버렸을 때의 원칙:

- GitHub Secret은 원문을 다시 볼 수 없으므로 새 값으로 재등록합니다.
- RDS 비밀번호는 원문 확인이 아니라 재설정으로 복구합니다.
- SES SMTP 비밀번호는 필요하면 새 SMTP credential을 만듭니다.
- remember-me secret을 바꾸면 기존 remember-me 로그인은 무효화될 수 있습니다.

## 코드 수정부터 재배포까지

### 1. 최신 코드 받기

```bash
cd /Users/potterlim/Developments/Projects/daymark
git status
git pull --ff-only
```

`git status`에 내가 의도하지 않은 변경이 있으면 먼저 내용을 확인합니다.

### 2. IntelliJ에서 수정

IntelliJ에서 아래 폴더를 프로젝트로 엽니다.

```text
/Users/potterlim/Developments/Projects/daymark
```

수정 전 기준:

- Java 코드는 코딩 표준을 지킵니다.
- 이미 운영 DB에 적용된 Flyway 마이그레이션 파일은 수정하지 않습니다.
- DB 구조 변경은 새 `V숫자__설명.sql` 파일로 추가합니다.
- secret 값은 코드, 테스트, 문서에 직접 쓰지 않습니다.

### 3. 로컬 실행 확인

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

브라우저에서 확인합니다.

```text
http://127.0.0.1:8080
```

확인할 기본 화면:

- 홈
- 로그인
- 회원가입
- 아침 계획
- 저녁 회고
- 주간 리뷰
- 기록 라이브러리
- 계정
- 관리자 지표
- 404

### 4. 테스트 실행

일반 테스트:

```bash
./gradlew test
```

DB/Flyway/JPA 쪽을 수정했다면 Docker를 켠 뒤 MySQL 통합 테스트도 실행합니다.

```bash
./gradlew mysqlIntegrationTest
```

### 5. 배포용 JAR 빌드

```bash
./gradlew bootJar
```

생성 결과:

```text
build/libs/daymark.jar
```

### 6. 변경 내용 검토

```bash
git status
git diff
```

확인할 것:

- `.env`, 로그, 백업, 캡처, PDF/Markdown 내보내기 파일이 포함되지 않았는지 확인합니다.
- 실제 DB 비밀번호, SMTP 비밀번호, access key가 diff에 없는지 확인합니다.
- 문서에는 secret 값이 아니라 확인 위치만 적었는지 확인합니다.

### 7. 커밋

```bash
git add .
git commit -m "docs: update operations handoff"
```

기능 수정이면 메시지를 기능 단위로 씁니다.

예:

```bash
git commit -m "feat: improve weekly navigation"
git commit -m "fix: handle reset password validation"
```

### 8. GitHub에 push

```bash
git push origin main
```

`main`에 push하면 GitHub Actions의 `Deploy Production` 워크플로가 실행됩니다.

### 9. GitHub Actions 확인

GitHub 저장소에서 확인합니다.

```text
Actions → Deploy Production
```

성공해야 하는 단계:

- Checkout
- Java 17 setup
- `./gradlew test bootJar`
- AWS OIDC 인증
- ECR 로그인
- Docker image build
- ECR push

실패하면 실패한 step의 로그를 보고 수정한 뒤 다시 push합니다.

주의할 점:

- GitHub Actions 성공은 새 Docker 이미지가 ECR에 올라갔다는 뜻입니다.
- 운영 화면 반영까지 끝났다는 뜻은 아닙니다.
- App Runner가 그 이미지를 받아 새 인스턴스를 띄우고 트래픽을 넘긴 뒤 운영 도메인에서 직접 확인해야 배포 완료입니다.

### 10. App Runner 배포 확인

AWS Console에서 확인합니다.

```text
App Runner → daymark-production → Deployments
```

확인할 것:

- 새 이미지 태그가 배포되었는지
- 서비스 상태가 `Running`인지
- health check가 통과하는지
- 로그에 시작 실패가 없는지

서비스 목록이나 상세 화면에 `Operation in progress`가 보이면 배포가 진행 중인 상태입니다. 보통 몇 분 동안 기존 화면이 계속 보일 수 있으므로 `Running`으로 돌아올 때까지 기다립니다.

자동 배포가 연결되어 있지 않거나 새 이미지가 반영되지 않는다면 App Runner 서비스 화면에서 수동 배포를 실행합니다.

```text
App Runner → daymark-production → Deploy
```

### 11. 운영 확인

브라우저에서 확인합니다.

```text
https://usedaymark.com
```

DNS 또는 인증서 전파 중이면 임시 App Runner 주소로 먼저 확인합니다.

```text
https://xefgmam2t3.ap-northeast-1.awsapprunner.com
```

최소 확인 항목:

- `/actuator/health/readiness`가 `UP`인지
- 홈이 열리는지
- 방금 수정한 문구, 링크, 화면 요소가 실제 운영 HTML에 반영되었는지
- 로그인과 회원가입이 동작하는지
- 인증 메일과 비밀번호 재설정 메일이 발송되는지

터미널에서 빠르게 확인할 때는 다음처럼 확인합니다.

```bash
curl -sS https://usedaymark.com/actuator/health/readiness
curl -sS https://usedaymark.com/ | rg "방금 수정한 문구"
```

운영 도메인에서 새 내용이 확인되기 전까지는 배포가 끝난 것으로 보지 않습니다.
- 아침 계획, 저녁 회고, 주간 리뷰가 저장되는지
- 라이브러리 검색과 Markdown/PDF 내보내기가 동작하는지
- 관리자 계정으로 `/admin/operations`가 열리는지
- 일반 계정은 `/admin/operations`에 접근하지 못하는지

## 환경값을 바꿀 때

코드 수정 없이 환경값만 바꿀 때는 App Runner와 SSM 값을 함께 관리합니다.

1. AWS Systems Manager Parameter Store에서 `/daymark/production` 값을 수정합니다.
2. App Runner `daymark-production`의 환경 변수에 반영되어 있는지 확인합니다.
3. App Runner 서비스를 재배포하거나 재시작합니다.
4. `/actuator/health/readiness`를 확인합니다.

메일 관련 값을 바꾸면 추가로 SES에서 발송 테스트를 합니다.

도메인 값을 바꾸면 추가로 Route 53, App Runner custom domain, `DAYMARK_PUBLIC_BASE_URL`을 함께 확인합니다.

로컬 브라우저나 터미널만 접속이 안 되면 DNS 캐시 문제일 수 있습니다. 먼저 Route 53 권한 DNS와 공개 DNS가 같은 값을 주는지 확인합니다.

```bash
dig @ns-402.awsdns-50.com +short usedaymark.com
dig @8.8.8.8 +short usedaymark.com
dig @1.1.1.1 +short usedaymark.com
```

공개 DNS가 App Runner 주소를 가리키는데 내 컴퓨터만 예전 IP를 보면 운영 장애로 단정하지 말고, 브라우저/DNS 캐시가 풀릴 때까지 기다리거나 다른 네트워크에서 재확인합니다.

## 롤백 기준

가장 안전한 롤백은 Git revert 후 다시 배포하는 방식입니다.

```bash
git revert <문제가_된_커밋_SHA>
git push origin main
```

긴급하게 이전 이미지로 되돌려야 하면 AWS Console에서 ECR의 이전 커밋 SHA 태그를 확인한 뒤 App Runner 서비스의 이미지 태그를 이전 값으로 지정해 수동 배포합니다.

DB 마이그레이션이 포함된 배포는 롤백이 더 위험합니다. 운영 DB에 적용된 마이그레이션은 되돌리기 전에 백업과 데이터 보존 방식을 먼저 결정합니다.

## 장애가 났을 때 보는 순서

1. App Runner 서비스 상태와 배포 이력을 확인합니다.
2. App Runner 또는 CloudWatch Logs에서 시작 실패 로그를 확인합니다.
3. `/actuator/health/readiness` 응답을 확인합니다.
4. RDS 상태와 연결 가능 여부를 확인합니다.
5. 최근 GitHub Actions 배포가 성공했는지 확인합니다.
6. 환경 변수 누락이나 secret 오타가 있는지 App Runner/SSM에서 확인합니다.
7. DNS 문제라면 Route 53 record와 App Runner custom domain 상태를 확인합니다.
8. 메일 문제라면 SES identity, DKIM, MAIL FROM, sandbox 상태를 확인합니다.

## 비용 알림 확인

월간 비용 알림은 AWS Budgets에 있습니다.

```text
Billing and Cost Management → Budgets → Daymark Monthly Cost Guard
```

설정 기준:

- 월간 실제 비용 기준
- 10, 20, 30, 40, 50, 60, 70, 80, 90, 100 USD 초과 시 알림
- 알림 수신자: 운영자 Gmail

이 알림은 비용을 자동으로 차단하지 않습니다. AWS 비용 데이터가 갱신된 뒤 이메일로 알려주는 장치입니다.
