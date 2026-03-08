########################################
# Extract: API JAR 레이어
########################################
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS extract-api

WORKDIR /workspace
COPY api/build/libs/doktori-api.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

########################################
# Extract: Chat JAR 레이어
########################################
FROM eclipse-temurin:21.0.5_11-jdk-alpine AS extract-chat

WORKDIR /workspace
COPY chat/build/libs/doktori-chat.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --launcher --destination extracted

########################################
# Runtime: API (:8080)
########################################
FROM eclipse-temurin:21.0.5_11-jre-alpine AS api

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --link --from=extract-api /workspace/extracted/dependencies/ ./
COPY --link --from=extract-api /workspace/extracted/spring-boot-loader/ ./
COPY --link --from=extract-api /workspace/extracted/snapshot-dependencies/ ./
COPY --link --from=extract-api /workspace/extracted/application/ ./

USER app
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]

########################################
# Runtime: Chat (:8081)
########################################
FROM eclipse-temurin:21.0.5_11-jre-alpine AS chat

WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --link --from=extract-chat /workspace/extracted/dependencies/ ./
COPY --link --from=extract-chat /workspace/extracted/spring-boot-loader/ ./
COPY --link --from=extract-chat /workspace/extracted/snapshot-dependencies/ ./
COPY --link --from=extract-chat /workspace/extracted/application/ ./

USER app
EXPOSE 8081
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
