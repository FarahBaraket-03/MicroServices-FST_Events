package tn.fst.eventservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;
import tn.fst.shared.Models.*;

@SpringBootTest(classes = EventBookingTest.Db.class, properties = {
    "spring.config.location=optional:classpath:/test-application.yml",
    "spring.cloud.config.enabled=false", "eureka.client.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:booking;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"})
class EventBookingTest {
  @org.springframework.boot.autoconfigure.EnableAutoConfiguration
  @org.springframework.context.annotation.Import(EventService.class)
  static class Db {}
  @Autowired EventService service;
  @MockitoBean RemoteCatalogService catalog;
  @Autowired BookingNotices notices;

  @Test
  void editsRecordOnlyChangedAssignmentsAndDeleteReleasesThem() {
    available("screen");
    var saved = service.create(input("screen", 1, 2));
    service.update(saved.idEvent(), input("screen", 1, 2));
    service.update(saved.idEvent(), input("screen", 5, 6));
    service.delete(saved.idEvent());
    var history = notices.findTop50ByOrderByIdAsc().stream().map(BookingNotice::message)
        .filter(change -> change.eventId().equals(saved.idEvent())).toList();
    assertEquals(List.of(true, false, true, false), history.stream().map(BookingChange::assigned).toList());
    assertEquals(List.of(1, 1, 5, 5), history.stream().map(change -> change.startDate().getDayOfMonth()).toList());
  }

  EventInput input(String resource, int start, int end) {
    return new EventInput("Demo", LocalDate.of(2027, 1, start), LocalDate.of(2027, 1, end),
        Set.of(), Set.of(resource));
  }

  void available(String resource) {
    when(catalog.logistics()).thenReturn(RemoteResult.ok(List.of(
        new LogisticsView(resource, "Chairs", false, BigDecimal.ONE, 1))));
  }

  @Test
  void protectsInclusiveDatesAndReleasesAfterDelete() {
    available("chairs");
    var first = service.create(input("chairs", 1, 3));
    service.update(first.idEvent(), input("chairs", 1, 3));
    var second = service.create(input("chairs", 4, 6));
    var conflict = assertThrows(ResponseStatusException.class,
        () -> service.update(second.idEvent(), input("chairs", 3, 6)));
    assertEquals(409, conflict.getStatusCode().value());
    service.delete(first.idEvent());
    service.update(second.idEvent(), input("chairs", 3, 6));
  }

  @Test
  void simultaneousFirstBookingsHaveOneWinner() throws Exception {
    available("projector");
    var start = new CyclicBarrier(2);
    Callable<Integer> create = () -> {
      start.await(10, TimeUnit.SECONDS);
      try {
        service.create(input("projector", 5, 5));
        return 201;
      } catch (ResponseStatusException e) {
        return e.getStatusCode().value();
      }
    };
    var pool = Executors.newFixedThreadPool(2);
    try {
      var futures = pool.invokeAll(List.of(create, create), 20, TimeUnit.SECONDS);
      assertEquals(Set.of(201, 409), Set.of(futures.get(0).get(), futures.get(1).get()));
    } finally {
      pool.shutdownNow();
    }
  }
}
