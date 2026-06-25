#!/usr/bin/env bash
# scripts/gen_grammar.sh — convert the JSON schema to a GBNF grammar
# Uses llama.cpp's standalone converter (no deps beyond python3).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CONV="$ROOT/scripts/json_schema_to_grammar.py"
if [ ! -f "$CONV" ]; then
  curl -fsSL -o "$CONV" \
    https://raw.githubusercontent.com/ggml-org/llama.cpp/master/examples/json_schema_to_grammar.py
fi
python3 "$CONV" "$ROOT/contract/actions.schema.json" > "$ROOT/contract/actions.gbnf"
echo "wrote contract/actions.gbnf ($(wc -l < "$ROOT/contract/actions.gbnf") lines)"
