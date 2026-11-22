package tn.fst.logisticsservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import tn.fst.shared.ApiExceptionHandler;

@SpringBootApplication
@Import(ApiExceptionHandler.class)
public class LogisticsServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(LogisticsServiceApplication.class, args);
  }
}
