# Security Policy

## Reporting a vulnerability

If you believe you have found a security vulnerability in ProxMoxOpen, please report it **privately** before disclosing it publicly.

- Open a private security advisory on the source repository (GitHub "Security" tab → "Report a vulnerability"), or
- Email the maintainers at `info@kiefer-networks.de` (placeholder — will be updated when the infrastructure is in place).

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

- Certificate pinning via Trust-On-First-Use with a hard block on fingerprint change
- Secrets stored only in the hardware-backed Android Keystore (AES-256-GCM)
- No analytics, no crash reporters, no trackers
- `android:allowBackup="false"`, optional `FLAG_SECURE`
- Cleartext HTTP disabled by default
