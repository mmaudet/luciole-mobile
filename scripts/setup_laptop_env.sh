#!/usr/bin/env bash
# scripts/setup_laptop_env.sh — laptop-side tooling for tests & staging
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
python3 -m venv .venv
./.venv/bin/pip install --quiet --upgrade pip
./.venv/bin/pip install --quiet jsonschema "qrcode[pil]" pytest
echo "python deps OK"
node --version
node -e "require('node:test'); console.log('node --test OK')"
adb --version >/dev/null && echo "adb OK"
echo "ENV READY"
