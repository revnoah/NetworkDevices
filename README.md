# Network Devices

Network Devices is an Android app for discovering devices on the current Wi-Fi network, inspecting common services, and opening detected web interfaces. It keeps a local cache of scan results and lets you assign custom device names.

## About

Use the app to inspect a home or lab Wi-Fi network from an Android device. The scanner derives a `/24` range from the phone's IPv4 address and probes addresses `.1` through `.254`, showing results as they arrive. It does not use the DHCP subnet mask, scan IPv6, or discover devices across routed networks.

## Features

- Discover hosts through ping responses or successful TCP connections, with scan progress and results sorted by IP address.
- Show IP addresses, available DNS and NetBIOS names, ping response, MAC address, and manufacturer where detectable.
- Probe 14 common TCP ports and display service labels.
- Inspect HTTP/HTTPS endpoints for response status, server header, page title, and redirect destination; open endpoints in an external browser.
- Restore cached results and preserve custom names for rediscovered IP addresses on the same subnet.

## Built with

The application uses Java activities, Android XML layouts, AppCompat, and Material Components. `NetworkScanner` performs network work in background executors; `DeviceCache` stores JSON in Android SharedPreferences. The repository includes Kotlin example tests and Gradle Kotlin DSL build files.

## Getting started

### Prerequisites

- Android Studio compatible with the project's declared Android Gradle Plugin **9.2.1**.
- Android SDK platform **36.1** for compilation. The app targets API **36** and supports Android **7.0 / API 24** or newer.
- The included Gradle **9.4.1** wrapper. Daemon JVM criteria request **Java 21**; application Java source and target compatibility are **11**.
- Internet access for the initial Gradle, toolchain, SDK, and dependency downloads from the configured repositories.
- An Android device connected to the Wi-Fi network you want to inspect. An emulator can help with UI development, but its virtual network may not expose the physical LAN.

These versions come from the checked-in build configuration; a successful build has not been verified as part of repository setup.

### Installation

1. Open this project directory in Android Studio and let Gradle sync. Install the requested SDK components if prompted.
2. Let Android Studio configure the SDK path in the local `local.properties` file. This machine-specific file is ignored.
3. Select the `app` module and an Android device, then use **Run** to install and launch **Network Devices**.

For command-line builds, run from the project root:

```sh
./gradlew :app:assembleDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. With a device connected and authorized for Android debugging, install it with:

```sh
./gradlew :app:installDebug
```

Launch Network Devices from the device's app launcher. On Windows, use `gradlew.bat` in place of `./gradlew`.

## Configuration

There are no API keys, accounts, environment variables, or backend services to configure. The scan range, ports, and timeouts are constants in [NetworkScanner.java](app/src/main/java/com/revnoah/networkdevices/NetworkScanner.java); there is no settings screen for changing them.

The scanner checks TCP ports `22`, `23`, `53`, `80`, `139`, `443`, `445`, `548`, `631`, `8000`, `8080`, `8443`, `9000`, and `9090`. Labels describe conventional port uses rather than verified service identities. Manufacturer lookup uses a small bundled MAC-prefix table without a remote lookup service.

Results and custom names live in the app-private `device_cache` SharedPreferences. Only one subnet's cache is retained: detecting a different `/24` clears the previous cache. Network identity is based on the subnet, so separate Wi-Fi networks using the same range share that identity. Custom names follow IP addresses, which can change owners after DHCP reassignment. Clearing Android app storage removes this data. Android backup is enabled, and the included backup rules do not explicitly exclude the cache.

## Usage

1. Connect the Android device to the Wi-Fi network you want to inspect and open Network Devices.
2. Tap **Scan Network**. The app shows the subnet, progress through 254 hosts, and discovered devices. Allow the scan to finish before opening a result: detail pages load from the saved cache.
3. Tap a device row to see its names, address, manufacturer, ping response, services, and web information.
4. Tap **Rename device**, enter a name, and choose **Save**. This changes only the local cached label. Saving an empty name restores the hostname or IP display.
5. Tap an **Open** URL button to hand a detected HTTP/HTTPS endpoint to a browser.

Scanning actively sends ping and TCP probes, DNS lookups, NetBIOS queries on UDP port 137, and web requests to detected endpoints. HTTP requests can use cleartext traffic; HTTPS failures fall back to an “open” label. Web redirects are reported without being followed by the scanner. Only scan networks you have permission to inspect.

A missing result does not establish that a device is offline: hosts may block probes, expose other ports, or be isolated by Wi-Fi configuration. MAC addresses depend on reading `/proc/net/arp` and may be unavailable under Android restrictions; manufacturer and name fields can therefore remain `Unknown`. Cached results describe an earlier scan rather than continuous monitoring.

## Development & testing

From the project root, the standard Android Gradle checks are:

```sh
./gradlew :app:testDebugUnitTest
./gradlew :app:connectedDebugAndroidTest
./gradlew :app:lintDebug
```

The connected test requires an attached device or running emulator. Existing tests only check arithmetic and the application package name; they do not cover scanning, persistence, naming, or web detection. No builds, lint, or tests were run during repository setup.

## License

Original project code is licensed under the [MIT License](LICENSE), copyright 2026 [Noah Stewart](https://noahjstewart.com). The Gradle wrapper and Android Open Source Project launcher resources retain their existing Apache-2.0 notices; see [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the bundled [Apache License 2.0](LICENSES/Apache-2.0.txt). Third-party dependencies retain their respective licenses.