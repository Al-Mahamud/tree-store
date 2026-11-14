# The API as a container: built from source, run as an unprivileged user with the prod profile.
# Tests are not run here; CI runs them before anything is deployed.

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B dependency:go-offline
COPY src/main src/main
RUN --mount=type=cache,target=/root/.m2 ./mvnw -q -B -Dmaven.test.skip=true package \
    && cp target/store-backend-*.jar app.jar

FROM eclipse-temurin:17-jre
RUN useradd --system --uid 10001 --home-dir /app shobuj \
    && mkdir -p /var/lib/shobuj/storage \
    && chown shobuj /var/lib/shobuj/storage
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER shobuj
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
# Plant pictures and review photos live here; mount a volume (deploy/compose.yaml).
VOLUME /var/lib/shobuj/storage
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
