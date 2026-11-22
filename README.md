# FST Events Microservices

Migration of the supplied `eventsproject` Java source and Angular frontend into a local Spring Cloud project for the Architecture des composants DS.

**Prepared for your existing Docker network `devops` and existing containers `jenkins`, `sonarqube`, `nexus`, `prometheus`, `grafana`. This Compose project does not recreate or manage those five containers.**

Start with [Windows installation and startup](docs/SETUP_WINDOWS.md). Then follow [the demonstration](docs/DEMO.md), [existing DevOps tools](docs/DEVOPS.md) and [evaluation checklist](docs/EVALUATION.md).

## Architecture

| Module | Responsibility |
| --- | --- |
| config-server | Central YAML configuration, native backend mounted from this Git repository |
| eureka-server | Dynamic service discovery |
| api-gateway | Explicit API routes, service load balancing and restricted development CORS |
| participant-service | Participant CRUD and task enum; its own MySQL database |
| logistics-service | Axon commands/aggregate/events; MySQL query projection; its own MySQL database |
| event-service | Event CRUD, references by ID, Feign composition, statistics, resilience and optional cost job; its own MySQL database |
| shared-contracts | HTTP DTOs and shared input error handling; no shared JPA entities |
| frontend | Adapted Angular interface for event, participant and logistics operations |

Three MySQL containers isolate persistence. The two participant instances share the participant database. Axon Server stores event streams; logistics MySQL stores the read projection and processing tokens. CQRS runs inside one logistics deployment: separate command/query HTTP APIs do not require separate executables.

The normal cross-service business calls are synchronous REST through Feign. Axon commands/events are the separate messaging mechanism for CQRS.

## Versions

Java 17; Spring Boot 3.5.8; Spring Cloud 2025.0.1; Axon Framework 4.12.3; Resilience4j 2.3.0; Maven 3.9+; existing Angular 18 frontend; Node 22 LTS. Exact library dependencies are in the root/module POMs and frontend lockfile. These are pinned reproducible choices, not a claim to be the newest releases. Review dependency updates before deploying beyond this local exercise.

## Quick start (PowerShell, from this folder)

```powershell
Copy-Item .env.example .env
mvn -B -ntp clean verify
docker compose up -d --build --wait --wait-timeout 300
powershell -ExecutionPolicy Bypass -File scripts/smoke.ps1
```

In another terminal:

```powershell
cd frontend
npm ci
npm start
```

Open http://localhost:4200. Gateway: http://localhost:8090. Allow Eureka discovery to propagate on first startup; smoke.ps1 waits for it.

## Ports: your existing containers are preserved

| Component | Host URL / port | Docker address on devops |
| --- | --- | --- |
| Existing Jenkins | http://localhost:8080 | jenkins:8080 |
| Existing Nexus | http://localhost:8081 | nexus:8081 |
| Existing SonarQube | http://localhost:9000 | sonarqube:9000 |
| Existing Grafana | http://localhost:3000 | grafana:3000 |
| Existing Prometheus | http://localhost:9090 | prometheus:9090 |
| Angular | http://localhost:4200 | Runs on Windows |
| Gateway | http://localhost:8090 | api-gateway:8080 |
| Participant | http://localhost:8091 | participant-service:8081 |
| Logistics | http://localhost:8092 | logistics-service:8082 |
| Event | http://localhost:8093 | event-service:8083 |
| Participant instance 2 | http://localhost:8094 | participant-service-2:8081 |
| Config Server | http://localhost:8888 | config-server:8888 |
| Eureka | http://localhost:8761 | eureka-server:8761 |
| Zipkin | http://localhost:9411 | zipkin:9411 |
| Axon Server | http://localhost:8024 ; gRPC 8124 | axonserver:8024 ; axonserver:8124 |
| Participant MySQL | 127.0.0.1:3307 | mysql-participant:3306 |
| Logistics MySQL | 127.0.0.1:3308 | mysql-logistics:3306 |
| Event MySQL | 127.0.0.1:3309 | mysql-event:3306 |

Host ports are configurable in `.env`. Docker ports remain stable. `localhost` inside a container refers to that container, not Windows or another service.

## API contracts

All business requests from Angular/Postman go through `/api` on Gateway. The Gateway strips `/api` before forwarding.

| API | Purpose |
| --- | --- |
| GET/POST /api/participants | List/create participants |
| GET/PUT/DELETE /api/participants/{id} | Participant CRUD |
| GET/POST /api/events | List/create events |
| GET/PUT/DELETE /api/events/{id} | Event CRUD, ID references |
| GET /api/event-stats?from=2027-01-01&to=2027-12-31 | Composed statistics |
| GET /api/event-stats/{id} | Composed event statistics |
| GET /api/event-stats/summary | Global statistics |
| POST /api/logistics/commands | Create resource (202 with idLog) |
| PUT /api/logistics/commands/{id} | Update resource |
| PUT /api/logistics/commands/{id}/reservation?reserved=true | Reserve/release |
| DELETE /api/logistics/commands/{id} | Delete an unreserved resource |
| GET /api/logistics/queries | List projected resources |
| GET /api/logistics/queries/{id} | Read a projected resource |
| GET /api/demo/participant-instance | Observe Gateway load balancing |

OpenAPI/Swagger UI: http://localhost:8091/swagger-ui/index.html, http://localhost:8092/swagger-ui/index.html and http://localhost:8093/swagger-ui/index.html. Import `postman/Events-Microservices.postman_collection.json`. IDs for logistics are UUID strings; event and participant IDs are numeric.

## Important behavior

- No JPA relation crosses a service boundary. Event association tables store external IDs.
- Event creation/update verifies referenced resources via Feign. A missing dependency gives 503 instead of accepting unvalidated references. A missing referenced ID gives 400.
- Resilience retries protect remote **reads**, not resource creation. Three total attempts, 500ms between attempts; client connect/read timeouts are 1s/5s. A slow dependency can take around 16 seconds across three timed-out attempts; the 5s timeout is per attempt.
- The Circuit Breaker observes each failed attempt. Its six-call window can open after two groups of three failures. Half-open probes happen after the configured wait period.
- The `eventApi` Rate Limiter allows five statistics requests per second **per event-service instance**, shared across its statistics endpoints. It is not a distributed or user-specific API quota.
- Fallback returns `degraded=true`, warnings and null unknown totals. It never invents zero costs. A removed referenced resource is also reported as degraded.
- Logistics commands pass through Axon and the aggregate; queries pass through QueryGateway and `@QueryHandler`. `@EventHandler` updates the MySQL projection. A 202 command receipt does not mean the asynchronous projection is immediately visible.
- Axon command timeout is an unknown write outcome; query first before manually resubmitting it. There is no distributed transaction between event, participant and logistics services.
- The optional scheduled cost refresh updates all events with complete logistics data. It removes the monolith's hard-coded organizer name. It preserves stored costs when dependencies fail; enable via COST_JOB_ENABLED=true. Run a single event-service instance while using this job.
- Existing monolith database rows are not automatically imported. Original entity IDs/associations need an explicit migration if you want to retain those rows. The smoke script creates fresh demonstration data.
- Native Config Server still provides central configuration; the YAML lives in this Git repository. Clients need restart to pick up changes; live refresh/bus is not implemented.
- This is local classroom configuration: loopback host bindings, explicit Gateway routes, development CORS; no user authentication or production authorization. Admin/config/fault endpoints are not forwarded by the Gateway. Set fault injection off outside demos. Do not expose this stack publicly.

## Repository verification

See [VALIDATION.md](docs/VALIDATION.md) for the checks actually run and their limits. Docker engine tests and real existing-tool integration must be run on your PC. Do not treat the inclusion of a configuration file as proof of a successful live demonstration.

## GitHub

Follow [GITHUB.md](docs/GITHUB.md). Do not commit `.env`, credentials, `node_modules`, build outputs, tool data, or fabricated screenshots/results. The included GitHub Actions workflow builds/tests the backend and builds Angular. The Jenkinsfile uses your existing tools for Sonar and optional Nexus publishing; it does not auto-deploy.

## Official references

- Spring Cloud compatibility: https://spring.io/projects/spring-cloud/
- Spring Cloud OpenFeign: https://docs.spring.io/spring-cloud-openfeign/reference/
- Axon Framework: https://docs.axoniq.io/axon-framework-reference/
- Resilience4j Retry: https://resilience4j.readme.io/docs/retry
- Docker Desktop for Windows: https://docs.docker.com/desktop/setup/install/windows-install/
- Prometheus configuration: https://prometheus.io/docs/prometheus/latest/configuration/configuration/
