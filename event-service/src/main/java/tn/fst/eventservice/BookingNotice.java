package tn.fst.eventservice;

import jakarta.persistence.*;
import tn.fst.shared.Models.BookingChange;
import java.time.LocalDate;
import java.util.UUID;

@Entity
public class BookingNotice {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
  String operationId;
  Long eventId;
  String resourceId;
  LocalDate startDate, endDate;
  boolean assigned;

  protected BookingNotice() {}

  BookingNotice(EventEntity event, String resource, boolean assigned) {
    this.operationId = UUID.randomUUID().toString();
    this.eventId = event.id;
    this.resourceId = resource;
    this.startDate = event.dateDebut;
    this.endDate = event.dateFin;
    this.assigned = assigned;
  }

  BookingChange message() {
    return new BookingChange(operationId, eventId, resourceId, startDate, endDate, assigned);
  }
}
