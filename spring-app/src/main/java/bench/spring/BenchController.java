package bench.spring;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BenchController {

    @GetMapping("/")
    public String root() {
        return "hello from spring\n";
    }

    @GetMapping("/health")
    public String health() {
        return "ok\n";
    }

    @GetMapping("/json")
    public Map<String, Object> json() {
        return Map.of(
                "server", "spring",
                "message", "hello",
                "ts", System.currentTimeMillis()
        );
    }
}
