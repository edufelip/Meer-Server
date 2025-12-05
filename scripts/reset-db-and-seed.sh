#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Destroys current schema and recreates with seed data. Use only when you want a clean slate.
SPRING_PROFILES_ACTIVE=local-db \
SPRING_JPA_HIBERNATE_DDL_AUTO=create \
MEER_SEED_STORES=${MEER_SEED_STORES:-true} \
MEER_SEED_CATEGORIES=${MEER_SEED_CATEGORIES:-true} \
./springboot/run-local.sh
