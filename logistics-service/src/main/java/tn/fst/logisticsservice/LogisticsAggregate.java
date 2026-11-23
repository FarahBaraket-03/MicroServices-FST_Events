package tn.fst.logisticsservice;

import static org.axonframework.modelling.command.AggregateLifecycle.*;

import java.math.BigDecimal;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import tn.fst.logisticsservice.LogisticsMessages.*;

@Aggregate
public class LogisticsAggregate {
  @AggregateIdentifier private String id;
  private boolean deleted;
  private boolean reserved;

  protected LogisticsAggregate() {}

  @CommandHandler
  public LogisticsAggregate(Create cmd) {
    validate(cmd.description(), cmd.prixUnit(), cmd.quantite());
    apply(
        new Created(
            cmd.id(), cmd.description().trim(), cmd.reserve(), cmd.prixUnit(), cmd.quantite()));
  }

  @CommandHandler
  public void handle(Update cmd) {
    active();
    validate(cmd.description(), cmd.prixUnit(), cmd.quantite());
    boolean changed = reserved != cmd.reserve();
    apply(new Updated(id, cmd.description().trim(), reserved, cmd.prixUnit(), cmd.quantite()));
    if (changed) apply(new Reserved(id, cmd.reserve()));
  }

  @CommandHandler
  public void handle(Reserve cmd) {
    active();
    if (reserved == cmd.reserve())
      throw new org.axonframework.commandhandling.CommandExecutionException(
          "Resource already has this reservation state", null, "RESERVATION_STATE_CONFLICT");
    apply(new Reserved(id, cmd.reserve()));
  }

  @CommandHandler
  public String handle(Delete cmd) {
    active();
    if (reserved) {
      apply(new DeletionRefused(id, "RESOURCE_RESERVED"));
      return "RESERVED_DELETE_CONFLICT";
    }
    apply(new Deleted(id));
    return "DELETED";
  }

  @EventSourcingHandler
  public void on(Created evt) {
    id = evt.id();
    reserved = evt.reserve();
  }

  @EventSourcingHandler
  public void on(Updated evt) {
    reserved = evt.reserve();
  }

  @EventSourcingHandler
  public void on(Reserved evt) {
    reserved = evt.reserve();
  }

  @EventSourcingHandler
  public void on(Deleted evt) {
    deleted = true;
    markDeleted();
  }

  private void active() {
    if (deleted) throw new IllegalArgumentException("Resource is deleted");
  }

  private static void validate(String description, BigDecimal price, int quantity) {
    if (description == null || description.isBlank())
      throw new IllegalArgumentException("Description is required");
    if (price == null || price.signum() < 0)
      throw new IllegalArgumentException("Price cannot be negative");
    if (quantity < 1) throw new IllegalArgumentException("Quantity must be positive");
  }
}
