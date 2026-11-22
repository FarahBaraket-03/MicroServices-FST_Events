package tn.fst.eventservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import tn.fst.shared.Models.*;

class EventServiceTest {
  EventRepository repository;
  RemoteCatalogService remote;
  EventService service;
  EventEntity event;

  @BeforeEach
  void setup() {
    repository = mock(EventRepository.class);
    remote = mock(RemoteCatalogService.class);
    service = new EventService(repository, remote);
    event = new EventEntity();
    event.id = 1L;
    event.description = "Conference";
    event.dateDebut = LocalDate.of(2027, 1, 1);
    event.dateFin = event.dateDebut.plusDays(1);
    event.participantIds.add(1L);
    event.logisticsIds.add("a");
    when(repository.findById(1L)).thenReturn(Optional.of(event));
    when(repository.findAll()).thenReturn(List.of(event));
  }

  @Test
  void unavailableLogisticsProducesUnknownCost() {
    when(remote.participants())
        .thenReturn(
            RemoteResult.ok(List.of(new ParticipantView(1L, "Test", "Ahmed", Tache.ORGANISATEUR))));
    when(remote.logistics()).thenReturn(RemoteResult.unavailable("logistics"));
    var stats = service.stats(1L);
    assertTrue(stats.degraded());
    assertNull(stats.coutLogistiqueReservee());
    assertEquals(1, stats.nbParticipants());
  }

  @Test
  void calculatesExactReservedCost() {
    when(remote.participants())
        .thenReturn(
            RemoteResult.ok(List.of(new ParticipantView(1L, "Test", "Ahmed", Tache.ORGANISATEUR))));
    when(remote.logistics())
        .thenReturn(
            RemoteResult.ok(
                List.of(new LogisticsView("a", "Chairs", true, new BigDecimal("0.10"), 3))));
    var stats = service.stats(1L);
    assertFalse(stats.degraded());
    assertEquals(new BigDecimal("0.30"), stats.coutLogistiqueReservee());
  }

  @Test
  void missingReferenceIsDegraded() {
    when(remote.participants()).thenReturn(RemoteResult.ok(List.of()));
    when(remote.logistics()).thenReturn(RemoteResult.ok(List.of()));
    var stats = service.stats(1L);
    assertTrue(stats.degraded());
    assertNull(stats.coutLogistiqueReservee());
    assertNull(stats.nbParticipants());
  }

  @Test
  void failedDependencyDoesNotOverwritePreviousCost() {
    event.cout = new BigDecimal("123.45");
    when(remote.logistics()).thenReturn(RemoteResult.unavailable("logistics"));
    service.refreshCosts();
    verify(repository, never()).save(any());
    assertEquals(new BigDecimal("123.45"), event.cout);
  }

  @Test
  void rejectsInvalidDatesBeforeRemoteCalls() {
    assertThrows(
        org.springframework.web.server.ResponseStatusException.class,
        () ->
            service.create(
                new EventInput(
                    "Conference",
                    LocalDate.of(2027, 2, 2),
                    LocalDate.of(2027, 2, 1),
                    Set.of(),
                    Set.of())));
    verifyNoInteractions(remote);
  }
}
