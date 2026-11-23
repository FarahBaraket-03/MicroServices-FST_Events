package tn.fst.eventservice;

import java.time.LocalDate;
import java.util.Set;
import org.springframework.data.jpa.repository.*;

public interface EventRepository extends JpaRepository<EventEntity, Long> {
  @Modifying
  @Query(value = "INSERT INTO booking_resource_lock (id) VALUES (?1) ON DUPLICATE KEY UPDATE id = id",
      nativeQuery = true)
  void lockBookingResource(String id);

  @Query("""
      select count(e) from EventEntity e join e.logisticsIds resourceId
      where resourceId in ?1 and e.dateDebut <= ?3 and e.dateFin >= ?2
      and (?4 is null or e.id <> ?4)
      """)
  long overlappingBookings(Set<String> ids, LocalDate start, LocalDate end, Long excluded);
}
