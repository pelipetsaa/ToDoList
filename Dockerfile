# Backend image. Build from the repository root: docker build -t todolist-server .

# Zulu 21 matches the Gradle daemon toolchain (gradle/gradle-daemon-jvm.properties), so nothing is downloaded.
FROM azul/zulu-openjdk:21 AS build
WORKDIR /workspace
COPY . .
# Only :core and :server are built; the Android and iOS modules are configured but need no SDK for this task.
RUN ./gradlew :server:buildFatJar --no-daemon --console=plain

FROM azul/zulu-openjdk:21-jre
RUN useradd --system --uid 10001 app
WORKDIR /app
COPY --from=build /workspace/server/build/libs/server-all.jar app.jar
USER app
ENV APP_ENV=production \
    PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
