#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

if [[ "${PREFIX:-}" != *com.termux* && ! -d /data/data/com.termux/files/usr ]]; then
  echo "Warning: this installer is intended for Termux on Android." >&2
fi

pkg update
pkg install -y git python clang make cmake ninja openssl libusb
python -m pip install --upgrade esptool mpremote
printf '\nInstalled tool versions:\n'
python -m esptool version || true
mpremote --version || true
