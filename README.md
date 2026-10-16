# Termux ESP32 Edition

A mobile-first toolkit for working with ESP32 boards from Termux on Android. This repository currently provides diagnostic and workflow scripts; it does not yet contain a modified Termux APK.

## Goals
- Diagnose Termux/Android USB-host and serial-port visibility.
- Install common ESP32 Python tools (esptool, mpremote).
- Provide a safer command wrapper for diagnostics and flashing.
- Run shell and Python tests in GitHub Actions.
- Track future integration work for the Termux Android app.

## Quick start
    bash scripts/install-termux.sh
    bash scripts/esp32.sh doctor
    bash scripts/esp32.sh ports

## USB limitations on Android
A USB device connected through OTG may be visible to Android but unavailable as a serial device inside Termux. Android USB host permissions and a USB-serial driver/userspace implementation may be required. This toolkit reports what it can observe; it cannot bypass Android permissions or kernel/device restrictions.

## Flashing
Flashing erases or overwrites firmware. Check the board, port and firmware file before running:
    bash scripts/esp32.sh flash-mpy /path/to/firmware.bin /dev/ttyUSB0
Only run flashing when the port is correct and you are prepared to replace the existing firmware.

## Tests
    bash -n scripts/*.sh
    python -m unittest discover -s tests -v

## Status
Starter implementation. CI tests the scripts on GitHub-hosted Linux; real ESP32 USB/OTG tests must be performed on physical Android hardware.
