package tn.fst.logisticsservice;

import java.math.BigDecimal;
import org.axonframework.test.aggregate.AggregateTestFixture;
import org.junit.jupiter.api.*;
import tn.fst.logisticsservice.LogisticsMessages.*;

class LogisticsAggregateTest {
  private AggregateTestFixture<LogisticsAggregate> fixture;
  private final BigDecimal price = new BigDecimal("25.00");

  @BeforeEach
  void setup() {
    fixture = new AggregateTestFixture<>(LogisticsAggregate.class);
  }

  @Test
  void createProducesEvent() {
    fixture
        .givenNoPriorActivity()
        .when(new Create("a", "Chairs", false, price, 4))
        .expectSuccessfulHandlerExecution()
        .expectEvents(new Created("a", "Chairs", false, price, 4));
  }

  @Test
  void rejectsNegativePrice() {
    fixture
        .givenNoPriorActivity()
        .when(new Create("a", "Chairs", false, new BigDecimal("-1"), 4))
        .expectException(IllegalArgumentException.class)
        .expectNoEvents();
  }

  @Test
  void rejectsZeroQuantity() {
    fixture
        .givenNoPriorActivity()
        .when(new Create("a", "Chairs", false, price, 0))
        .expectException(IllegalArgumentException.class)
        .expectNoEvents();
  }

  @Test
  void reservationProducesEvent() {
    fixture
        .given(new Created("a", "Chairs", false, price, 4))
        .when(new Reserve("a", true))
        .expectSuccessfulHandlerExecution()
        .expectEvents(new Reserved("a", true));
  }

  @Test
  void rejectsDuplicateReservation() {
    fixture
        .given(new Created("a", "Chairs", true, price, 4))
        .when(new Reserve("a", true))
        .expectException(org.axonframework.commandhandling.CommandExecutionException.class)
        .expectNoEvents();
  }

  @Test
  void reservedResourceCannotBeDeleted() {
    fixture
        .given(new Created("a", "Chairs", true, price, 4))
        .when(new Delete("a"))
        .expectResultMessagePayload("RESERVED_DELETE_CONFLICT")
        .expectEvents(new DeletionRefused("a", "RESOURCE_RESERVED"));
  }

  @Test
  void releasedResourceCanBeDeleted() {
    fixture
        .given(new Created("a", "Chairs", true, price, 4), new Reserved("a", false))
        .when(new Delete("a"))
        .expectSuccessfulHandlerExecution()
        .expectEvents(new Deleted("a"))
        .expectMarkedDeleted();
  }
}
