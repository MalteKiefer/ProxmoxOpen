# Security Policy

## Reporting a vulnerability

If you believe you have found a security vulnerability in ProxMoxOpen, please report it **privately** before disclosing it publicly.

- Open a private security advisory on the source repository (GitHub "Security" tab → "Report a vulnerability"). All reports are triaged there.

Please include:

- A clear description of the issue and its impact
- Step-by-step reproduction instructions
- The app version (git SHA or release tag) and Android version you tested against
- Any proof-of-concept code or screenshots

## What to expect

- Acknowledgement within 72 hours
- An initial assessment within one week
- Coordinated disclosure after a fix is available

## Scope

In scope: the ProxMoxOpen Android app, its Gradle build configuration, and its release artifacts.

Out of scope: vulnerabilities in Proxmox VE itself (please report those upstream at https://bugzilla.proxmox.com), third-party libraries (please report upstream), or issues that require a physically compromised device.

## Hardening summary

- Certificate pinning via Trust-On-First-Use with a hard block on fingerprint change (constant-time comparison)
- Secrets stored only in the hardware-backed Android Keystore (AES-256-GCM, key bound to user authentication with 300s validity)
- SQLCipher database with a Keystore-wrapped key, also user-auth-bound
- App lock fails closed when biometric / device credential is unavailable; re-locks on background
- No analytics, no crash reporters, no trackers
- `android:allowBackup="false"`, `data_extraction_rules.xml` excludes both cloud backup and device-to-device transfer
- `FLAG_SECURE` enabled by default, additionally pinned per sensitive screen (login, server config, console, config export/import)
- Console loopback proxy authenticated via per-session 256-bit secret (other apps on the device cannot ride the live PVE session)
- Console one-time tickets injected post-load via `window.__pxo`, never carried in WebView URL
- Cleartext HTTP disabled globally (loopback exception only for the WebSocket proxy)
- Config-backup envelopes use PBKDF2-HmacSHA256 at 600,000 iterations with versioned envelope (V2)
- HTTP retry policy excludes `/access/ticket` to avoid fail2ban lockouts
- Gradle dependency-verification metadata enforced; GitHub Actions SHA-pinned; Dependabot watches Actions ecosystem
