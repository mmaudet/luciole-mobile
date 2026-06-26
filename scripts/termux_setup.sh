#!/data/data/com.termux/files/usr/bin/bash
# scripts/termux_setup.sh — one-shot on-device install for the Luciole workshop (run IN Termux).
#
# Prereq: run `termux-setup-storage` (tap Allow) first. Reads from /sdcard/Download:
#   - luciole-repo.tar.gz          (built on the laptop with: git archive --format=tar.gz HEAD)
#   - Luciole-1B-SFT-Q4_K_M.gguf   (the model)
#
# Installs llama.cpp + python + termux-am (the dispatcher is STDLIB-ONLY — no pip deps),
# lays out ~/luciole-mobile and ~/models. Logs to /sdcard/Download/luciole-setup.log.
set -uo pipefail
SD=/sdcard/Download
exec > >(tee "$SD/luciole-setup.log") 2>&1
echo "===== LUCIOLE SETUP START ====="; date 2>/dev/null || true
[ -r "$SD/luciole-repo.tar.gz" ] || { echo "ERREUR: lance d'abord 'termux-setup-storage' (Autoriser)."; exit 2; }

echo "## pkg install llama-cpp python termux-am"
pkg install -y llama-cpp python termux-am

echo "## repo -> ~/luciole-mobile"
mkdir -p ~/luciole-mobile ~/models
tar -xzf "$SD/luciole-repo.tar.gz" -C ~/luciole-mobile
chmod +x ~/luciole-mobile/server/run-server.sh 2>/dev/null || true

echo "## modele -> ~/models"
DST="$HOME/models/Luciole-1B-SFT-Q4_K_M.gguf"
if [ -f "$DST" ]; then echo "deja present ($(du -h "$DST" 2>/dev/null | cut -f1))"
else cp "$SD/Luciole-1B-SFT-Q4_K_M.gguf" "$DST"; echo "copie ($(du -h "$DST" 2>/dev/null | cut -f1))"; fi

echo "## sanity"
command -v llama-server python am || echo "ATTENTION: binaire manquant"
python --version
echo "(option) validation schema : pip install jsonschema  puis  DISPATCH_VALIDATE=1 ..."
echo "===== LUCIOLE SETUP DONE ====="
