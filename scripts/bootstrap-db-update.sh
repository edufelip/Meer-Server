#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# One-time schema bootstrap without dropping data. Creates missing tables/columns, skips seeders.
SPRING_PROFILES_ACTIVE=local-db \
SPRING_JPA_HIBERNATE_DDL_AUTO=update \
MEER_SEED_STORES=${MEER_SEED_STORES:-false} \
MEER_SEED_CATEGORIES=${MEER_SEED_CATEGORIES:-false} \
./springboot/run-local.sh
