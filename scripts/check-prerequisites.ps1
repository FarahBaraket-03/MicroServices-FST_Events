$ErrorActionPreference='Continue'
Write-Host 'Expected: Java 17+, Maven 3.9+, Node 22 LTS, Git, Docker Desktop with Linux containers.'
java -version
mvn -version
node --version
npm --version
git --version
docker version
docker network inspect devops --format '{{.Name}}'
docker ps --format 'table {{.Names}}\t{{.Ports}}\t{{.Networks}}'
