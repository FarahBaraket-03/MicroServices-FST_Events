param([string]$BaseUrl='http://localhost:8090')
$ErrorActionPreference='Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
function Json-Post($Path,$Body) { Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/$Path" -ContentType 'application/json' -Body ($Body | ConvertTo-Json -Depth 8) }
# Retry gateway discovery until Eureka's registration has propagated.
$ready=$false
for ($i=0;$i -lt 60;$i++) { try { $null=Invoke-RestMethod "$BaseUrl/api/participants"; $ready=$true; break } catch { Start-Sleep -Seconds 2 } }
if (!$ready) { throw 'Gateway cannot reach participant-service. Check Eureka and docker compose logs.' }
$p=Json-Post 'participants' @{nom='Demo';prenom='Ahmed';tache='ORGANISATEUR'}
$l=Json-Post 'logistics/commands' @{description='DS demonstration chairs';reserve=$true;prixUnit=12.50;quantite=4}
$projected=$false
for ($i=0;$i -lt 30;$i++) { try { $row=Invoke-RestMethod "$BaseUrl/api/logistics/queries/$($l.idLog)"; if ($row.reserve) {$projected=$true;break} } catch {} ; Start-Sleep -Seconds 1 }
if (!$projected) { throw 'CQRS projection did not become readable. Check Axon, logistics MySQL and event processor logs.' }
$e=Json-Post 'events' @{description='DS microservices conference';dateDebut='2027-01-15';dateFin='2027-01-16';participantIds=@($p.idPart);logisticsIds=@($l.idLog)}
$s=Invoke-RestMethod "$BaseUrl/api/event-stats/$($e.idEvent)"
if ($s.degraded -or $s.nbParticipants -ne 1 -or $s.coutLogistiqueReservee -ne 50) {throw 'Scenario failed: expected one participant, reserved cost 50 and degraded=false.'}
New-Item -ItemType Directory -Force reports | Out-Null
@{participantId=$p.idPart;logisticsId=$l.idLog;eventId=$e.idEvent;baseUrl=$BaseUrl} | ConvertTo-Json | Set-Content reports/demo-ids.json
$s | ConvertTo-Json -Depth 8
Write-Host 'PASS: participant -> Axon command -> MySQL projection -> event -> Feign statistics. Demo IDs: reports/demo-ids.json'
