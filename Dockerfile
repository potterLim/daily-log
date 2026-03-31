FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /workspace

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle gradle
COPY src src

RUN chmod +x gradlew && ./gradlew bootJar --no-daemon

FROM eclipse-temurin:17-jre

WORKDIR /app

ENV TZ=Asia/Seoul

COPY --from=builder /workspace/build/libs/*.jar /app/daylog.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/daylog.jar"]
