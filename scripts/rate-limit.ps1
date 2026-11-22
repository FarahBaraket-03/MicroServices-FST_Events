param([string]$BaseUrl='http://localhost:8090')
$ErrorActionPreference='Stop'
# Send concurrent requests to avoid network latency spreading them across windows.
$jobs=1..15 | ForEach-Object { Start-Job -ArgumentList $BaseUrl -ScriptBlock {
 param($Url)
 try { (Invoke-WebRequest "$Url/api/event-stats/summary" -UseBasicParsing).StatusCode }
 catch { if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { 0 } }
} }
$codes=$jobs | Wait-Job | Receive-Job
$jobs | Remove-Job
$codes | Group-Object | Select-Object Name,Count
if ($codes -notcontains 429) {Write-Warning 'No 429 captured. PowerShell job startup may have spread requests out. Repeat with Postman/Newman concurrency or inspect limiter metrics.'}
