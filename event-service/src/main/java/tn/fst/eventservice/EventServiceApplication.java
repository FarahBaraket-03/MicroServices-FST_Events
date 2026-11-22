package tn.fst.eventservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import tn.fst.shared.ApiExceptionHandler;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
@Import(ApiExceptionHandler.class)
public class EventServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(EventServiceApplication.class, args);
  }
}
