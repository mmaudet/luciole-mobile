#!/data/data/com.termux/files/usr/bin/bash
# scripts/setup_ssh.sh — (DEV ONLY) start Termux's SSH server so the laptop can drive the phone
# over USB. Not needed for the demo itself (the Pixel runs standalone).
#
# Prereq: push your Mac public key to /sdcard/Download/mac.pub, and run `termux-setup-storage`.
# Then, from the Mac (works even in airplane mode — it tunnels over USB):
#   adb forward tcp:8022 tcp:8022
#   ssh -p 8022 -i <your_key> -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null $(whoami)@127.0.0.1
set -uo pipefail
pkg install -y openssh
mkdir -p ~/.ssh && chmod 700 ~/.ssh
PUB=/sdcard/Download/mac.pub
if [ -r "$PUB" ]; then
  grep -qxF "$(cat "$PUB")" ~/.ssh/authorized_keys 2>/dev/null || cat "$PUB" >> ~/.ssh/authorized_keys
  chmod 600 ~/.ssh/authorized_keys
else
  echo "NB: $PUB introuvable — ajoute ta clé publique Mac à ~/.ssh/authorized_keys, ou utilise 'passwd'."
fi
command -v pkill >/dev/null 2>&1 && pkill sshd 2>/dev/null || true
sleep 1
sshd
echo "sshd up on :8022 — user=$(whoami)"
