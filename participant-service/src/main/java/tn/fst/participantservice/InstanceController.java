package tn.fst.participantservice;

import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

@RestController
public class InstanceController {
  private final Environment env;

  public InstanceController(Environment env) {
    this.env = env;
  }

  @GetMapping("/internal/instance")
  public Map<String, String> instance() {
    return Map.of(
        "service",
        env.getProperty("spring.application.name", "unknown"),
        "instance",
        env.getProperty("eureka.instance.instance-id", "local"));
  }
}
