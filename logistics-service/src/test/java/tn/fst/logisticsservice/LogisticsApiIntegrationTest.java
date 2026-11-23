package tn.fst.logisticsservice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.inmemory.InMemoryEventStorageEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** Exercises the real Axon gateways and HTTP contracts; H2/in-memory storage only in this test. */
@SpringBootTest(
    properties = {
      "spring.config.location=optional:classpath:/test-application.yml",
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "axon.axonserver.enabled=false",
      "spring.datasource.url=jdbc:h2:mem:logistics;DB_CLOSE_DELAY=-1",
      "spring.datasource.username=sa",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=create-drop",
      "axon.serializer.general=jackson",
      "axon.serializer.events=jackson",
      "axon.serializer.messages=jackson",
      "axon.eventhandling.processors.logistics-projection.mode=subscribing"
    })
@AutoConfigureMockMvc
@Import(LogisticsApiIntegrationTest.LocalEvents.class)
class LogisticsApiIntegrationTest {
  @TestConfiguration
  static class LocalEvents {
    @Bean
    EventStorageEngine events() {
      return new InMemoryEventStorageEngine();
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper json;
  @Autowired org.axonframework.eventsourcing.eventstore.EventStore store;

  @Test
  void planningHistoryIsIdempotent() throws Exception {
    var body = """
        {"operationId":"msg-1","eventId":12,"resourceId":"projector","startDate":"2027-01-01",
         "endDate":"2027-01-03","assigned":true}
        """;
    for (int attempt = 0; attempt < 2; attempt++) {
      mvc.perform(post("/internal/booking-history").contentType("application/json").content(body))
          .andExpect(status().isOk());
    }
    var stream = store.readEvents("planning-projector");
    var change = (tn.fst.shared.Models.BookingChange) stream.next().getPayload();
    org.junit.jupiter.api.Assertions.assertEquals(12L, change.eventId());
    org.junit.jupiter.api.Assertions.assertFalse(stream.hasNext());
  }


  @Test
  void commandEventProjectionQueryAndRejectedTransition() throws Exception {
    String body =
        "{\"description\":\"Test chairs\",\"reserve\":false,\"prixUnit\":12.50,\"quantite\":4}";
    var response =
        mvc.perform(post("/logistics/commands").contentType("application/json").content(body))
            .andExpect(status().isAccepted())
            .andReturn();
    String id = json.readTree(response.getResponse().getContentAsString()).get("idLog").asText();
    mvc.perform(get("/logistics/queries/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.description").value("Test chairs"))
        .andExpect(jsonPath("$.reserve").value(false));
    mvc.perform(put("/logistics/commands/" + id + "/reservation").param("reserved", "true"))
        .andExpect(status().isAccepted());
    mvc.perform(get("/logistics/queries/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reserve").value(true));
    mvc.perform(put("/logistics/commands/" + id + "/reservation").param("reserved", "true"))
        .andExpect(status().isConflict());
    mvc.perform(delete("/logistics/commands/" + id)).andExpect(status().isConflict());
    mvc.perform(put("/logistics/commands/" + id + "/reservation").param("reserved", "false"))
        .andExpect(status().isAccepted());
    mvc.perform(delete("/logistics/commands/" + id)).andExpect(status().isAccepted());
    mvc.perform(get("/logistics/queries/" + id)).andExpect(status().isNotFound());
  }
}
