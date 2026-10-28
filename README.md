# Java Cold Start / SnapStart Benchmark

Two HTTP servers with **identical endpoints**, built and run on the same images:

| App | Folder | Stack | Jar on `/app` drive |
|---|---|---|---|
| `bench-spring` | `spring-app/` | Spring Boot 3.5, Tomcat, virtual threads | `/app/spring/app.jar` |
| `bench-plain` | `plain-app/` | JDK `com.sun.net.httpserver`, virtual threads, 0 deps | `/app/plain/app.jar` |

Endpoints (both apps, port `8080`): `GET /` · `GET /health` · `GET /json`

Both print a line like `READY in 83 ms (since process start)` so you can read the in-guest
startup time from the VM console, separately from the end-to-end time measured from outside.

## Docker images (rootfs sources)

| Runtime name | `docker_source` | Role |
|---|---|---|
| `java-maven-latest` | `maven:3.9-eclipse-temurin-21-alpine` | CI builder (mvn, javac, jar). Cache `/root/.m2` |
| `java-jre-latest` | `eclipse-temurin:21-jre-alpine` | CD runner for both apps |

## Files

- `anzar.yaml` – multi-app CI/CD config (build + start commands)
- `forge-java.yaml` – `build_image` / `build_snapshot` for both runtimes + `prepare_env` / `run_pipeline` / `stage_deployment` / `simulate_traffic` for both apps

## Build commands (CI, run from repo root)

```sh
# Spring Boot
cd spring-app && mvn -B -q -DskipTests package
mkdir -p /app/spring && cp spring-app/target/bench-spring.jar /app/spring/app.jar

# Plain Java
cd plain-app && rm -rf out && javac --release 21 -d out src/main/java/bench/plain/Main.java
mkdir -p /app/plain && jar --create --file /app/plain/app.jar --main-class bench.plain.Main -C plain-app/out .
```

## Start commands (CD)

```sh
JAVA_FLAGS="-XX:+UseSerialGC -XX:MaxRAMPercentage=75 -XX:-UsePerfData -Djava.security.egd=file:/dev/./urandom"

java $JAVA_FLAGS -jar /app/spring/app.jar --server.address=0.0.0.0 --server.port=8080
java $JAVA_FLAGS -jar /app/plain/app.jar  --host 0.0.0.0 --port 8080
```

Both apps use the same flags, so any difference comes from the framework, not the JVM settings.

## Notes for snapshot benchmarking

- `-XX:+UseSerialGC`: the JVM's default GC choice depends on the CPU and memory it sees, so the default can differ between VM sizes. Pinning SerialGC keeps runs comparable, and SerialGC is the smallest and quickest to start.
- `-Djava.security.egd=file:/dev/./urandom`: fresh microVMs often have little entropy right after boot, which can make the JVM's SecureRandom seeding (used by Tomcat session IDs) slow.
- When several VMs are restored from the same snapshot, they all start with the same JVM state, including SecureRandom seeds. That's fine for a benchmark, but it matters for production crypto.
- Optional cold-start tuning (not enabled, to keep a raw baseline): Spring AppCDS
  (`java -Djarmode=tools -jar app.jar extract` + `-XX:ArchiveClassesAtExit` with `-Dspring.context.exit=onRefresh`).
  The CDS archive has to be created by the same JVM build that runs it, so the training run would have to happen in the JRE runner image.
