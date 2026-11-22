Set-Location (Split-Path $PSScriptRoot -Parent)
# Keep the MySQL and Axon volumes. Existing devops tool containers are untouched.
docker compose --profile scale down
