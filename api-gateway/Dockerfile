# ── Stage 1: Build ──────────────────────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S connectsphere && adduser -S connectsphere -G connectsphere

COPY --from=builder /app/target/api-gateway-1.0.0.jar app.jar

RUN chown connectsphere:connectsphere app.jar

USER connectsphere

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=45s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", \
  "-Xms256m", \
  "-Xmx512m", \
  "-jar", "app.jar"]