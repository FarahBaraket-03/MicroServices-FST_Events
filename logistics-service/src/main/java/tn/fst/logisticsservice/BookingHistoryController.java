package tn.fst.logisticsservice;

import org.axonframework.eventhandling.GenericDomainEventMessage;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.web.bind.annotation.*;
import tn.fst.shared.Models.BookingChange;

// A separate stream keeps dated assignments distinct from the resource's manual state.
// Both active and deleted resources retain a searchable planning history.
@RestController
public class BookingHistoryController {
  private final EventStore store;

  public BookingHistoryController(EventStore store) { this.store = store; }

  @PostMapping("/internal/booking-history")
  public void record(@RequestBody BookingChange change) {
    String stream = "planning-" + change.resourceId();
    var history = store.readEvents(stream);
    long sequence = 0;
    while (history.hasNext()) {
      var entry = history.next();
      if (change.operationId().equals(entry.getMetaData().get("operationId"))) return;
      sequence = entry.getSequenceNumber() + 1;
    }
    store.publish(new GenericDomainEventMessage<>("BookingHistory", stream, sequence, change,
        java.util.Map.of("operationId", change.operationId())));
  }
}
