package bench.quarkus;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;

@Path("/")
@RunOnVirtualThread // same request model as the Spring and plain servers
public class BenchResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String root() {
        return "hello from quarkus\n";
    }

    @GET
    @Path("health")
    @Produces(MediaType.TEXT_PLAIN)
    public String health() {
        return "ok\n";
    }

    @GET
    @Path("json")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> json() {
        return Map.of(
                "server", "quarkus",
                "message", "hello",
                "ts", System.currentTimeMillis()
        );
    }
}
