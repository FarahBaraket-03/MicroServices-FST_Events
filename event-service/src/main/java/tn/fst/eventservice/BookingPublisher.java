package tn.fst.eventservice;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BookingPublisher {
  private final BookingNotices notices;
  private final LogisticsClient logistics;

  public BookingPublisher(BookingNotices notices, LogisticsClient logistics) {
    this.notices = notices;
    this.logistics = logistics;
  }

  @Scheduled(fixedDelay = 2000)
  public void deliver() {
    for (var notice : notices.findTop50ByOrderByIdAsc()) {
      logistics.recordBooking(notice.message());
      notices.deleteById(notice.id);
    }
  }
}
