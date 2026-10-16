#!/data/data/com.termux/files/usr/bin/bash
set -u

section() { printf '\n== %s ==\n' "$1"; }
section "System"
printf 'date: '; date
printf 'uname: '; uname -a
printf 'arch: '; uname -m
printf 'PREFIX: %s\n' "${PREFIX:-unknown}"
printf 'Android release: %s\n' "$(getprop ro.build.version.release 2>/dev/null || echo unknown)"
printf 'Android SDK: %s\n' "$(getprop ro.build.version.sdk 2>/dev/null || echo unknown)"

section "ESP32 tools"
for tool in python git clang cmake ninja esptool.py mpremote adb; do
  if command -v "$tool" >/dev/null 2>&1; then
    printf '%-12s %s\n' "$tool" "$(command -v "$tool")"
  else
    printf '%-12s %s\n' "$tool" "not found"
  fi
done
python -m esptool version 2>/dev/null || true
mpremote connect list 2>&1 || true

section "USB visibility"
if [[ -d /dev/bus/usb ]]; then
  find /dev/bus/usb -maxdepth 2 -type c -print 2>/dev/null | head -40
else
  echo "/dev/bus/usb is not visible inside this Termux environment."
fi
for p in /dev/ttyUSB* /dev/ttyACM* /dev/serial/by-id/*; do
  [[ -e "$p" ]] && ls -l "$p"
done
echo "Note: Android USB permissions and serial drivers can prevent visibility even when OTG is connected."
