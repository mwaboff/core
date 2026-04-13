# ---- build stage ----
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /build
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
RUN addgroup -S -g 1000 app && adduser -S -u 1000 -G app app
COPY --from=build --chown=app:app /build/target/*.jar app.jar
USER app
EXPOSE 8080
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=40 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
ENTRYPOINT ["java", "-jar", "app.jar"]
