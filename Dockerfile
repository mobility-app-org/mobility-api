# -----------------------------------------------
# 1단계: 빌드(Build) 환경 (JDK + Gradle)
# -----------------------------------------------
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /workspace

# 소스코드 전체를 컨테이너로 복사
COPY . .

# gradlew 실행 권한 부여
RUN chmod +x ./gradlew

# ⚡️컨테이너 '안에서' bootJar 빌드를 실행
RUN ./gradlew bootJar --no-daemon

# -----------------------------------------------
# 2단계: 실행(Run) 환경 (JRE)
# -----------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=docker

# ⚡️'builder' 단계의 빌드 결과물(.jar)만 복사해옴
COPY --from=builder /workspace/build/libs/*.jar app.jar

# 컨테이너가 시작될 때 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]