package tn.fst.eventservice;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tn.fst.shared.Models.*;

@SpringBootTest(
    classes = ResiliencePolicyTest.TestApp.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = {
      "spring.config.location=optional:classpath:/test-application.yml",
      "spring.cloud.config.enabled=false",
      "eureka.client.enabled=false",
      "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
      "resilience4j.retry.instances.participantRead.maxAttempts=3",
      "resilience4j.retry.instances.participantRead.waitDuration=1ms",
      "resilience4j.circuitbreaker.instances.participantRead.minimumNumberOfCalls=6",
      "resilience4j.circuitbreaker.instances.participantRead.slidingWindowSize=6",
      "resilience4j.ratelimiter.instances.eventApi.limitForPeriod=5",
      "resilience4j.ratelimiter.instances.eventApi.limitRefreshPeriod=1s",
      "resilience4j.ratelimiter.instances.eventApi.timeoutDuration=0"
    })
class ResiliencePolicyTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  @Import(RemoteCatalogService.class)
  static class TestApp {}

  @MockitoBean ParticipantClient participants;
  @MockitoBean LogisticsClient logistics;
  @Autowired RemoteCatalogService remote;
  @Autowired CircuitBreakerRegistry breakers;
  @Autowired RateLimiterRegistry limiters;

  @Test
  void threeFailuresThenFallbackAndBreakerOpens() {
    var breaker = breakers.circuitBreaker("participantRead");
    breaker.reset();
    when(participants.all()).thenThrow(new RemoteDependencyException("offline"));
    assertFalse(remote.participants().available());
    verify(participants, times(3)).all();
    assertFalse(remote.participants().available());
    verify(participants, times(6)).all();
    assertEquals(
        io.github.resilience4j.circuitbreaker.CircuitBreaker.State.OPEN, breaker.getState());
    assertFalse(remote.participants().available());
    verify(participants, times(6)).all();
  }

  @Test
  void limiterGrantsExactlyFivePermissions() {
    var limiter = limiters.rateLimiter("eventApi");
    for (int i = 0; i < 5; i++) assertTrue(limiter.acquirePermission());
    assertFalse(limiter.acquirePermission());
  }
}
