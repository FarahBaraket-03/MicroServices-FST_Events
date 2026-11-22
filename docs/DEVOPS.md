# Reuse your existing DevOps containers

Known network: `devops`. Known containers and ports: jenkins:8080, sonarqube:9000, nexus:8081, prometheus:9090, grafana:3000. The new application host entry point is localhost:8090. No existing container is modified by the application Compose file.

## Prometheus

First locate your current configuration mount:

```powershell
docker inspect prometheus --format '{{json .Mounts}}'
```

Find the Source mounted to `/etc/prometheus/prometheus.yml` (or check the container's command for a custom `--config.file`). Open that existing host file. Back it up, then append the job from `ops/prometheus/events-scrape.yml` **inside the existing `scrape_configs` list**. Do not replace other jobs or paste a second scrape_configs key.

Restart the existing Prometheus container to reload its mounted file:

```powershell
docker restart prometheus
```

At http://localhost:9090/targets verify the new job is UP. In Docker the Gateway target is api-gateway:8080, despite its Windows host port being 8090. The optional participant-service-2 target should only be added when that instance is running; otherwise DOWN is expected.

Try these expressions:

```promql
up{job="events-microservices"}
rate(http_server_requests_seconds_count{application="event-service"}[1m])
resilience4j_circuitbreaker_state
resilience4j_retry_calls_total
```

## Grafana

At http://localhost:3000 reuse your existing Prometheus data source, or create one pointing to `http://prometheus:9090` (Docker URL). Import `ops/grafana/events-dashboard.json`, select that data source and run some requests/failure demos. The dashboard includes request rate, p95 duration, circuit state, retries, HTTP failures/429 and heap. Metrics may appear only after the associated code path is exercised.

## Zipkin

http://localhost:9411 -> run smoke.ps1 or request statistics -> select event-service -> Find Traces. Find a single trace containing the event-service request and Feign spans for participant/logistics. Tracing sampling is 100% for this local exercise. Zipkin's included container uses in-memory trace storage, so traces are lost when restarted.

## SonarQube manually

1. Open http://localhost:9000 and create the project key `events-microservices`.
2. Generate an analysis token. Keep it out of Git and screenshots.
3. Run this in PowerShell from the project root, with SONAR_TOKEN supplied in your terminal environment (do not put the token in this file):

```powershell
$env:SONAR_HOST_URL='http://localhost:9000'
mvn -B -ntp clean verify sonar:sonar -Dsonar.qualitygate.wait=true
```

JaCoCo reports are generated at each module's target/site/jacoco/jacoco.xml. This project has targeted tests, not a promise of a particular coverage percentage. Address actual findings rather than suppressing them to obtain a screenshot. The Maven scanner analyzes the Java reactor. The separate frontend build in CI checks Angular compilation; configure a separate Sonar frontend analysis if you need its own JS/TS report.

## Nexus manually

1. Open http://localhost:8081.
2. Verify/create Maven hosted repositories named `maven-snapshots` (Snapshot version policy) and `maven-releases` (Release policy). Standard Nexus installations usually already have them.
3. Create a deployment user with permission to read/add/edit artifacts in those repositories. Do not use a hard-coded admin password.
4. Provide NEXUS_USER and NEXUS_PASSWORD as terminal environment variables, and set:

```powershell
$env:NEXUS_URL='http://localhost:8081'
mvn -B -ntp -s ops/maven/settings.example.xml deploy
```

The project version is 1.0.0-SNAPSHOT, so artifacts go to maven-snapshots. The root parent POM, shared-contracts JAR and six service artifacts are published. Nexus Maven deployment publishes build artifacts; it is separate from Docker image publication. This exercise does not set up a Nexus Docker registry.

## Jenkins pipeline on your existing Linux Jenkins container

The included Jenkinsfile uses `sh`, so it targets your Linux container/agent. Windows host installations of Maven/JDK are not automatically available inside that container.

1. Manage Jenkins -> Plugins: ensure Pipeline, Git, Credentials Binding and JUnit are available. The pipeline calls Maven's scanner directly; it does not require a SonarQube Jenkins plugin or Docker socket access.
2. Manage Jenkins -> Tools: configure a JDK named **jdk17** and Maven named **maven3**. The JDK tool may point to a compatible installed JDK 17/21 in your Jenkins container; use `docker exec jenkins java -version` and locate JAVA_HOME. It must contain javac. If installing JDK automatically, configure an appropriate installer supported by your Jenkins plugins. Configure Maven automatic installation or a real existing Maven home in the container/agent.
3. Credentials: Secret text **sonar-token** containing the analysis token; Username/password **nexus-credentials** containing the deployment user. Keep these IDs exact or update Jenkinsfile consistently.
4. Create a Pipeline job -> Pipeline script from SCM -> Git -> your repository URL -> branch main -> Script Path Jenkinsfile. Supply read credentials if your GitHub repository is private.
5. First run with RUN_SONAR=true and PUBLISH_NEXUS=false. Fix actual build/quality gate errors. Then enable PUBLISH_NEXUS for a run that publishes the verified artifacts.
6. The Jenkins pipeline uses http://sonarqube:9000 and http://nexus:8081 through devops. A separate build agent must be able to resolve/reach these addresses too. If it isn't on devops, adjust URLs to reachable endpoints.

No automatic production deployment or GitHub webhook is configured. You can build manually with Build Now. GitHub Actions runs independently and cannot reach your private local Sonar/Nexus; the included Actions workflow therefore performs build/tests only.

## Configuration persistence

Changes to your existing Prometheus configuration, Grafana imports, Jenkins tools/credentials and Nexus users must be made in your current tool setup. We cannot infer its host mount paths or passwords from container names. This repository supplies the fragments and exact integration steps without replacing your existing configuration.
