package tn.fst.shared;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** HTTP contracts only: no shared JPA entities or repositories. */
public final class Models {
  private Models() {}

  public enum Tache {
    INVITE,
    ORGANISATEUR,
    SERVEUR,
    ANIMATEUR
  }

  public record ParticipantInput(
      @NotBlank @Size(max = 255) String nom,
      @NotBlank @Size(max = 255) String prenom,
      @NotNull Tache tache) {}

  public record ParticipantView(Long idPart, String nom, String prenom, Tache tache) {}

  public record LogisticsInput(
      @NotBlank @Size(max = 255) String description,
      boolean reserve,
      @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal prixUnit,
      @Min(1) int quantite) {}

  public record LogisticsView(
      String idLog, String description, boolean reserve, BigDecimal prixUnit, int quantite) {}

  public record EventInput(
      @NotBlank @Size(max = 255) String description,
      @NotNull LocalDate dateDebut,
      @NotNull LocalDate dateFin,
      @NotNull Set<@NotNull @Positive Long> participantIds,
      @NotNull Set<@NotBlank String> logisticsIds) {}

  public record EventView(
      Long idEvent,
      String description,
      LocalDate dateDebut,
      LocalDate dateFin,
      Set<Long> participantIds,
      Set<String> logisticsIds,
      BigDecimal cout) {}

  public record EventStats(
      Long idEvent,
      String description,
      LocalDate dateDebut,
      LocalDate dateFin,
      long dureeJours,
      Integer nbParticipants,
      Map<Tache, Long> participantsParTache,
      Integer nbLogistics,
      Integer nbLogisticsReservees,
      BigDecimal coutLogistiqueReservee,
      boolean degraded,
      List<String> warnings) {}

  public record GlobalStats(
      long nbEvents,
      long nbEventsAVenir,
      Long nbParticipants,
      Long nbLogistics,
      Long nbLogisticsReservees,
      BigDecimal coutTotalLogistiqueReservee,
      boolean degraded,
      List<String> warnings) {}

  public record CommandReceipt(String idLog, String status) {}
}
