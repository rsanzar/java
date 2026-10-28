package bench.plain;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;

/**
 * Zero-dependency HTTP server (JDK built-in com.sun.net.httpserver + virtual threads).
 * Exposes the same endpoints as the Spring app: /, /health, /json
 *
 * Usage: java -jar plain-server.jar --host 0.0.0.0 --port 8080
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        String host = "0.0.0.0";
        int port = 8080;
        for (int i = 0; i < args.length - 1; i++) {
            switch (args[i]) {
                case "--host" -> host = args[++i];
                case "--port" -> port = Integer.parseInt(args[++i]);
                default -> { }
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(host, port), 65535);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

        server.createContext("/", ex -> {
            if (!"/".equals(ex.getRequestURI().getPath())) {
                send(ex, 404, "text/plain", "not found\n");
            } else {
                send(ex, 200, "text/plain", "hello from plain java\n");
            }
        });
        server.createContext("/health", ex -> send(ex, 200, "text/plain", "ok\n"));
        server.createContext("/json", ex -> send(ex, 200, "application/json",
                "{\"server\":\"plain\",\"message\":\"hello\",\"ts\":" + System.currentTimeMillis() + "}"));

        server.start();

        long jvmStart = ProcessHandle.current().info().startInstant()
                .map(Instant::toEpochMilli)
                .orElse(System.currentTimeMillis());
        System.out.println("[bench-plain] READY on " + host + ":" + port + " in "
                + (System.currentTimeMillis() - jvmStart) + " ms (since process start)");
    }

    private static void send(HttpExchange ex, int status, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", contentType);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
