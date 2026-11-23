package tn.fst.logisticsservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import org.axonframework.axonserver.connector.AxonServerConfiguration;
import org.axonframework.axonserver.connector.command.CommandSerializer;
import org.axonframework.commandhandling.*;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.serialization.json.JacksonSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class RemoteRejectionTest {
  @Test
  void duplicateReservationRetainsConflictAcrossAxonWireFormat() {
    var aggregate = new LogisticsAggregate();
    aggregate.on(new LogisticsMessages.Created("a", "Chairs", true, BigDecimal.ONE, 4));
    var duplicate = new LogisticsMessages.Reserve("a", true);
    var rejection = assertThrows(CommandExecutionException.class, () -> aggregate.handle(duplicate));
    var wire = new CommandSerializer(JacksonSerializer.builder().build(), new AxonServerConfiguration());
    var response = wire.serialize(new GenericCommandResultMessage<>(rejection), "test");
    Throwable remote = wire.deserialize(response).exceptionResult();
    var gateway = mock(CommandGateway.class);
    when(gateway.send(duplicate)).thenReturn(CompletableFuture.failedFuture(remote));
    var api = new LogisticsController(gateway, mock(QueryGateway.class));
    assertEquals(409, assertThrows(ResponseStatusException.class,
        () -> api.reserve("a", true)).getStatusCode().value());
  }
}
