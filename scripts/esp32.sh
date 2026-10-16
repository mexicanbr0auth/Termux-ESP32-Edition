#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
usage() {
  cat <<'EOF'
Usage:
  esp32.sh doctor
  esp32.sh ports
  esp32.sh bridge-test
  esp32.sh flash-mpy FIRMWARE.bin SERIAL_PORT

Commands:
  doctor       Diagnose system, tools and USB visibility
  ports        List serial ports detected by mpremote
  bridge-test  Test the Android USB bridge at 127.0.0.1:3333
  flash-mpy    Erase flash and write a firmware image (destructive)
EOF
}
cmd="${1:-help}"
case "$cmd" in
  doctor) exec bash "$ROOT/scripts/esp32-doctor.sh" ;;
  ports) exec mpremote connect list ;;
  bridge-test)
    python -c 'import socket,sys
s=socket.socket()
s.settimeout(3)
try:
    s.connect(("127.0.0.1",3333))
    print("Bridge reachable at 127.0.0.1:3333")
except OSError as e:
    print("Bridge not reachable. Open the Termux ESP32 USB Bridge app, connect the board, and retry: " + str(e), file=sys.stderr)
    sys.exit(1)
finally:
    s.close()'
    ;;
  flash-mpy)
    [[ $# -eq 3 ]] || { usage >&2; exit 2; }
    firmware="$2"
    port="$3"
    [[ -f "$firmware" ]] || { echo "Firmware not found: $firmware" >&2; exit 2; }
    [[ -n "$port" ]] || { echo "Serial port must not be empty" >&2; exit 2; }
    echo "WARNING: this erases the board and writes firmware: $firmware -> $port"
    if [[ "$port" == socket://* ]]; then
      echo "TCP bridge detected: fixed 115200 baud. Put the ESP32 into download mode manually first."
      common=(--port "$port" --baud 115200 --before no-reset --after no-reset)
    else
      common=(--port "$port")
    fi
    read -r -p 'Type FLASH to confirm: ' confirm
    [[ "$confirm" == FLASH ]] || { echo "Cancelled."; exit 1; }
    python -m esptool "${common[@]}" erase-flash
    exec python -m esptool "${common[@]}" write-flash 0x0 "$firmware"
    ;;
  help|-h|--help) usage ;;
  *) echo "Unknown command: $cmd" >&2; usage >&2; exit 2 ;;
esac
