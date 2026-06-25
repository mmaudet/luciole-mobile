#!/data/data/com.termux/files/usr/bin/bash
# server/run-server.sh — launch Luciole-1B with grammar-constrained output (run in Termux)
set -euo pipefail
MODEL="${MODEL:-$HOME/models/Luciole-1B-SFT-Q4_K_M.gguf}"
GRAMMAR="${GRAMMAR:-$HOME/luciole-mobile/contract/actions.gbnf}"
llama-server -m "$MODEL" \
  --host 0.0.0.0 --port 8080 \
  --grammar-file "$GRAMMAR" \
  --parallel 4 --ctx-size 2048 \
  --path "$HOME/luciole-mobile/web"   # serve the participant client at /
