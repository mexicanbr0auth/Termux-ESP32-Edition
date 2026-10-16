# Termux ESP32 Edition

A mobile-first toolkit for working with ESP32 boards from Termux on Android. This repository includes command-line diagnostics and an experimental Android USB-serial bridge. It is not a modified Termux APK.

## Components
- Termux scripts for diagnostics and MicroPython firmware flashing.
- Android companion app using Android USB Host permissions and USB serial drivers.
- GitHub Actions for shell/Python tests and Android APK builds.

## Quick start
    bash scripts/install-termux.sh
    bash scripts/esp32.sh doctor
    bash scripts/esp32.sh ports

## USB bridge on Android
Build the companion app with the GitHub Actions workflow named Build Android USB bridge. Download the termux-esp32-usb-bridge-debug artifact from the successful workflow run, install it on the phone, connect the ESP32 via a data-capable OTG adapter, approve USB access, and keep the app open.

In Termux, test the bridge:
    bash scripts/esp32.sh bridge-test

The bridge listens only on 127.0.0.1:3333. It uses Android's USB Host API to access USB serial devices that are not exposed as /dev/ttyUSB0 inside Termux.

## Flashing MicroPython
The Android bridge currently uses fixed 115200 baud and does not forward DTR/RTS. Manually enter ESP32-C3 download mode first (hold BOOT, tap RESET, release BOOT), then:
    bash scripts/esp32.sh flash-mpy /path/to/ESP32_GENERIC_C3.bin socket://127.0.0.1:3333

Flashing erases existing firmware. Type FLASH only after checking the firmware file and board. A successful build is not proof of physical USB compatibility; test with the actual phone, OTG adapter and board.

## Limits and next steps
The companion bridge is experimental and currently selects the first supported USB-serial device. It needs physical-device validation and additional work for serial baud selection, DTR/RTS reset control, multi-device selection, long-running foreground service behavior, and support for all ESP32 variants. Some boards may expose native USB CDC, while others use CP210x, CH340, FTDI or other USB-UART bridges.

## Tests
    bash -n scripts/install-termux.sh scripts/esp32-doctor.sh scripts/esp32.sh
    python -m unittest discover -s tests -v
