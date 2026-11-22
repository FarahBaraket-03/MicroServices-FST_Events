package tn.fst.eventservice;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.*;
import org.springframework.stereotype.Service;
import tn.fst.shared.Models.*;

@Service
public class RemoteCatalogService {
  private final ParticipantClient participants;
  private final LogisticsClient logistics;

  public RemoteCatalogService(ParticipantClient participants, LogisticsClient logistics) {
    this.participants = participants;
    this.logistics = logistics;
  }

  // Retry is outside CircuitBreaker; each HTTP attempt is counted by the breaker.
  // Only the outer Retry owns fallback: failed attempts must reach both policies.
  @Retry(name = "participantRead", fallbackMethod = "participantFallback")
  @CircuitBreaker(name = "participantRead")
  public RemoteResult<ParticipantView> participants() {
    try {
      List<ParticipantView> rows = participants.all();
      if (rows == null
          || rows.stream()
              .anyMatch(
                  p ->
                      p == null
                          || p.idPart() == null
                          || p.nom() == null
                          || p.nom().isBlank()
                          || p.prenom() == null
                          || p.tache() == null))
        throw new RemoteDependencyException("Invalid participant payload");
      return RemoteResult.ok(rows);
    } catch (FeignException e) {
      throw new RemoteDependencyException("Participant HTTP call failed", e);
    }
  }

  @Retry(name = "logisticsRead", fallbackMethod = "logisticsFallback")
  @CircuitBreaker(name = "logisticsRead")
  public RemoteResult<LogisticsView> logistics() {
    try {
      List<LogisticsView> rows = logistics.all();
      if (rows == null
          || rows.stream()
              .anyMatch(
                  l ->
                      l == null
                          || l.idLog() == null
                          || l.idLog().isBlank()
                          || l.description() == null
                          || l.prixUnit() == null
                          || l.prixUnit().signum() < 0
                          || l.quantite() < 1))
        throw new RemoteDependencyException("Invalid logistics payload");
      return RemoteResult.ok(rows);
    } catch (FeignException e) {
      throw new RemoteDependencyException("Logistics HTTP call failed", e);
    }
  }

  public RemoteResult<ParticipantView> participantFallback(Throwable error) {
    return RemoteResult.unavailable("participant-service");
  }

  public RemoteResult<LogisticsView> logisticsFallback(Throwable error) {
    return RemoteResult.unavailable("logistics-service");
  }
}
