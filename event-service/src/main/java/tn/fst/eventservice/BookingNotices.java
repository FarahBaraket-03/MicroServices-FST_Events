package tn.fst.eventservice;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingNotices extends JpaRepository<BookingNotice, Long> {
  List<BookingNotice> findTop50ByOrderByIdAsc();
}
