package tn.fst.eventservice;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import tn.fst.shared.Models.LogisticsView;

@FeignClient(name = "logistics-service", url = "${app.clients.logistics-url:}")
public interface LogisticsClient {
  @org.springframework.web.bind.annotation.PostMapping("/internal/booking-history")
  void recordBooking(@org.springframework.web.bind.annotation.RequestBody tn.fst.shared.Models.BookingChange change);

  @GetMapping("/logistics/queries")
  List<LogisticsView> all();
}
