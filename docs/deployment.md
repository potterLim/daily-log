# 배포 가이드

Daymark의 초기 AWS 배포 기준은 App Runner, Amazon ECR, Amazon RDS for MySQL, Amazon SES SMTP, Route 53/ACM, 운영 알림 웹훅입니다. 운영 데이터는 MySQL에 저장하며, Flyway가 스키마 변경을 관리합니다.

## 운영 결정

| 항목 | 결정 |
| --- | --- |
| 애플리케이션 실행 | AWS App Runner에 Docker 이미지 배포 |
| 컨테이너 이미지 | Amazon ECR `daymark` 저장소 |
| 배포 인증 | GitHub Actions OIDC로 AWS 임시 권한 발급 |
| 운영 DB | Amazon RDS for MySQL |
| 메일 | Amazon SES SMTP |
| 발신 주소 | `no-reply@usedaymark.com` |
| 도메인 인증 | SES 도메인 인증, DKIM, SPF, DMARC 모두 설정 |
| HTTPS | App Runner custom domain + ACM 인증서 |
| 쿠키 | 세션 쿠키와 remember-me 쿠키 모두 Secure 강제 |
| 알림 | `DAYMARK_ALERT_WEBHOOK_URL`로 운영 실패 알림 전송 |
| 알 수 없는 공개 URL | 로그인으로 보내지 않고 제품형 404 표시 |

App Runner는 2026. 04. 30.부터 신규 고객에게 닫히므로, 사용하려면 그 전에 App Runner 서비스를 생성해 둡니다. 기존 고객은 서비스를 계속 사용할 수 있지만, 정식 장기 운영 전에는 ECS Express Mode 이전 가능성을 열어 둡니다.

## 현재 운영 리소스

아래 값은 GitHub에 공개해도 되는 운영 위치 정보입니다. DB 비밀번호, remember-me secret, SMTP 비밀번호, AWS 계정 ID, RDS 엔드포인트는 공개 문서에 남기지 않습니다.

| 항목 | 값 |
| --- | --- |
| 운영 리전 | `ap-northeast-1` |
| 도메인 | `usedaymark.com` |
| 임시 App Runner URL | `https://xefgmam2t3.ap-northeast-1.awsapprunner.com` |
| App Runner 서비스 | `daymark-production` |
| ECR 저장소 | `daymark` |
| Route 53 Hosted Zone ID | `Z05714131U5V3ES6US84K` |
| RDS 식별자 | `daymark-production-db` |
| SSM 파라미터 prefix | `/daymark/production` |

Namecheap에서 `usedaymark.com`의 DNS를 Route 53으로 위임할 때 아래 네임서버를 사용합니다.

```text
ns-402.awsdns-50.com
ns-1642.awsdns-13.co.uk
ns-685.awsdns-21.net
ns-1110.awsdns-10.org
```

현재 진행 상태:

- App Runner 서비스는 `RUNNING`입니다.
- Namecheap nameserver는 Route 53 네임서버로 저장했습니다.
- Route 53에 App Runner custom domain 인증 CNAME을 등록했습니다.
- Route 53에 SES domain verification TXT, DKIM, SPF, DMARC, MAIL FROM 레코드를 등록했습니다.
- 공개 DNS 전파 전에는 일부 조회가 Namecheap 기본 네임서버를 계속 볼 수 있습니다.

DNS 위임 후 확인할 항목:

- `usedaymark.com`이 App Runner custom domain으로 연결되는지 확인합니다.
- App Runner custom domain 인증서 상태가 `ACTIVE`로 바뀌는지 확인합니다.
- SES identity, DKIM, MAIL FROM 상태가 `SUCCESS`로 바뀌는지 확인합니다.
- SES sandbox 해제 전에는 검증된 수신자에게만 메일을 보낼 수 있습니다.

## App Runner 배포

### 1. AWS 리전

App Runner는 서울 리전을 지원하지 않으므로 초기 운영 리전은 도쿄 리전입니다. App Runner, ECR, RDS, SES는 같은 리전에 둡니다.

```text
ap-northeast-1
```

Route 53은 글로벌 서비스입니다.

### 2. GitHub Actions OIDC 배포 권한

장기 Access Key는 사용하지 않습니다. AWS IAM에 GitHub OIDC provider와 전용 배포 Role을 만들고, GitHub Actions가 배포 순간에만 임시 권한을 발급받습니다.

AWS에 생성할 리소스:

- OIDC provider: `token.actions.githubusercontent.com`
- Role: `daymark-github-production-deploy`
- Trust policy: `ops/aws/github-oidc-trust-policy.template.json` 기준
- Permission policy: `ops/aws/github-deploy-policy.template.json` 기준
- GitHub environment: `production`, 배포 브랜치는 `main`만 허용

GitHub `production` 환경 변수:

| 변수 | 값 |
| --- | --- |
| `AWS_ROLE_TO_ASSUME` | `arn:aws:iam::<AWS_ACCOUNT_ID>:role/daymark-github-production-deploy` |

배포 워크플로:

```text
.github/workflows/deploy-production.yml
```

`main` 브랜치 push 또는 수동 실행으로 테스트, JAR 빌드, Docker 이미지 빌드, ECR push가 순서대로 실행됩니다.

### 3. RDS MySQL

권장 기본값:

- 엔진: MySQL 8.0
- 퍼블릭 접근: 비활성화
- 보안 그룹: App Runner VPC Connector에서 오는 트래픽만 허용
- 데이터베이스 이름: `daymark`
- 사용자: `daymark`
- 백업 보존: 7일 이상
- 삭제 방지: 운영 공개 후 활성화

JDBC URL 예시:

```text
jdbc:mysql://daymark-db.xxxxxxxxxxxx.ap-northeast-1.rds.amazonaws.com:3306/daymark?useSSL=true&requireSSL=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

### 4. SES 메일

운영 메일 기준:

- SES 도메인 identity 생성
- DKIM DNS 레코드 등록
- SPF/custom MAIL FROM 설정
- DMARC TXT 레코드 등록
- SES sandbox 해제 요청
- SMTP credentials 생성

권장 발신 주소:

```text
no-reply@usedaymark.com
```

권장 SMTP endpoint:

```text
email-smtp.ap-northeast-1.amazonaws.com
```

### 5. ECR 이미지 푸시

정식 배포는 GitHub Actions가 수행합니다. 로컬에서 직접 push하는 방식은 장애 대응용 보조 경로로만 사용합니다.

```bash
git push origin main
```

워크플로는 `daymark:<commit-sha>`와 `daymark:latest` 이미지를 ECR에 푸시합니다.

여기까지는 이미지 업로드 단계입니다. 운영 화면 반영은 App Runner가 새 이미지를 받아 배포까지 마쳐야 완료됩니다. GitHub Actions가 성공했더라도 App Runner가 `Operation in progress` 상태이면 기존 화면이 잠시 계속 보일 수 있습니다.

### 6. App Runner 서비스 생성

App Runner에서 다음 값으로 서비스를 생성합니다.

| 항목 | 값 |
| --- | --- |
| Source | Container registry |
| Provider | Amazon ECR |
| Image | `daymark:latest` 또는 커밋 태그 |
| Deployment trigger | Automatic 권장 |
| Port | `8080` |
| Health check path | `/actuator/health/readiness` |
| CPU/Memory | 초기 `0.25 vCPU / 0.5GB` 또는 `0.5 vCPU / 1GB` |
| Auto scaling | 최소 1, 최대 2부터 시작 |
| Network | RDS 접근용 VPC Connector 연결 |

Spring Boot와 App Runner는 같은 포트 값을 사용해야 합니다. App Runner의 이미지 포트는 `8080`으로 설정하고, `PORT`는 App Runner 예약 환경 변수이므로 직접 추가하지 않습니다.

### 7. App Runner 환경 변수

콘솔에 입력할 값은 `ops/aws/app-runner-env.example`를 기준으로 준비합니다. 예시 값은 그대로 쓰지 말고 모두 실제 값으로 바꿉니다.

필수 값:

| 환경 변수 | 설명 |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `production` |
| `DAYMARK_PUBLIC_BASE_URL` | `https://usedaymark.com` |
| `DATABASE_URL` | RDS MySQL JDBC URL |
| `DATABASE_USERNAME` | RDS 사용자 |
| `DATABASE_PASSWORD` | RDS 비밀번호 |
| `DAYMARK_REMEMBER_ME_KEY` | 64자 이상 임의 secret 권장 |
| `DAYMARK_REMEMBER_ME_COOKIE_SECURE` | `true` |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `true` |
| `DAYMARK_MAIL_FROM_ADDRESS` | SES에서 인증한 발신 주소 |
| `SPRING_MAIL_HOST` | SES SMTP endpoint |
| `SPRING_MAIL_PORT` | `587` |
| `SPRING_MAIL_USERNAME` | SES SMTP 사용자 |
| `SPRING_MAIL_PASSWORD` | SES SMTP 비밀번호 |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH` | `true` |
| `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE` | `true` |
| `DAYMARK_ALERT_WEBHOOK_URL` | 운영 알림 HTTPS webhook |

`production` 프로필에서는 위 값이 부족하거나 placeholder이면 애플리케이션이 시작되지 않습니다.

### 8. 도메인 연결

권장 구조:

```text
https://usedaymark.com
```

App Runner custom domain을 추가하고 DNS 검증 레코드를 등록합니다. `DAYMARK_PUBLIC_BASE_URL`도 같은 주소로 설정합니다.

### 9. 배포 후 확인

```text
https://usedaymark.com/actuator/health/readiness
```

확인 항목:

- App Runner health check가 통과하는지 확인합니다.
- App Runner 서비스가 `Running` 상태인지 확인합니다.
- `Operation in progress`가 보이면 새 컨테이너가 준비되는 중이므로 완료될 때까지 기다립니다.
- GitHub Actions 성공만으로 배포 완료로 판단하지 않고, 운영 도메인에서 방금 수정한 화면 또는 문구가 실제로 보이는지 확인합니다.
- 회원가입 후 인증 메일이 실제로 도착하는지 확인합니다.
- 비밀번호 재설정 메일이 실제로 도착하는지 확인합니다.
- 인증/재설정 링크가 `DAYMARK_PUBLIC_BASE_URL` 도메인으로 생성되는지 확인합니다.
- 알 수 없는 공개 URL이 로그인 대신 404 화면을 보여주는지 확인합니다.
- `/daymark/morning` 같은 보호 화면은 로그인으로 이동하는지 확인합니다.

빠른 운영 확인 예:

```bash
curl -sS https://usedaymark.com/actuator/health/readiness
curl -sS https://usedaymark.com/ | rg "방금 수정한 문구"
```

### 10. 운영자 계정 부여

회원가입으로 계정을 만든 뒤 운영 DB에서 해당 계정을 관리자 권한으로 승격합니다.

```sql
update user_account
set user_role = 'ADMIN'
where user_name = '운영자_워크스페이스_ID';
```

그 계정으로 로그인하면 다음 주소에서 운영 지표를 확인할 수 있습니다.

```text
https://usedaymark.com/admin/operations
```

관리자 화면에서는 이번 주 활성 사용자, 작성 사용자, 인증 메일 흐름, 비밀번호 복구, 라이브러리 조회, Markdown/PDF 내보내기 사용량을 확인합니다.

## 백업과 복구 리허설

운영 기준:

- RDS 자동 백업 보존 기간은 최소 7일로 시작합니다.
- 공개 전 수동 스냅샷을 1회 생성합니다.
- 월 1회 비운영 RDS로 스냅샷 복구를 리허설합니다.
- 복구 후 `/actuator/health/readiness`, 로그인, 기록 조회를 확인합니다.
- 백업/복구 실패는 `DAYMARK_ALERT_WEBHOOK_URL`로 알림을 보냅니다.

## 배포 방식

| 방식 | 적합한 경우 |
| --- | --- |
| App Runner | AWS 초기 베타 배포 |
| JAR 배포 | VM, VPS, 클라우드 서버에서 직접 운영할 때 |
| Docker Compose | 애플리케이션과 MySQL을 함께 컨테이너로 관리할 때 |

App Runner 외의 운영 환경에서는 Nginx, Caddy, 로드 밸런서 같은 앞단에서 HTTPS를 종료하는 구성을 권장합니다.

## 로컬 확인

MySQL 준비 없이 로컬에서 먼저 확인하려면 `local` 프로필을 사용합니다.

```bash
./gradlew bootRun --args="--spring.profiles.active=local"
```

접속 주소:

```text
http://127.0.0.1:8080
```

`local` 프로필은 H2 메모리 데이터베이스를 사용하고, SMTP가 없어도 인증/복구 링크를 로그로 확인할 수 있습니다.

## 배포 전 확인

```bash
./gradlew test
./gradlew bootJar
```

Docker가 준비되어 있다면 MySQL 통합 테스트도 실행합니다.

```bash
./gradlew mysqlIntegrationTest
```

생성되는 JAR:

```text
build/libs/daymark.jar
```

## 필수 환경 변수

기본 프로필은 다음 값이 없으면 시작하지 않습니다.

| 환경 변수 | 설명 |
| --- | --- |
| `DAYMARK_PUBLIC_BASE_URL` | 인증/복구 링크에 사용할 공개 HTTPS 주소 |
| `DATABASE_URL` | MySQL JDBC URL |
| `DATABASE_USERNAME` | MySQL 사용자 |
| `DATABASE_PASSWORD` | MySQL 비밀번호 |
| `DAYMARK_REMEMBER_ME_KEY` | remember-me 서명 키 |

예시 JDBC URL:

```text
jdbc:mysql://127.0.0.1:3306/daymark?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
```

## 주요 선택 환경 변수

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `PORT` | `8080` | 애플리케이션 포트 |
| `SERVER_SERVLET_SESSION_COOKIE_SECURE` | `false` | HTTPS 환경에서 보안 쿠키 사용 |
| `DAYMARK_REMEMBER_ME_COOKIE_SECURE` | `false` | HTTPS 환경에서 remember-me 보안 쿠키 사용 |
| `DAYMARK_PASSWORD_RESET_TOKEN_VALIDITY_MINUTES` | `30` | 비밀번호 재설정 링크 유효 시간 |
| `DAYMARK_EMAIL_VERIFICATION_TOKEN_VALIDITY_MINUTES` | `1440` | 이메일 인증 링크 유효 시간 |
| `DAYMARK_MAIL_FROM_ADDRESS` | `no-reply@daymark.local` | 발신 메일 주소 |
| `DAYMARK_ALERT_WEBHOOK_URL` | 없음 | 운영 알림 웹훅 |
| `DAYMARK_WEEKLY_SUMMARY_ENABLED` | `false` | 주간 운영 요약 로그 |
| `DAYMARK_LOG_DIR` | `./logs` | 애플리케이션 로그 경로 |
| `DAYMARK_TOMCAT_BASE_DIR` | `./ops/runtime/tomcat` | Tomcat 접근 로그 경로 |

`production` 프로필에서는 운영 준비 상태 검증이 켜지고, SMTP/웹훅/보안 쿠키/remember-me 키 길이를 더 엄격하게 확인합니다.

## JAR 배포

### 1. JAR 준비

```bash
./gradlew bootJar
```

### 2. 서버 경로 준비

예시:

```bash
sudo mkdir -p /opt/daymark
sudo mkdir -p /etc/daymark
sudo mkdir -p /var/log/daymark/app
sudo mkdir -p /var/log/daymark/tomcat
```

### 3. 환경 파일 작성

예시 경로:

```text
/etc/daymark/daymark.env
```

예시 내용:

```bash
DATABASE_URL=jdbc:mysql://127.0.0.1:3306/daymark?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DATABASE_USERNAME=daymark
DATABASE_PASSWORD=replace-this
DAYMARK_PUBLIC_BASE_URL=https://daymark.example.com
DAYMARK_REMEMBER_ME_KEY=replace-this-with-a-long-random-secret
DAYMARK_MAIL_FROM_ADDRESS=no-reply@example.com
DAYMARK_ALERT_WEBHOOK_URL=https://example.com/alerts/daymark
DAYMARK_LOG_DIR=/var/log/daymark/app
DAYMARK_TOMCAT_BASE_DIR=/var/log/daymark/tomcat
SPRING_MAIL_HOST=smtp.example.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=mailer@example.com
SPRING_MAIL_PASSWORD=replace-this
SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true
SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true
SERVER_SERVLET_SESSION_COOKIE_SECURE=true
DAYMARK_REMEMBER_ME_COOKIE_SECURE=true
SPRING_PROFILES_ACTIVE=production
```

### 4. 수동 실행 확인

```bash
set -a
source /etc/daymark/daymark.env
set +a
java -jar /opt/daymark/daymark.jar
```

확인할 항목:

- 데이터베이스 연결
- Flyway 마이그레이션
- 회원가입과 로그인
- 이메일 인증과 비밀번호 복구
- 아침 계획, 저녁 회고, 주간 리뷰
- 라이브러리 검색
- Markdown 다운로드와 PDF 미리보기
- `/actuator/health/readiness`

### 5. systemd 예시

```ini
[Unit]
Description=Daymark
After=network.target

[Service]
User=daymark
WorkingDirectory=/opt/daymark
EnvironmentFile=/etc/daymark/daymark.env
ExecStart=/usr/bin/java -jar /opt/daymark/daymark.jar
SuccessExitStatus=143
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
```

적용:

```bash
sudo systemctl daemon-reload
sudo systemctl enable daymark
sudo systemctl start daymark
sudo systemctl status daymark
```

## Docker Compose 배포

### 1. 환경 파일 준비

```bash
cp .env.example .env
```

반드시 예시 비밀번호와 secret을 실제 값으로 바꿉니다.

주요 값:

- `SPRING_PROFILES_ACTIVE`
- `APP_PORT`
- `MYSQL_DATABASE`
- `MYSQL_USER`
- `MYSQL_PASSWORD`
- `MYSQL_ROOT_PASSWORD`
- `DAYMARK_PUBLIC_BASE_URL`
- `DAYMARK_REMEMBER_ME_KEY`
- `SERVER_SERVLET_SESSION_COOKIE_SECURE`
- `DAYMARK_REMEMBER_ME_COOKIE_SECURE`
- `DAYMARK_MAIL_FROM_ADDRESS`
- `DAYMARK_ALERT_WEBHOOK_URL`
- `SPRING_MAIL_HOST`
- `SPRING_MAIL_PORT`
- `SPRING_MAIL_USERNAME`
- `SPRING_MAIL_PASSWORD`

### 2. 실행

```bash
docker compose up -d --build
```

### 3. 확인

```bash
docker compose ps
docker compose logs -f app
curl -fsS http://127.0.0.1:${APP_PORT:-8080}/actuator/health/readiness
```

### 4. 수동 백업

```bash
docker compose --profile ops run --rm backup
```

## 상태 확인 엔드포인트

| 경로 | 용도 |
| --- | --- |
| `/actuator/health` | 수동 상태 확인 |
| `/actuator/health/liveness` | 프로세스 생존 확인 |
| `/actuator/health/readiness` | 트래픽 수신 가능 여부 확인 |

## 백업 기준

운영 백업 대상:

- MySQL 데이터
- Git에 커밋하지 않는 환경 변수와 secret
- 필요 시 운영 로그

저장소에는 다음 보조 스크립트가 있습니다.

- `ops/backup/mysql-backup.sh`
- `ops/backup/mysql-restore.sh`
- `ops/backup/daymark-backup.service`
- `ops/backup/daymark-backup.timer`

권장 사항:

- 백업 파일과 체크섬을 함께 보관합니다.
- 복구 절차를 비운영 데이터베이스에서 주기적으로 확인합니다.
- 백업 실패 알림은 `DAYMARK_ALERT_WEBHOOK_URL`로 연결합니다.
- 운영 지표 원천 데이터는 `operation_usage_event`, 주간 보관본은 `weekly_operation_metric_snapshot`에 저장합니다.
- 생성된 백업, 로그, PDF, Markdown, 화면 캡처는 저장소에 커밋하지 않습니다.

## 공개 저장소 기준

퍼블릭 저장소에 포함해도 되는 것:

- 운영 지표 코드
- 관리자 화면 코드
- DB 마이그레이션
- 예시 환경 변수 파일
- 배포 절차 문서

퍼블릭 저장소에 포함하면 안 되는 것:

- 실제 운영 DB 데이터
- 실제 `.env`
- AWS/SES/RDS credential
- 실제 webhook URL
- 운영 로그와 백업 파일

## 운영 전 최종 점검

- HTTPS 앞단이 준비되어 있는지 확인합니다.
- `DAYMARK_REMEMBER_ME_KEY`를 충분히 긴 임의 값으로 설정합니다.
- 실제 MySQL 비밀번호를 사용합니다.
- 외부에서 MySQL에 직접 접근하지 못하게 제한합니다.
- SMTP가 실제로 메일을 발송하는지 확인합니다.
- 이메일 인증과 비밀번호 복구 링크가 한 번만 사용되는지 확인합니다.
- 아침/저녁 빈 저장이 기록으로 보이지 않는지 확인합니다.
- 라이브러리 검색과 Markdown/PDF 내보내기가 같은 조건을 따르는지 확인합니다.
- 알 수 없는 경로가 제품형 404 화면으로 연결되는지 확인합니다.
