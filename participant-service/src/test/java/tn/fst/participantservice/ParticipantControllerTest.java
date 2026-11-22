package tn.fst.participantservice;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = ParticipantController.class,
    properties = {
      "spring.config.location=optional:classpath:/test-application.yml",
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false"
    })
class ParticipantControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean ParticipantRepository repository;

  @Test
  void rejectsBlankNames() throws Exception {
    mvc.perform(
            post("/participants")
                .contentType("application/json")
                .content("{\"nom\":\"\",\"prenom\":\"Ahmed\",\"tache\":\"INVITE\"}"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(repository);
  }

  @Test
  void returns404ForUnknownParticipant() throws Exception {
    when(repository.findById(99L)).thenReturn(Optional.empty());
    mvc.perform(get("/participants/99")).andExpect(status().isNotFound());
  }
}
