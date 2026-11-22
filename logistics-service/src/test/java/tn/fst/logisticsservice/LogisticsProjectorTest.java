package tn.fst.logisticsservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import tn.fst.logisticsservice.LogisticsMessages.*;

class LogisticsProjectorTest {
  @Test
  void projectionCreatesReadableModel() {
    var repo = mock(LogisticsReadRepository.class);
    var rows = new HashMap<String, LogisticsReadModel>();
    when(repo.save(any()))
        .thenAnswer(
            i -> {
              LogisticsReadModel row = i.getArgument(0);
              rows.put(row.id, row);
              return row;
            });
    when(repo.findById("a")).thenAnswer(i -> Optional.ofNullable(rows.get("a")));
    var projector = new LogisticsProjector(repo);
    projector.on(new Created("a", "Projector", false, new BigDecimal("50"), 2));
    projector.on(new Reserved("a", true));
    var view = projector.handle(new FindOne("a"));
    assertTrue(view.reserve());
    assertEquals(new BigDecimal("50"), view.prixUnit());
  }
}
