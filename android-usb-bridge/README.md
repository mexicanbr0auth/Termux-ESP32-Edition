# Termux ESP32 USB Bridge (experimental)

This companion Android app uses Android USB Host permissions and usb-serial-for-android drivers to bridge a supported USB serial adapter to a TCP socket bound only to 127.0.0.1.

Build: the GitHub Actions workflow produces a debug APK artifact named termux-esp32-usb-bridge-debug.

Use:
1. Connect the ESP32-C3 using a data-capable OTG adapter.
2. Install/open the bridge APK and approve Android's USB permission dialog.
3. Keep the app open while using Termux.
4. Test the local stream from Termux with Python socket.create_connection(("127.0.0.1", 3333)).
5. For flashing, manually enter download mode: hold BOOT, tap RESET, release BOOT. The esptool socket URL can then be tried with --port socket://127.0.0.1:3333 and --before no-reset --after no-reset.

The stream is fixed at 115200 baud, 8N1. DTR/RTS control is not yet forwarded, so automatic reset/bootloader entry is not implemented. The USB device will not appear as /dev/ttyUSB0; Android's companion app owns the USB connection.

Experimental starter requiring real-device validation. It does not claim support for every ESP32 board or every USB-UART chipset. The bridge listens on localhost only, not Wi-Fi.
