param([switch]$Demo,[switch]$Scale)
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
if (!(Test-Path .env)) { Copy-Item .env.example .env }
docker network inspect devops *> $null
if ($LASTEXITCODE -ne 0) { throw 'The existing devops network was not found. Start Docker Desktop and check docker network ls.' }
mvn -B -ntp clean verify
if ($LASTEXITCODE -ne 0) { throw 'Maven verification failed.' }
$composeArgs=@('compose','-f','compose.yml')
if ($Demo) { $composeArgs+=@('-f','compose.demo.yml') }
if ($Scale) { $composeArgs+=@('--profile','scale') }
$composeArgs+=@('up','-d','--build','--wait','--wait-timeout','300')
& docker @composeArgs
if ($LASTEXITCODE -ne 0) { throw 'Startup failed. Run docker compose ps and docker compose logs --tail=100.' }
Write-Host 'Gateway: http://localhost:8090 ; start Angular in another terminal: cd frontend; npm ci; npm start'
