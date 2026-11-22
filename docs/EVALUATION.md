# Evaluation traceability

Source: supplied DS assignment and evaluation grid. The grid gives totals below but no separate point breakdown for individual technical features. This checklist is a preparation aid, not a predicted grade.

| Rubric category | Points | Implementation / evidence |
| --- | ---: | --- |
| Travail effectué | 9 | Config Server, Eureka, Gateway, three business services, independent MySQL, REST/Feign, resilience, monitoring, load balancing, Axon CQRS |
| Scénario | 1 | Organizer -> logistics command -> projection -> event -> composed reserved cost; scripts/smoke.ps1 |
| Excellence | 3 | Adapted Angular, Sonar analysis; Nexus/Jenkins as additional extensions |
| Maîtrise code/concepts | 5 | Explain your actual code, design decisions and failure behavior |
| Git/JIRA | 2 | Real commits, branches/tasks and validation history |
| Total | 20 | No guaranteed grade |

## Required technical demonstrations

- [ ] Root Maven build and tests pass on your PC.
- [ ] Config Server serves event-service configuration from the central repository.
- [ ] Eureka shows all services and the optional second participant instance.
- [ ] Angular/Postman uses only Gateway for business calls.
- [ ] Each service owns its MySQL database; no cross-service JPA joins.
- [ ] Feign communication uses logical service names with LoadBalancer.
- [ ] Retry: three total attempts, 500ms wait; no stacked Feign retries.
- [ ] Rate Limiter: five statistics requests per second per instance; real 429 evidence.
- [ ] Circuit Breaker states and fallback are visible.
- [ ] Five failure cases are reproduced and restored using DEMO.md.
- [ ] Prometheus scrapes the real applications and Grafana displays live data.
- [ ] Zipkin shows the composed request trace with Feign calls.
- [ ] Commands, events, aggregate, EventHandler projector and QueryHandler are explained.
- [ ] Axon Server runs in Docker and contains actual events.
- [ ] Command/query REST APIs are exercised, including a rejected business transition.
- [ ] Sonar/Nexus/Jenkins evidence is from actual configured runs.
- [ ] Git/JIRA evidence reflects actual work.

## Concepts to explain in your own words

1. Why separate databases prevent JPA relations across microservices.
2. How Gateway routing, Eureka discovery and LoadBalancer differ.
3. Why Feign uses a service name instead of localhost or a fixed port.
4. Difference between timeout, Retry, Circuit Breaker, Rate Limiter and fallback.
5. Why repeated writes can create duplicates and why retries here protect reads.
6. Why fallback uses unknown/null rather than a made-up zero.
7. Difference between metrics (Micrometer/Prometheus), dashboards (Grafana) and traces (Zipkin).
8. Difference between a command, an event, a query and a read projection.
9. Why aggregate state comes from events and MySQL projection can lag.
10. Difference between EventSourcingHandler, EventHandler and QueryHandler.
11. Why two instances of the same service share their database but different services do not.
12. Why localhost inside a container differs from localhost on Windows.
13. What Sonar, Nexus and Jenkins each contribute.
14. What is still local-only and what would need hardening before production.

The assignment also says each student chooses a substantial module from the team's specification. Verify that this event-management scope is accepted for your team's cahier des charges.
