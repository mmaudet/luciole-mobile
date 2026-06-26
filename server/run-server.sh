#!/data/data/com.termux/files/usr/bin/bash
# server/run-server.sh — launch Luciole-1B with grammar-constrained output (run in Termux).
#
# Tuned on the Pixel 10 Pro Fold (Tensor G5, 2+5+1 cores): pin work to the 6 fast cores
# (cpu2-7, mask 0xFC) and mlock the model in RAM. On-device this took generation from
# ~1.5 to ~14 tok/s (the kernel was otherwise scheduling threads onto the 2 little cores).
#
# Latency note: the system prompt is ~730 tokens. The KV cache makes the SECOND and later
# requests fast (~2.4 s); the first request after start is ~14 s — pre-warm before a demo
# (send one throwaway request). The prompt is intentionally time-invariant (no {now}) so the
# cached prefix stays valid across minutes.
#
# Concurrency note: each --parallel slot gets ctx-size/parallel tokens. With a ~730-token
# prompt you need >=~1100 tokens/slot, so keep CTX/PARALLEL >= ~1100. Default is single-slot
# (validated). For the participant hands-on raise PARALLEL and CTX together, e.g.
#   PARALLEL=2 CTX=4096 bash server/run-server.sh   # 2 concurrent, 2048 tokens/slot
set -euo pipefail
MODEL="${MODEL:-$HOME/models/Luciole-1B-SFT-Q4_K_M.gguf}"
GRAMMAR="${GRAMMAR:-$HOME/luciole-mobile/contract/actions.gbnf}"
THREADS="${THREADS:-6}"        # number of inference threads (6 fast cores)
CPU_MASK="${CPU_MASK:-FC}"     # 0xFC = cpu2..7; skips the 2 little cores (cpu0-1)
PARALLEL="${PARALLEL:-1}"      # concurrent request slots (raise with CTX for the hands-on)
CTX="${CTX:-2048}"            # total context (split across PARALLEL slots)
exec llama-server -m "$MODEL" \
  --host 0.0.0.0 --port 8080 \
  --grammar-file "$GRAMMAR" \
  --threads "$THREADS" --threads-batch "$THREADS" --cpu-mask "$CPU_MASK" --mlock \
  --parallel "$PARALLEL" --ctx-size "$CTX" \
  --path "$HOME/luciole-mobile/web"   # sert le client participant + system_prompt.txt à /
