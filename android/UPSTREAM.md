# Android app integration roadmap

The repository's scripts are a toolkit layer, not a fork or modified APK of Termux.

Potential upstream/app work, to be developed against a pinned Termux source revision:
1. Add a diagnostics screen for Android USB-host device discovery and permission status.
2. Integrate a maintained USB-serial library where compatible with supported Android versions.
3. Expose serial connection state and clear permission/error messages to the terminal session.
4. Add tests for permission denial, disconnect/reconnect and missing driver cases.
5. Validate on real devices and multiple USB-UART chipsets before claiming compatibility.

The upstream build workflow is a reference build of the upstream app. It does not inject these toolkit scripts into the APK and does not prove ESP32 USB serial compatibility.
