FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
# Break hardlinks (e.g. in /usr/share/zoneinfo) and remove unnecessary docs/zoneinfo so Unikraft libukcpio extracts cleanly without hardlink errors (-11)
RUN rm -rf /usr/share/zoneinfo /usr/share/man /usr/share/doc /var/cache/apk/* && \
    find / -xdev -type f -links +1 -exec sh -c 'for f; do cp --remove-destination "$f" "$f.tmp" && mv "$f.tmp" "$f"; done' _ {} +

FROM scratch
COPY --from=runtime / /
COPY --from=builder /app/target/bench-spring-unikraft.jar /app.jar
