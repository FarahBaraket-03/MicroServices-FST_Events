package tn.fst.eventservice;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import tn.fst.shared.Models.ParticipantView;

@FeignClient(name = "participant-service", url = "${app.clients.participant-url:}")
public interface ParticipantClient {
  @GetMapping("/participants")
  List<ParticipantView> all();
}
