########################################
# Base: Gradle 의존성 + common 모듈 (캐시 공유)
########################################
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS base

WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY api/build.gradle api/
COPY chat/build.gradle chat/
COPY common/build.gradle common/

RUN chmod +x gradlew \
    && ./gradlew dependencies --no-daemon --stacktrace

COPY common/src common/src

########################################
# Build: API
########################################
FROM base AS build-api

COPY api/src api/src
RUN ./gradlew :api:bootJar --no-daemon -x test

########################################
# Build: Chat
########################################
FROM base AS build-chat

COPY chat/src chat/src
RUN ./gradlew :chat:bootJar --no-daemon -x test

########################################
# Extract: API JAR 레이어
########################################
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS extract-api

WORKDIR /workspace
COPY --from=build-api /workspace/api/build/libs/doktori-api.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

########################################
# Extract: Chat JAR 레이어
########################################
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS extract-chat

WORKDIR /workspace
COPY --from=build-chat /workspace/chat/build/libs/doktori-chat.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

########################################
# Runtime: API (:8080)
########################################
FROM eclipse-temurin:21.0.5_11-jre-alpine AS api

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=extract-api /workspace/extracted/dependencies/ ./
COPY --from=extract-api /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract-api /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract-api /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

########################################
# Runtime: Chat (:8081)
########################################
FROM eclipse-temurin:21.0.5_11-jre-alpine AS chat

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=extract-chat /workspace/extracted/dependencies/ ./
COPY --from=extract-chat /workspace/extracted/spring-boot-loader/ ./
COPY --from=extract-chat /workspace/extracted/snapshot-dependencies/ ./
COPY --from=extract-chat /workspace/extracted/application/ ./

USER app
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
