#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."

# Default to the dev env file; can be overridden by setting ENV_FILE before calling
export ENV_FILE="${ENV_FILE:-.env.dev}"
./springboot/run-local.sh
