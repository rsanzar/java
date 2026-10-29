# Java Cold Start / SnapStart Benchmark

3 frameworks × 2 modes = **6 apps**. They all have identical endpoints and listen on port `8080`:
`GET /` · `GET /health` · `GET /json`

|  | JVM (Temurin 21 JRE) | GraalVM native (static musl binary) |
|---|---|---|
| **Spring Boot 3.5** (Tomcat, virtual threads) | `bench-spring` | `bench-spring-native` |
| **Plain Java** (JDK HttpServer, virtual threads, 0 deps) | `bench-plain` | `bench-plain-native` |
| **Quarkus 3.20** (Quarkus REST / Vert.x, virtual threads) | `bench-quarkus` | `bench-quarkus-native` |

Every app prints `[bench-xxx] READY in N ms (since process start)`, so you can read the
startup time measured inside the guest separately from the end-to-end time measured from outside.

## Docker images (rootfs sources)

| Runtime name | `docker_source` | Role |
|---|---|---|
| `java-maven-latest` | `maven:3.9-eclipse-temurin-21-alpine` | CI: builds the JVM jars |
| `java-jre-latest` | `eclipse-temurin:21-jre-alpine` | CD: runs the JVM apps |
| `graal-native-latest` | `ghcr.io/graalvm/native-image-community:21-muslib` | CI: builds the native binaries (GraalVM CE 21 + musl toolchain) |
| `naked-latest` | `alpine` | CD: runs the native apps (fully static binaries, no JVM) |

The GraalVM image has no Maven, so the native builds use the repo's `./mvnw`. It downloads Maven 3.9.9
into `/root/.m2/wrapper`, which is a cache mount. If the image has no curl/wget, mvnw falls back to `javac`.

## Files

- `anzar.yaml`: the 6 apps, each with CI build commands and CD start commands
- `forge-java.yaml`: `build_image`/`build_snapshot` for the 4 runtimes, plus `prepare_env` → `run_pipeline` → `stage_deployment` → `simulate_traffic` for the 6 apps

## Build commands (CI, run from repo root)

```sh
# ---- JVM (java-maven-latest) ----
cd spring-app  && mvn -B -q -DskipTests package           # -> spring-app/target/bench-spring.jar
cd quarkus-app && mvn -B -q -DskipTests package           # -> quarkus-app/target/quarkus-app/quarkus-run.jar (+ lib/)
cd plain-app   && javac --release 21 -d out src/main/java/bench/plain/Main.java \
               && jar --create --file app.jar --main-class bench.plain.Main -C out .

# ---- Native (graal-native-latest) ----
sh mvnw -B -q -f spring-app/pom.xml  -Pnative,musl -DskipTests native:compile   # -> spring-app/target/bench-spring
sh mvnw -B -q -f quarkus-app/pom.xml -Dnative      -DskipTests package          # -> quarkus-app/target/bench-quarkus-1.0.0-runner
cd plain-app && native-image --static --libc=musl -cp out bench.plain.Main -o bench-plain
```

## Start commands (CD)

```sh
JAVA_FLAGS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75 -XX:-UsePerfData -Djava.security.egd=file:/dev/./urandom"

# JVM
java $JAVA_FLAGS -jar /app/spring/app.jar --server.address=0.0.0.0 --server.port=8080
java $JAVA_FLAGS -jar /app/plain/app.jar  --host 0.0.0.0 --port 8080
java $JAVA_FLAGS -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080 -jar /app/quarkus/quarkus-app/quarkus-run.jar

# Native
/app/spring-native/bench-spring   --server.address=0.0.0.0 --server.port=8080
/app/plain-native/bench-plain     --host 0.0.0.0 --port 8080
/app/quarkus-native/bench-quarkus -Dquarkus.http.host=0.0.0.0 -Dquarkus.http.port=8080
```

## Notes

- **Same GC in both modes:** GraalVM CE native binaries always use the Serial GC. The JVM apps are pinned to `-XX:+UseSerialGC` so the two modes compare like for like.
- **RAM for native builds:** native-image needs a lot of memory (roughly 4–8 GB for Spring, 3–6 GB for Quarkus). The native CI jobs use tshirt `large` (4 GB VM).
- **CPU features:** native binaries are compiled for x86-64-v3 (AVX2) by default. If a host CPU or a Firecracker CPU template hides AVX2, add `-march=compatibility`:
  - Spring: add it as another `buildArg` in the `musl` profile.
  - Quarkus: append it to `quarkus.native.additional-build-args`.
  - Plain: add it to the `native-image` command.
- **Entropy:** `-Djava.security.egd=file:/dev/./urandom` stops SecureRandom from waiting on entropy in a freshly booted microVM.
- **Snapshot clones:** every VM restored from the same snapshot starts with the same JVM/native state, including random-number seeds. That's fine for a benchmark, but it matters for production crypto.
- **Optional JVM cold-start tuning** (not enabled, to keep a raw baseline): Spring AppCDS / Quarkus AppCDS
  (`-Dquarkus.package.jar.appcds.enabled=true`). The CDS archive has to be created by the same JVM build that runs it.
