package tn.fst.eventservice;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.fst.shared.Models.*;

@Service
public class EventService {
  private final EventRepository repository;
  private final RemoteCatalogService remote;

  public EventService(EventRepository repository, RemoteCatalogService remote) {
    this.repository = repository;
    this.remote = remote;
  }

  public List<EventView> all() {
    return repository.findAll().stream().map(EventEntity::view).toList();
  }

  public EventView get(Long id) {
    return find(id).view();
  }

  public EventView create(EventInput input) {
    validate(input);
    EventEntity e = new EventEntity();
    e.update(input);
    return repository.save(e).view();
  }

  public EventView update(Long id, EventInput input) {
    EventEntity e = find(id);
    validate(input);
    e.update(input);
    e.cout = null;
    e.costUpdatedAt = null;
    return repository.save(e).view();
  }

  public void delete(Long id) {
    repository.delete(find(id));
  }

  private EventEntity find(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
  }

  private void validate(EventInput input) {
    if (input.dateFin().isBefore(input.dateDebut()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date precedes start date");
    if (!input.participantIds().isEmpty()) {
      var p = remote.participants();
      if (!p.available())
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "Cannot validate participant references");
      Set<Long> ids = new HashSet<>();
      p.data().forEach(x -> ids.add(x.idPart()));
      if (!ids.containsAll(input.participantIds()))
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown participant reference");
    }
    if (!input.logisticsIds().isEmpty()) {
      var l = remote.logistics();
      if (!l.available())
        throw new ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE, "Cannot validate logistics references");
      Set<String> ids = new HashSet<>();
      l.data().forEach(x -> ids.add(x.idLog()));
      if (!ids.containsAll(input.logisticsIds()))
        throw new ResponseStatusException(
            HttpStatus.BAD_REQUEST, "Unknown logistics reference or projection pending");
    }
  }

  public List<EventStats> stats(LocalDate from, LocalDate to) {
    if (from != null && to != null && from.isAfter(to))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date interval");
    var p = remote.participants();
    var l = remote.logistics();
    return repository.findAll().stream()
        .filter(
            e ->
                (from == null || !e.dateDebut.isBefore(from))
                    && (to == null || !e.dateDebut.isAfter(to)))
        .sorted(Comparator.comparing(e -> e.dateDebut))
        .map(e -> build(e, p, l))
        .toList();
  }

  public EventStats stats(Long id) {
    var e = find(id);
    return build(e, remote.participants(), remote.logistics());
  }

  public GlobalStats summary() {
    var p = remote.participants();
    var l = remote.logistics();
    var events = repository.findAll();
    return new GlobalStats(
        events.size(),
        events.stream().filter(e -> !e.dateDebut.isBefore(LocalDate.now())).count(),
        p.available() ? (long) p.data().size() : null,
        l.available() ? (long) l.data().size() : null,
        l.available() ? l.data().stream().filter(LogisticsView::reserve).count() : null,
        l.available() ? sum(l.data()) : null,
        !p.available() || !l.available(),
        warnings(p, l));
  }

  private EventStats build(
      EventEntity e, RemoteResult<ParticipantView> p, RemoteResult<LogisticsView> l) {
    var selectedP = p.data().stream().filter(x -> e.participantIds.contains(x.idPart())).toList();
    var selectedL = l.data().stream().filter(x -> e.logisticsIds.contains(x.idLog())).toList();
    boolean pValid = p.available() && selectedP.size() == e.participantIds.size();
    boolean lValid = l.available() && selectedL.size() == e.logisticsIds.size();
    var warning = warnings(p, l);
    if (p.available() && !pValid) warning.add("Some participant references no longer exist");
    if (l.available() && !lValid)
      warning.add("Some logistics references no longer exist or projection is pending");
    Map<Tache, Long> tasks = new EnumMap<>(Tache.class);
    for (Tache t : Tache.values())
      tasks.put(t, selectedP.stream().filter(x -> x.tache() == t).count());
    return new EventStats(
        e.id,
        e.description,
        e.dateDebut,
        e.dateFin,
        ChronoUnit.DAYS.between(e.dateDebut, e.dateFin) + 1,
        pValid ? selectedP.size() : null,
        pValid ? tasks : null,
        lValid ? selectedL.size() : null,
        lValid ? (int) selectedL.stream().filter(LogisticsView::reserve).count() : null,
        lValid ? sum(selectedL) : null,
        !pValid || !lValid,
        warning);
  }

  private static BigDecimal sum(List<LogisticsView> rows) {
    return rows.stream()
        .filter(LogisticsView::reserve)
        .map(x -> x.prixUnit().multiply(BigDecimal.valueOf(x.quantite())))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private static List<String> warnings(RemoteResult<?>... results) {
    List<String> w = new ArrayList<>();
    for (var r : results) if (!r.available()) w.add(r.warning());
    return w;
  }

  // A degraded dependency never overwrites a valid persisted cost with zero.
  public void refreshCosts() {
    var rows = remote.logistics();
    if (!rows.available()) return;
    for (var e : repository.findAll()) {
      var selected = rows.data().stream().filter(l -> e.logisticsIds.contains(l.idLog())).toList();
      if (selected.size() != e.logisticsIds.size()) continue;
      e.cout = sum(selected);
      e.costUpdatedAt = Instant.now();
      repository.save(e);
    }
  }
}
