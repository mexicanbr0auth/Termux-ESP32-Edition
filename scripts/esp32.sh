#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
usage() {
  cat <<'EOF'
Usage:
  esp32.sh doctor
  esp32.sh ports
  esp32.sh flash-mpy FIRMWARE.bin SERIAL_PORT

Commands:
  doctor       Diagnose system, tools and USB visibility
  ports        List serial ports detected by mpremote
  flash-mpy    Erase flash and write a firmware image (destructive)
EOF
}
cmd="${1:-help}"
case "$cmd" in
  doctor) exec bash "$ROOT/scripts/esp32-doctor.sh" ;;
  ports) exec mpremote connect list ;;
  flash-mpy)
    [[ $# -eq 3 ]] || { usage >&2; exit 2; }
    firmware="$2"
    port="$3"
    [[ -f "$firmware" ]] || { echo "Firmware not found: $firmware" >&2; exit 2; }
    [[ -n "$port" ]] || { echo "Serial port must not be empty" >&2; exit 2; }
    echo "WARNING: this erases the board and writes firmware: $firmware -> $port"
    read -r -p 'Type FLASH to confirm: ' confirm
    [[ "$confirm" == FLASH ]] || { echo "Cancelled."; exit 1; }
    python -m esptool --port "$port" erase-flash
    exec python -m esptool --port "$port" write-flash 0x0 "$firmware"
    ;;
  help|-h|--help) usage ;;
  *) echo "Unknown command: $cmd" >&2; usage >&2; exit 2 ;;
esac
