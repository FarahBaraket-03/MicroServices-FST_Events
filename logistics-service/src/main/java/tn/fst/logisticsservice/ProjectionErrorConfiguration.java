package tn.fst.logisticsservice;

import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

/** Do not advance the event processor token when a projection update failed. */
@Configuration
public class ProjectionErrorConfiguration {
  @Autowired
  public void configure(EventProcessingConfigurer configurer) {
    configurer.registerListenerInvocationErrorHandler(
        "logistics-projection", configuration -> PropagatingErrorHandler.instance());
  }
}
