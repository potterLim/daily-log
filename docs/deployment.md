# 배포 가이드

Daymark는 실행 가능한 JAR 또는 Docker Compose로 배포할 수 있습니다. 운영 데이터는 MySQL에 저장하며, Flyway가 스키마 변경을 관리합니다.

## 배포 방식

| 방식 | 적합한 경우 |
| --- | --- |
| JAR 배포 | VM, VPS, 클라우드 서버에서 직접 운영할 때 |
| Docker Compose | 애플리케이션과 MySQL을 함께 컨테이너로 관리할 때 |

운영 환경에서는 Nginx, Caddy, 로드 밸런서 같은 앞단에서 HTTPS를 종료하는 구성을 권장합니다.

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
- `DAYMARK_REMEMBER_ME_KEY`
- `SERVER_SERVLET_SESSION_COOKIE_SECURE`
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
- 생성된 백업, 로그, PDF, Markdown, 화면 캡처는 저장소에 커밋하지 않습니다.

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
