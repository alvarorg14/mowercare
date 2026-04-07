#!/usr/bin/env bash
# Seed API data for Maestro E2E: bootstrap org (if needed), login, create one issue.
# See docs/testing-e2e.md for full instructions.
set -euo pipefail
exec python3 "$(dirname "$0")/seed.py"
