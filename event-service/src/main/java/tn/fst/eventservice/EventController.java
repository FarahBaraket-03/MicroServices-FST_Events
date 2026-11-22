package tn.fst.eventservice;

import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.fst.shared.Models.*;

@RestController
public class EventController {
  private final EventService service;

  public EventController(EventService service) {
    this.service = service;
  }

  @GetMapping("/events")
  public List<EventView> all() {
    return service.all();
  }

  @GetMapping("/events/{id}")
  public EventView get(@PathVariable Long id) {
    return service.get(id);
  }

  @PostMapping("/events")
  @ResponseStatus(HttpStatus.CREATED)
  public EventView create(@Valid @RequestBody EventInput input) {
    return service.create(input);
  }

  @PutMapping("/events/{id}")
  public EventView update(@PathVariable Long id, @Valid @RequestBody EventInput input) {
    return service.update(id, input);
  }

  @DeleteMapping("/events/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    service.delete(id);
  }

  @GetMapping("/event-stats")
  @RateLimiter(name = "eventApi", fallbackMethod = "limitedStats")
  public List<EventStats> stats(
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return service.stats(from, to);
  }

  @GetMapping("/event-stats/{id}")
  @RateLimiter(name = "eventApi", fallbackMethod = "limitedOne")
  public EventStats one(@PathVariable Long id) {
    return service.stats(id);
  }

  @GetMapping("/event-stats/summary")
  @RateLimiter(name = "eventApi", fallbackMethod = "limitedSummary")
  public GlobalStats summary() {
    return service.summary();
  }

  public List<EventStats> limitedStats(LocalDate from, LocalDate to, RequestNotPermitted error) {
    throw limit();
  }

  public EventStats limitedOne(Long id, RequestNotPermitted error) {
    throw limit();
  }

  public GlobalStats limitedSummary(RequestNotPermitted error) {
    throw limit();
  }

  private ResponseStatusException limit() {
    return new ResponseStatusException(
        HttpStatus.TOO_MANY_REQUESTS,
        "Maximum 5 statistics requests per second per event-service instance");
  }
}
