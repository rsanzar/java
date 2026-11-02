FROM --platform=linux/x86_64 debian:bookworm AS build

ENV DEBIAN_FRONTEND=noninteractive
RUN set -xe ; \
    apt-get -yqq update ; \
    apt-get -yqq install --no-install-recommends default-jre-headless default-jdk-headless maven ca-certificates ; \
    rm -rf /var/lib/apt/lists/*

RUN ldconfig /usr/lib/jvm/java-17-openjdk-amd64/lib/ /usr/lib/jvm/java-17-openjdk-amd64/lib/server/

WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM alpine:3 AS sys

RUN set -xe; \
    mkdir -p /target/etc; \
    mkdir -p /blank; \
    apk --no-cache add \
    ca-certificates \
    tzdata \
    ; \
    update-ca-certificates; \
    ln -sf ../usr/share/zoneinfo/Etc/UTC /target/etc/localtime; \
    echo "Etc/UTC" > /target/etc/timezone;

FROM scratch

COPY --from=sys /target/etc /etc
COPY --from=sys /usr/share/zoneinfo/Etc/UTC /usr/share/zoneinfo/Etc/UTC
COPY --from=sys /etc/ssl/certs/ca-certificates.crt /etc/ssl/certs/ca-certificates.crt
COPY --from=sys /blank /tmp

COPY --from=build /lib/x86_64-linux-gnu/libc.so.6 /lib/x86_64-linux-gnu/libc.so.6
COPY --from=build /lib/x86_64-linux-gnu/libstdc++.so.6 /lib/x86_64-linux-gnu/libstdc++.so.6
COPY --from=build /lib/x86_64-linux-gnu/libm.so.6 /lib/x86_64-linux-gnu/libm.so.6
COPY --from=build /usr/lib/x86_64-linux-gnu/libz.so.1 /usr/lib/x86_64-linux-gnu/libz.so.1
COPY --from=build /lib/x86_64-linux-gnu/libgcc_s.so.1 /lib/x86_64-linux-gnu/libgcc_s.so.1
COPY --from=build /lib64/ld-linux-x86-64.so.2 /lib64/ld-linux-x86-64.so.2
COPY --from=build /etc/ld.so.cache /etc/ld.so.cache

COPY --from=build /usr/lib/jvm/java-17-openjdk-amd64/ /usr/lib/jvm/java-17-openjdk-amd64/
COPY --from=build /etc/java-17-openjdk/security/ /etc/java-17-openjdk/security/

COPY --from=build /src/target/bench-spring-unikraft.jar /usr/src/server.jar
