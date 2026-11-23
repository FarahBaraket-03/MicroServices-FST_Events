package tn.fst.eventservice;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "booking_resource_lock")
public class BookingResourceLock {
  @Id private String id;
  protected BookingResourceLock() {}
}
