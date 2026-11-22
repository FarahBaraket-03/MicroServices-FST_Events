# Install and run on your Windows PC

Your existing Docker Desktop, network `devops`, Jenkins (8080), Nexus (8081), SonarQube (9000), Grafana (3000), Prometheus (9090) are reused. Do not reinstall them or remove their containers/volumes.

## 1. Install or verify

| Tool | Need | Check | Official download |
| --- | --- | --- | --- |
| JDK 17 | Required for the chosen baseline; JDK 21 can also compile with release 17 | java -version; javac -version | https://adoptium.net/temurin/releases/?version=17 |
| Maven 3.9+ | Required on Windows to build JARs | mvn -version | https://maven.apache.org/download.cgi |
| Node.js 22 LTS | Required for the existing Angular 18 frontend | node -v; npm -v | https://nodejs.org/en/download |
| Git for Windows | Required to put the project in GitHub | git --version | https://git-scm.com/downloads/win |
| Docker Desktop | Already installed; running Linux containers | docker version; docker compose version | https://docs.docker.com/desktop/setup/install/windows-install/ |
| IntelliJ IDEA / VS Code / Spring Tools | Optional editor | Open root pom.xml as Maven project | Choose your existing editor |
| Postman | Optional visual API demonstration | Import included collection | https://www.postman.com/downloads/ |
| MySQL Workbench | Optional database viewer | Connect to ports 3307, 3308, 3309 | https://dev.mysql.com/downloads/workbench/ |

No local MySQL Server, Axon installation, global Angular CLI, Kubernetes or extra cloud account is needed. Docker runs the databases, Axon, Zipkin and Spring apps. Angular CLI is installed locally by npm ci.

If Java 17 is already in `C:\Program Files\Java\jdk-17`, verify `javac -version` and that `mvn -version` uses the intended JDK. Install the JDK, not only a JRE. In Windows Environment Variables set JAVA_HOME to that JDK folder and add its `bin` and Maven's `bin` to Path. Close/reopen the terminal. Do not overwrite the full Path using setx.

## 2. Extract the project

Extract the ZIP once into a short local path, for example `E:\MicroService\events-microservices`. Keep the folder that contains pom.xml as the project root. Open PowerShell there.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/check-prerequisites.ps1
docker network inspect devops
docker ps --format "table {{.Names}}\t{{.Ports}}\t{{.Networks}}"
Copy-Item .env.example .env
```

Keep DOCKER_NETWORK=devops. The supplied new host ports avoid 8080/8081. If another application uses 8090 or 3307-3309, change the corresponding .env host port. If Gateway changes, update frontend/proxy.conf.json and Postman baseUrl. Docker Compose loads .env automatically; an IDE does not.

## 3. Build all services first

```powershell
mvn -B -ntp clean verify
```

This downloads dependencies, compiles seven Java modules, runs tests and packages six executable service JARs plus the DTO library. Internet is needed on the initial download. Build success does not require the MySQL containers. Tests use mocks and Axon aggregate fixtures.

Do not run individual service builds without their shared dependency: use root reactor or `mvn -pl event-service -am package`. To run a service through the IDE, first `mvn install -DskipTests` from the root if the IDE cannot resolve shared-contracts.

## 4. Start the stack

```powershell
docker compose up -d --build --wait --wait-timeout 300
docker compose ps
```

The Compose file starts only the new project services: three MySQL servers, Axon, Zipkin, Config Server, Eureka, Gateway and the three business services. Existing DevOps containers remain managed by your existing setup. Startup dependency ordering uses health checks; discovery can take extra time after a service becomes healthy.

If a service fails:

```powershell
docker compose logs --tail=100 config-server eureka-server
docker compose logs --tail=100 participant-service logistics-service event-service api-gateway
```

Check http://localhost:8888/event-service/default for central configuration, http://localhost:8761 for registration, http://localhost:8024 for Axon, and the Swagger URLs in README.

## 5. Run the business scenario

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke.ps1
```

It creates a participant, sends an Axon logistics command, polls the MySQL projection, creates an event referring to both, then verifies cost 50 and no degraded dependencies. It creates additional demo rows each run. IDs are saved in reports/demo-ids.json (ignored by Git).

## 6. Start Angular

In another terminal, from project root:

```powershell
cd frontend
npm ci
npm start
```

Open http://localhost:4200. It uses the Gateway at 8090 through the Angular development proxy. Create/modify events, participants and resources. CQRS projection can appear shortly after a logistics command; the UI reports acceptance and offers refresh. To delete a reserved resource, edit it to release its reservation first.

After Java changes: run `mvn verify`, then `docker compose up -d --build`. After frontend changes the development server rebuilds automatically. After config-repo changes restart the relevant application containers with `docker compose restart event-service` (or the relevant name).

## 7. Connect existing monitoring and CI

Follow DEVOPS.md. Keep SonarQube and Nexus stopped when not using them if RAM is tight. Existing Prometheus and Grafana can run during the demo. Starting all tools plus three MySQL servers and six JVMs can exceed a small laptop's RAM. Check Docker Desktop resource limits and container statistics:

```powershell
docker stats --no-stream
```

The memory limits in Compose apply to the new containers; they do not alter existing tool containers. If needed, run business JVMs from your IDE and only databases/Axon/discovery infrastructure in Docker (next section).

## Optional: debug business services in your IDE

Keep ports in `.env` at their defaults for these steps. Start infrastructure only:

```powershell
docker compose up -d --build config-server eureka-server mysql-participant mysql-logistics mysql-event axonserver zipkin
```

Open the root Maven project and run ParticipantServiceApplication, LogisticsServiceApplication, EventServiceApplication and ApiGatewayApplication. Default local ports are 8091, 8092, 8093, 8090. Default DB ports/credentials match .env.example. Set changed DB credentials explicitly in each IDE run configuration. Start IDE apps from the repository root; the config-server application, if run in IDE too, expects CONFIG_REPO=file:./config-repo.

Do not start container and IDE copies of the same service at the same time on the same host port. Existing Prometheus Docker service targets will not address IDE processes. For IDE mode change targets to host.docker.internal:8091, :8092, :8093, :8090 and use the original Docker targets for config/Eureka.

## Stop without deleting data

```powershell
docker compose --profile scale down
```

This stops only this project's containers and preserves its volumes and the external devops network. Existing Jenkins/Sonar/Nexus/Prometheus/Grafana remain running. `docker compose down -v` deletes this project's MySQL and Axon data: use only if you deliberately want a full reset. Never delete only MySQL projection data while keeping its persisted processor tokens; a proper projection replay/reset is needed for reconstruction.
