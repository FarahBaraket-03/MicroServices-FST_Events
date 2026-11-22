package tn.fst.eventservice;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import tn.fst.shared.Models.*;

@Entity
@Table(name = "events")
public class EventEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Version public Long version;

  @Column(nullable = false)
  public String description;

  @Column(nullable = false)
  public LocalDate dateDebut;

  @Column(nullable = false)
  public LocalDate dateFin;

  @Column(precision = 18, scale = 2)
  public BigDecimal cout;

  public Instant costUpdatedAt;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "event_participant_ids")
  public Set<Long> participantIds = new HashSet<>();

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "event_logistics_ids")
  public Set<String> logisticsIds = new HashSet<>();

  public EventView view() {
    return new EventView(
        id,
        description,
        dateDebut,
        dateFin,
        Set.copyOf(participantIds),
        Set.copyOf(logisticsIds),
        cout);
  }

  public void update(EventInput in) {
    description = in.description().trim();
    dateDebut = in.dateDebut();
    dateFin = in.dateFin();
    participantIds = new HashSet<>(in.participantIds());
    logisticsIds = new HashSet<>(in.logisticsIds());
  }
}
