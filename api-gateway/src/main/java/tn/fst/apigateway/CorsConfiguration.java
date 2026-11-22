package tn.fst.apigateway;

import java.util.List;
import org.springframework.context.annotation.*;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.*;

@Configuration
class GatewayCorsConfiguration {
  @Bean
  CorsWebFilter corsWebFilter() {
    var c = new CorsConfiguration();
    c.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("Content-Type", "Accept", "traceparent", "b3"));
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", c);
    return new CorsWebFilter(source);
  }
}
