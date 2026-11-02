FROM maven:3.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime
# 1. Configure musl dynamic linker path & copy libjli/JVM shared libraries to /usr/lib so Unikraft resolves libjli.so without /proc/self/exe ($ORIGIN)
# 2. Break hardlinks (e.g. in /usr/share/zoneinfo) so Unikraft libukcpio extracts cleanly without hardlink errors (-11)
RUN echo "/lib:/usr/local/lib:/usr/lib:/opt/java/openjdk/lib:/opt/java/openjdk/lib/jli:/opt/java/openjdk/lib/server" > /etc/ld-musl-x86_64.path && \
    cp -f /opt/java/openjdk/lib/*.so /usr/lib/ 2>/dev/null || true && \
    cp -f /opt/java/openjdk/lib/jli/*.so /usr/lib/ 2>/dev/null || true && \
    cp -f /opt/java/openjdk/lib/server/*.so /usr/lib/ 2>/dev/null || true && \
    rm -rf /usr/share/zoneinfo /usr/share/man /usr/share/doc /var/cache/apk/* && \
    find / -xdev -type f -links +1 -exec sh -c 'for f; do cp --remove-destination "$f" "$f.tmp" && mv "$f.tmp" "$f"; done' _ {} +

FROM scratch
COPY --from=runtime / /
COPY --from=builder /app/target/bench-spring-unikraft.jar /app.jar
