package tn.fst.eventservice;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.cost-job-enabled", havingValue = "true")
public class CostScheduler {
  private final EventService service;

  public CostScheduler(EventService service) {
    this.service = service;
  }

  @Scheduled(fixedDelayString = "${app.cost-job-delay:60000}")
  public void run() {
    service.refreshCosts();
  }
}
