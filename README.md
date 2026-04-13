<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="ProxMoxOpen" width="128" height="128">
</p>

<h1 align="center">ProxMoxOpen</h1>

<p align="center">
  <strong>Native Android Client for Proxmox VE</strong><br>
  Secure, open-source, F-Droid-first server management from your pocket
</p>

<p align="center">
  <a href="https://github.com/MalteKiefer/ProxmoxOpen/releases"><img src="https://img.shields.io/github/v/release/MalteKiefer/ProxmoxOpen?style=flat-square" alt="Release"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue?style=flat-square" alt="License: GPL-3.0"></a>
  <a href="https://github.com/MalteKiefer/ProxmoxOpen/actions"><img src="https://img.shields.io/github/actions/workflow/status/MalteKiefer/ProxmoxOpen/ci.yml?style=flat-square&label=CI" alt="CI"></a>
  <a href="https://github.com/MalteKiefer/ProxmoxOpen/issues"><img src="https://img.shields.io/github/issues/MalteKiefer/ProxmoxOpen?style=flat-square" alt="Issues"></a>
  <a href="https://github.com/MalteKiefer/ProxmoxOpen/stargazers"><img src="https://img.shields.io/github/stars/MalteKiefer/ProxmoxOpen?style=flat-square" alt="Stars"></a>
</p>

<p align="center">
  <a href="#"><img src="https://img.shields.io/badge/F--Droid-coming%20soon-lightgrey?style=flat-square&logo=f-droid" alt="F-Droid: coming soon"></a>
  <a href="https://kiefer-networks.de"><img src="https://img.shields.io/badge/by-Kiefer%20Networks-blue?style=flat-square" alt="Kiefer Networks"></a>
  <a href="https://de.liberapay.com/beli3ver"><img src="https://img.shields.io/badge/donate-Liberapay-F6C915?style=flat-square&logo=liberapay" alt="Donate via Liberapay"></a>
</p>

---

ProxMoxOpen is a native Android client for managing Proxmox VE clusters. It connects directly to your self-hosted Proxmox servers, keeps all credentials in the Android Keystore, ships zero trackers, and is built for F-Droid from day one.

## Features

| Feature | Description |
|---------|-------------|
| **Multi-Server Management** | Add and switch between multiple Proxmox VE servers |
| **Cluster Dashboard** | Node overview with CPU, RAM, Disk usage and live charts |
| **VM Management** | Start, stop, shutdown, reboot, suspend, reset, migrate, clone, delete |
| **Container Management** | Full LXC lifecycle with config editing (6 tabs) |
| **Node Shell** | xterm.js terminal with Proxmox framing protocol |
| **VNC Console** | noVNC graphical console for QEMU VMs |
| **Terminal Keyboard** | Special key bar (Esc, Tab, Ctrl, Alt, arrows, F1-F12, Ctrl+C/D/Z/L) |
| **Snapshots** | Create, rollback, delete snapshots for VMs and containers |
| **Backups** | Per-guest backup/restore and cluster-wide backup job management |
| **Backup Job Wizard** | 4-step stepper: Schedule, Guest selection, Storage/Node, Options |
| **Live Charts** | CPU, RAM, Network, Disk I/O / IO Delay with timeframe selection |
| **Task Log** | Real-time task monitoring with polling and status tracking |
| **Storage Overview** | Storage pool usage, content types, active/inactive status |
| **Authentication** | PAM, PVE, API Token with TOTP two-factor support |
| **Certificate Pinning** | Trust On First Use (TOFU) with SHA-256 fingerprint verification |
| **Encrypted Storage** | AES-256-GCM via Android Keystore for credentials |
| **Encrypted Database** | SQLCipher with AndroidKeyStore-wrapped encryption key |
| **Biometric Lock** | Fingerprint/Face unlock gate for the app |
| **13 Terminal Themes** | Dark, Light, Solarized, Dracula, Monokai, Nord, Gruvbox, One Dark, Tokyo Night, Catppuccin, Proxmox |
| **8 Font Sizes** | 10px to 28px for terminal |
| **5 Languages** | English, German, French, Spanish, Italian |
| **Material 3** | Proxmox brand colors (orange on black) with dynamic color support |
| **No Tracking** | No analytics, no telemetry, no ads, no in-app purchases |

## Security

| Layer | Implementation |
|-------|---------------|
| Credentials | AES-256-GCM, Android Keystore (hardware-backed) |
| Database | SQLCipher 4.5, key wrapped with AndroidKeyStore |
| TLS | TOFU certificate pinning with SHA-256 fingerprints |
| Network | Cleartext blocked globally (localhost exception for WebSocket proxy) |
| WebView | MIXED_CONTENT_NEVER_ALLOW, URL validation, onRelease destroy |
| Proxy | Path traversal blocked, header size limit (16KB), cookie injection prevention |
| Logging | Timber (stripped in release), no sensitive data logged |
| Manifest | allowBackup=false, no exported components except launcher |

## Architecture

- **Language:** Kotlin 2.1, Jetpack Compose
- **Architecture:** Clean Architecture (domain/data/app layers)
- **DI:** Hilt (Dagger)
- **Networking:** Ktor + OkHttp engine
- **Database:** Room + SQLCipher
- **State:** StateFlow + Compose collectAsStateWithLifecycle
- **Navigation:** Type-safe Compose Navigation with serializable routes
- **Design:** Material 3 with custom Proxmox theme

### Module Structure

```
ProxMoxOpen/
  app/          # UI layer (Screens, ViewModels, Navigation)
  domain/       # Business logic (Models, Repository interfaces, UseCases)
  data/
    api/        # Ktor HTTP client, Repository implementations, DTOs
    db/         # Room database + SQLCipher
    secrets/    # Android Keystore credential storage
  core/
    ui/         # Shared Compose components, Theme, Colors
    common/     # DispatcherProvider, utilities
```

## Building from Source

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17+
- Android SDK with `minSdk 31` (Android 12+)

### Clone

```bash
git clone https://github.com/MalteKiefer/ProxmoxOpen.git
cd ProxmoxOpen
```

### Build Debug APK

```bash
./gradlew :app:assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

### Build Release APK

Create `keystore.properties` in the project root:

```properties
storeFile=/path/to/keystore.jks
storePassword=yourpass
keyAlias=youralias
keyPassword=yourkeypass
```

```bash
./gradlew :app:assembleRelease
```

### Run Tests

```bash
./gradlew test
```

## Localization

Available in 5 languages:

English, German (Deutsch), French (Fran&ccedil;ais), Spanish (Espa&ntilde;ol), Italian (Italiano)

Translation files are in `app/src/main/res/values-*/strings.xml`.

## Console

ProxMoxOpen includes a local WebSocket proxy that bridges the Android WebView to Proxmox's WebSocket endpoints, handling self-signed TLS certificates transparently.

- **Terminal (Node/LXC):** xterm.js with Proxmox framing protocol, auth handshake, special key bar
- **VNC (QEMU):** noVNC with overlay canvas rendering, binary subprotocol

## Donate

If you find ProxMoxOpen useful, consider supporting development:

- [Donate via Liberapay](https://de.liberapay.com/beli3ver)
- [Donate via PayPal](https://paypal.me/maltekiefer)

## Security Contact

- Report vulnerabilities to: security@kiefer-networks.de
- General contact: info@kiefer-networks.de

## License

Copyright (C) 2024-2026 [Malte Kiefer](https://kiefer-networks.de)

This program is licensed under the [GNU General Public License v3.0](LICENSE).

Bundled third-party libraries:
- [noVNC](app/src/main/assets/novnc/) — [MPL-2.0](app/src/main/assets/novnc/LICENSE.txt)
- [xterm.js](app/src/main/assets/xterm/) — [MIT](app/src/main/assets/xterm/LICENSE)
- [pako](app/src/main/assets/novnc/vendor/pako/) — [MIT](app/src/main/assets/novnc/vendor/pako/LICENSE)
