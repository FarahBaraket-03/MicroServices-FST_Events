param([string]$BaseUrl='http://localhost:8090')
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
docker compose --profile scale up -d --wait participant-service-2
if ($LASTEXITCODE -ne 0) {throw 'Second instance failed to start.'}
Write-Host 'Waiting for Eureka discovery propagation (up to 90 seconds)...'
$seen=@{}
for ($i=0;$i -lt 45;$i++) {
 try { $r=Invoke-RestMethod "$BaseUrl/api/demo/participant-instance"; $seen[$r.instance]=$true; Write-Host $r.instance; if ($seen.Count -ge 2) { break } } catch { Write-Host 'Discovery pending...' }
 Start-Sleep -Seconds 2
}
if ($seen.Count -lt 2) {throw 'Only one instance observed. Inspect Eureka registrations and Gateway logs.'}
Write-Host 'Both instances observed. Sequential requests:'
1..8 | ForEach-Object { (Invoke-RestMethod "$BaseUrl/api/demo/participant-instance").instance; Start-Sleep -Milliseconds 250 }
