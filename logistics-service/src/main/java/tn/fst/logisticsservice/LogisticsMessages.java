package tn.fst.logisticsservice;
import tn.fst.shared.Models.LogisticsView;
import java.math.BigDecimal;
import java.util.List;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public final class LogisticsMessages {
  private LogisticsMessages() {}

  public record Create(
      @TargetAggregateIdentifier String id,
      String description,
      boolean reserve,
      BigDecimal prixUnit,
      int quantite) {}

  public record Update(
      @TargetAggregateIdentifier String id,
      String description,
      boolean reserve,
      BigDecimal prixUnit,
      int quantite) {}

  public record Reserve(@TargetAggregateIdentifier String id, boolean reserve) {}

  public record Delete(@TargetAggregateIdentifier String id) {}

  public record Created(
      String id, String description, boolean reserve, BigDecimal prixUnit, int quantite) {}

  public record Updated(
      String id, String description, boolean reserve, BigDecimal prixUnit, int quantite) {}

  public record Reserved(String id, boolean reserve) {}

  public record DeletionRefused(String id, String reason) {}

  public record Deleted(String id) {}

  public record FindOne(String id) {}

  public record FindAll() {}

  public record LogisticsList(List<LogisticsView> items) {}
}
