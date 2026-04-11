# ProxMoxOpen

Open, secure, F-Droid-first Android client for Proxmox VE.

ProxMoxOpen is a native Kotlin + Jetpack Compose app for managing one or many Proxmox VE servers from your phone. It pins certificates on first use, stores all secrets in the hardware-backed Android Keystore, ships zero trackers, and is built to meet every requirement for F-Droid's main repository.

## Status

Active development — see [`docs/superpowers/specs/2026-04-11-proxmoxopen-design.md`](docs/superpowers/specs/2026-04-11-proxmoxopen-design.md) for the full design and [`docs/superpowers/plans/2026-04-11-proxmoxopen-mvp.md`](docs/superpowers/plans/2026-04-11-proxmoxopen-mvp.md) for the current MVP roadmap.

## Features (MVP target)

- Multi-server management with fast server switching
- Cluster, node, VM and container dashboards with live RRD charts
- Power actions: start, stop, shutdown, reboot, suspend, resume, reset
- API-token, PAM and PVE authentication with TOTP two-factor
- Trust-On-First-Use TLS pinning, hardware-backed secret storage
- Material 3 with dynamic color on Android 12+
- German and English at launch, community translations via Weblate

## Building

Requirements: JDK 17 or newer, Android SDK with platforms 31 – 34, Android Studio Koala or newer (optional).

```sh
./gradlew :app:assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Contributing

We welcome contributions. Please read [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md) before opening a pull request. Translations live on Weblate — see [`docs/TRANSLATING.md`](docs/TRANSLATING.md) when available.

## Security

See [`SECURITY.md`](SECURITY.md) for the responsible disclosure process.

## License

GPL-3.0-or-later. See [`LICENSE`](LICENSE).
