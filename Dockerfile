# ── Build stage ────────────────────────────────────────────────────────────────
FROM gradle:8.12-jdk17 AS build
WORKDIR /build

# The Android Gradle Plugin requires sdk.dir during project configuration even
# when we only compile the JVM :server target.  A stub empty directory satisfies
# the check without pulling in the full SDK.
ENV ANDROID_HOME=/tmp/android-sdk
RUN mkdir -p /tmp/android-sdk
COPY . .
RUN printf "sdk.dir=/tmp/android-sdk\n" > local.properties && \
    gradle :server:buildFatJar --no-daemon

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /build/server/build/libs/server-*-all.jar server.jar

# SERVER_BIND_HOST / SERVER_BIND_PORT are read by application.conf at runtime.
# Override them via docker run -e or docker-compose environment.
ENV SERVER_BIND_HOST=0.0.0.0
ENV SERVER_BIND_PORT=8080

EXPOSE 8080
CMD ["java", "-jar", "server.jar"]
