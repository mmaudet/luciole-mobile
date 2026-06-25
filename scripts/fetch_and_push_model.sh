#!/usr/bin/env bash
# scripts/fetch_and_push_model.sh — download Q4_K_M on the laptop, push to the phone.
set -euo pipefail
REPO="OpenLLM-France/Luciole-1B-SFT-1.1-GGUF"
FILE="Luciole-1B-SFT-Q4_K_M.gguf"
DEST_DIR="${DEST_DIR:-./.models}"
mkdir -p "$DEST_DIR"
# Uses the cached HF token (~/.cache/huggingface/token).
hf download "$REPO" "$FILE" --local-dir "$DEST_DIR"
LOCAL="$DEST_DIR/$FILE"
ls -lh "$LOCAL"
# Termux app-private home isn't adb-writable; push to /sdcard then move inside Termux.
adb push "$LOCAL" "/sdcard/Download/$FILE"
echo "Pushed to /sdcard/Download/$FILE"
echo "In Termux, run:  mkdir -p ~/models && mv /sdcard/Download/$FILE ~/models/"
