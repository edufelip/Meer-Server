#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Runs with seeds enabled (they only execute when tables are present and empty).
SPRING_PROFILES_ACTIVE=local-db \
MEER_SEED_STORES=true \
MEER_SEED_CATEGORIES=true \
./springboot/run-local.sh
