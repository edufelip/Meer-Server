#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Default to the prod env file; can be overridden by setting ENV_FILE before calling
export ENV_FILE="${ENV_FILE:-.env}"
./springboot/run-local.sh
