package tn.fst.eventservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import tn.fst.shared.Models.*;

class RemoteCatalogServiceTest {
  @Test
  void rejectsSemanticallyCorruptHttp200Payload() {
    var p = mock(ParticipantClient.class);
    var l = mock(LogisticsClient.class);
    when(p.all()).thenReturn(List.of(new ParticipantView(null, "Test", "Ahmed", Tache.INVITE)));
    assertThrows(
        RemoteDependencyException.class, () -> new RemoteCatalogService(p, l).participants());
  }
}
