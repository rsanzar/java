package bench.quarkus;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.time.Instant;

@ApplicationScoped
public class StartupTimer {

    /** Prints process-start -> ready time, same format as the other servers. */
    void onStart(@Observes StartupEvent ev) {
        long start = ProcessHandle.current().info().startInstant()
                .map(Instant::toEpochMilli)
                .orElse(System.currentTimeMillis());
        System.out.println("[bench-quarkus] READY in "
                + (System.currentTimeMillis() - start) + " ms (since process start)");
    }
}
