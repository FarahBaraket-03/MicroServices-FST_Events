package tn.fst.logisticsservice;

import jakarta.validation.Valid;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tn.fst.logisticsservice.LogisticsMessages.*;
import tn.fst.shared.Models.*;

@RestController
@RequestMapping("/logistics")
public class LogisticsController {
  private final CommandGateway commands;
  private final QueryGateway queries;

  public LogisticsController(CommandGateway commands, QueryGateway queries) {
    this.commands = commands;
    this.queries = queries;
  }

  @PostMapping("/commands")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CommandReceipt create(@Valid @RequestBody LogisticsInput in) {
    String id = UUID.randomUUID().toString();
    send(new Create(id, in.description(), in.reserve(), in.prixUnit(), in.quantite()));
    return new CommandReceipt(id, "ACCEPTED");
  }

  @PutMapping("/commands/{id}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CommandReceipt update(@PathVariable String id, @Valid @RequestBody LogisticsInput in) {
    send(new Update(id, in.description(), in.reserve(), in.prixUnit(), in.quantite()));
    return new CommandReceipt(id, "ACCEPTED");
  }

  @PutMapping("/commands/{id}/reservation")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CommandReceipt reserve(@PathVariable String id, @RequestParam boolean reserved) {
    send(new Reserve(id, reserved));
    return new CommandReceipt(id, "ACCEPTED");
  }

  @DeleteMapping("/commands/{id}")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public CommandReceipt delete(@PathVariable String id) {
    send(new Delete(id));
    return new CommandReceipt(id, "ACCEPTED");
  }

@GetMapping("/queries")
public List<LogisticsView> all() {
    return queries
        .query(new FindAll(), ResponseTypes.instanceOf(LogisticsMessages.LogisticsList.class))
        .orTimeout(5, TimeUnit.SECONDS)
        .join()
        .items();
}

  @GetMapping("/queries/{id}")
  public LogisticsView get(@PathVariable String id) {
    LogisticsView v =
        queries
            .query(new FindOne(id), ResponseTypes.instanceOf(LogisticsView.class))
            .orTimeout(5, TimeUnit.SECONDS)
            .join();
    if (v == null)
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND, "Logistics not found or projection pending");
    return v;
  }

  private void send(Object command) {
    try {
      Object outcome = commands.send(command).get(5, TimeUnit.SECONDS);
      if ("RESERVED_DELETE_CONFLICT".equals(outcome))
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Libérez la ressource avant de la supprimer.");
    } catch (java.util.concurrent.TimeoutException e) {
      throw new ResponseStatusException(
          HttpStatus.GATEWAY_TIMEOUT,
          "Command outcome is unknown; check the query before retrying a write",
          e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Command interrupted", e);
    } catch (java.util.concurrent.ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof org.axonframework.modelling.command.AggregateNotFoundException)
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not found", e);
      for (Throwable failure = cause; failure != null; failure = failure.getCause()) {
        if (failure instanceof org.axonframework.commandhandling.CommandExecutionException commandFailure) {
          Object detail = commandFailure.getDetails().orElse(null);
          if ("RESERVATION_STATE_CONFLICT".equals(detail))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "La ressource est déjà dans cet état de réservation.", e);
          if ("RESERVED_DELETE_CONFLICT".equals(detail))
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Libérez la ressource avant de la supprimer.", e);
        }
      }
      if (cause instanceof IllegalArgumentException)
        throw new ResponseStatusException(HttpStatus.CONFLICT, cause.getMessage(), e);
      throw new ResponseStatusException(
          HttpStatus.SERVICE_UNAVAILABLE, "Command failed; inspect Axon and database health", e);
    }
  }
}
