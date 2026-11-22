package tn.fst.eventservice;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import tn.fst.shared.Models.LogisticsView;

@FeignClient(name = "logistics-service", url = "${app.clients.logistics-url:}")
public interface LogisticsClient {
  @GetMapping("/logistics/queries")
  List<LogisticsView> all();
}
