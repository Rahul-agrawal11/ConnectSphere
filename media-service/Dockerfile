FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create uploads directory for local storage
RUN mkdir -p /app/uploads

RUN addgroup -S connectsphere && adduser -S connectsphere -G connectsphere
COPY --from=builder /app/target/media-service-1.0.0.jar app.jar
RUN chown -R connectsphere:connectsphere /app

USER connectsphere
EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8087/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-Dapp.storage.local.upload-dir=/app/uploads", \
  "-jar", "app.jar"]