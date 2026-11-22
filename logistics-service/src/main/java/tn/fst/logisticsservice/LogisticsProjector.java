package tn.fst.logisticsservice;

import java.util.*;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.fst.logisticsservice.LogisticsMessages.*;
import tn.fst.shared.Models.LogisticsView;

@Component
@ProcessingGroup("logistics-projection")
public class LogisticsProjector {
  private final LogisticsReadRepository repository;

  public LogisticsProjector(LogisticsReadRepository repository) {
    this.repository = repository;
  }

  @EventHandler
  @Transactional
  public void on(Created evt) {
    LogisticsReadModel row = new LogisticsReadModel();
    row.id = evt.id();
    row.description = evt.description();
    row.reserve = evt.reserve();
    row.prixUnit = evt.prixUnit();
    row.quantite = evt.quantite();
    repository.save(row);
  }

  @EventHandler
  @Transactional
  public void on(Updated evt) {
    LogisticsReadModel row = required(evt.id());
    row.description = evt.description();
    row.reserve = evt.reserve();
    row.prixUnit = evt.prixUnit();
    row.quantite = evt.quantite();
    repository.save(row);
  }

  @EventHandler
  @Transactional
  public void on(Reserved evt) {
    LogisticsReadModel row = required(evt.id());
    row.reserve = evt.reserve();
    repository.save(row);
  }

  @EventHandler
  @Transactional
  public void on(Deleted evt) {
    repository.deleteById(evt.id());
  }

 @QueryHandler
@Transactional(readOnly = true)
public LogisticsMessages.LogisticsList handle(FindAll query) {
    return new LogisticsMessages.LogisticsList(
        repository.findAll().stream().map(LogisticsReadModel::view).toList()
    );
}

  @QueryHandler
  @Transactional(readOnly = true)
  public LogisticsView handle(FindOne query) {
    return repository.findById(query.id()).map(LogisticsReadModel::view).orElse(null);
  }

  private LogisticsReadModel required(String id) {
    return repository
        .findById(id)
        .orElseThrow(
            () -> new IllegalStateException("Projection missing previous event for " + id));
  }
}
