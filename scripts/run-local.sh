#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Run the API against the existing local-db schema without touching DDL or seed data.
SPRING_PROFILES_ACTIVE=local-db \
MEER_SEED_STORES=${MEER_SEED_STORES:-false} \
MEER_SEED_CATEGORIES=${MEER_SEED_CATEGORIES:-false} \
./springboot/run-local.sh
