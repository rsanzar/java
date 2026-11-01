FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

FROM scratch
COPY --from=runtime / /
COPY --from=builder /app/target/bench-spring-unikraft.jar /app.jar
