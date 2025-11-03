# 1. 베이스 이미지 (Java 21 JRE)
FROM eclipse-temurin:21-jre

# 2. 작업 디렉토리 설정
WORKDIR /app

# 3. 빌드된 JAR 파일 복사
# build/libs/ 폴더에 있는 .jar 파일을 app.jar 라는 이름으로 복사합니다.
COPY build/libs/*.jar app.jar

# 4. Spring Boot 프로필 설정 (도커 환경용)
ENV SPRING_PROFILES_ACTIVE=docker

# 5. 컨테이너 시작 시 실행할 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]