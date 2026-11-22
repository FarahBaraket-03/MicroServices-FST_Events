# Delivery validation - 22 November 2026

## Checks completed

| Check | Result |
| --- | --- |
| Maven reactor `mvn verify`, JDK 17 | PASS: all eight reactor projects, including parent; six executable apps and shared library packaged |
| Automated tests | PASS: 19 tests, zero failures/errors/skips |
| Participant controller validation and unknown-ID handling | PASS: 2 tests |
| Logistics aggregate business rules | PASS: 7 tests |
| Logistics projector | PASS: 1 test |
| Logistics HTTP -> real Axon command gateway -> event -> projection -> real query gateway | PASS: 1 integration test with H2 and in-memory event storage, subscribing processor |
| Event composition, exact reserved cost, missing references and preservation of stored cost on failure | PASS: 5 tests |
| Semantically corrupt remote payload detection | PASS: 1 test |
| Actual Resilience4j AOP retry/fallback/circuit transition and rate limiter policy | PASS: 2 tests |
| Angular production `npm run build` | PASS |
| Config Server native backend runtime | PASS: started application and fetched central event-service resilience properties |
| Gateway route/runtime security boundary | PASS: started Gateway, stripped /api prefix, routed by service name; unlisted admin route returned 404 |
| Eureka/Gateway runtime integration | PASS: started Eureka, registered two HTTP stub instances, observed Gateway calls reaching both via discovery/LoadBalancer |
| Compose schema and YAML uniqueness | PASS: main Compose and demo/bad-URL overlays validate; no duplicate keys |
| Docker image tags | Registry manifests confirmed for mysql:8.4, axoniq/axonserver:2024.2.2, openzipkin/zipkin:3.5, eclipse-temurin:17-jre-jammy |

The source was formatted and the full Maven verification passed again afterward. The Resilience4j BOM is pinned to keep its modules on the same version rather than mixing transitive Spring Cloud versions. Test mocks use the subclass mock maker, so tests do not need runtime self-attachment.

## Checks to run on your PC

This execution environment has no Docker engine/CLI or Windows PowerShell. The following are prepared, **not claimed as executed**:

- Full Compose startup with the three real MySQL containers and Axon Server Docker image.
- Production tracking processor recovery after real database interruption (the CQRS integration test uses subscribing processing for deterministic local assertions).
- The provided Windows PowerShell scripts, full five-failure demonstrations and two real participant JVM/container instances.
- Your existing Prometheus/Grafana configuration, a complete Zipkin trace under the Docker stack, Sonar quality gate, Nexus publication and Jenkins execution.
- GitHub push/Actions execution and Jira import/history.

Use SETUP_WINDOWS.md, DEMO.md and DEVOPS.md to perform these checks. The runtime Gateway tests used HTTP stubs, not the production business services. Image manifest availability is not proof that the Docker containers boot. No coverage percentage or grade is promised.
