#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
[ -f .env ] || cp .env.example .env
docker network inspect devops >/dev/null
mvn -B -ntp clean verify
docker compose up -d --build --wait --wait-timeout 300
