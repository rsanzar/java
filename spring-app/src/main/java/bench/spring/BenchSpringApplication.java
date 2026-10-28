package bench.spring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class BenchSpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(BenchSpringApplication.class, args);
    }

    /** Prints JVM-start -> ready-to-serve time, same format as the plain server. */
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        long jvmStart = ProcessHandle.current().info().startInstant()
                .map(java.time.Instant::toEpochMilli)
                .orElse(System.currentTimeMillis());
        System.out.println("[bench-spring] READY in "
                + (System.currentTimeMillis() - jvmStart) + " ms (since process start)");
    }
}
