#!/usr/bin/env bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
if [ -f "$SCRIPT_DIR/.env" ]; then
  export $(grep -v '^#' "$SCRIPT_DIR/.env" | xargs)
fi
export SPRING_PROFILES_ACTIVE=${SPRING_PROFILES_ACTIVE:-default}
(cd "$SCRIPT_DIR" && ./gradlew bootRun)
