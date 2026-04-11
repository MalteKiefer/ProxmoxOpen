# ProxMoxOpen — Design Spec

**Date:** 2026-04-11
**Status:** Draft for review
**Author:** Brainstorming session

## 1. Vision & Goals

ProxMoxOpen is a native Android client for Proxmox VE servers with these non-negotiable goals:

- **Secure** — TOFU certificate pinning, hardware-backed secret storage, no data leaves the device
- **Stable** — Clean architecture, high test coverage, well-defined error handling
- **Transparent** — Open source (GPLv3), public development, reproducible builds
- **Open** — No proprietary dependencies, no trackers, no analytics
- **100% F-Droid** — All requirements for F-Droid main repo met from day one
- **Multilingual** — i18n from the start, community translations via Weblate
- **Multi-Server** — Manage multiple Proxmox instances from a single app
- **Full Proxmox API coverage** — built incrementally across phases
- **Material 3** — Modern UI including dynamic color, beautiful charts

## 2. Tech Stack

| Concern | Choice | Rationale |
|---|---|---|
| Language/UI | Kotlin + Jetpack Compose | Native, modern, F-Droid-friendly |
| Design System | Material 3 (incl. Dynamic Color) | Native Android look |
| minSdk / targetSdk | 31 / 34 | Material You, modern security APIs |
| Architecture | Clean Architecture (app / domain / data) | Testability, separation of concerns |
| State | MVI-light via `StateFlow` in ViewModels | Simple, reactive, Compose-friendly |
| DI | Hilt | Mature, F-Droid-compatible |
| HTTP | Ktor Client (OkHttp engine) | Pure Kotlin, full TLS control for TOFU pinning |
| Serialization | kotlinx.serialization | Kotlin-native, reflection-free |
| Local DB | Room | Server metadata (non-secret) |
| Secrets | Android Keystore (AES-256-GCM, hardware-backed) | No plain preferences |
| Charts | Vico | Compose-native, Apache-2.0, F-Droid-ok |
| Navigation | Jetpack Navigation Compose (type-safe routes) | Standard |
| Logging | Timber | No PII, tokens never logged |
| i18n | `strings.xml` + Weblate | MVP: de, en |
| Build | Gradle KTS + Version Catalog, reproducible | F-Droid requirement |
| License | GPLv3 | Strong copyleft, protects FOSS status |

## 3. Module Structure

```
:app              — Compose UI, Navigation, ViewModels
:domain           — Pure Kotlin: Models, UseCases, Repository interfaces
:data:api         — Ktor Proxmox client, TOFU TrustManager
:data:db          — Room entities, DAOs
:data:secrets     — Keystore wrapper, BiometricPrompt integration
:core:ui          — Theme, Material3 tokens, shared composables
:core:common      — Result types, DispatcherProvider, logging
```

## 4. High-Level Data Flow

```
Compose Screen → ViewModel → UseCase → Repository → ProxmoxApiClient (Ktor) → Proxmox REST API
                                                   ↘ Room (metadata)
                                                   ↘ Keystore (secrets)
```

## 5. Phased Roadmap

### Phase 0 — Foundation

- Gradle setup, module layout, Version Catalog
- CI pipeline (GitHub Actions): lint, unit tests, debug+release APK builds
- Reproducible-build configuration (pinned JDK, no dynamic versions)
- Material 3 theme (light/dark/system + dynamic color)
- Navigation skeleton, empty screens
- `strings.xml` (de, en), Weblate config
- License, SECURITY.md, CONTRIBUTING.md, CODE_OF_CONDUCT.md, README

### Phase 1 — MVP: Multi-Server Dashboard

**Auth & Connection**
- Add server (name, URL, port, realm selection: PAM / PVE / API token)
- Login via API token *or* username/password (+ optional TOTP)
- TOFU certificate pinning with fingerprint confirmation dialog
- Secure storage: metadata in Room, secrets in Android Keystore
- Multi-server switcher (drawer / bottom sheet)
- Optional app-lock via BiometricPrompt

**Cluster & Node View**
- Cluster status, node list with status badges
- Node detail: CPU, RAM, disk, uptime, load, KSM, network I/O
- Graphs for CPU / RAM / network / disk (Vico) with 1h / 24h / 7d / 30d windows via `rrddata`

**VM & Container List**
- Combined list of VMs (qemu) and CTs (lxc) across all nodes
- Filters (node, status, type, tags), search, sort
- Status badges, mini CPU/RAM indicators

**VM/CT Detail + Power Actions**
- Detail with live graphs and read-only config summary
- Actions: start, stop, shutdown, reboot, suspend / resume, reset
- Confirmation dialog for destructive actions
- Task polling via UPID for in-flight actions

**General**
- Task log view (current + recent server tasks)
- Pull-to-refresh and configurable auto-refresh
- Clear error messaging for network, auth, TLS failures

**Quality**
- Unit tests for domain/data, UI tests for critical flows
- F-Droid metadata (`fastlane/metadata/android`), first F-Droid release

### Phase 2 — Console & Snapshots

- noVNC / xterm.js console via WebView (VNC for VMs, terminal for CTs), ticket injection
- Native VNC client evaluation (deferred)
- Snapshots: list, create, rollback, delete
- Backup view (read + trigger manual backup)
- Extended task logs with live stream

### Phase 3 — Extended Auth & Lifecycle

- LDAP/AD realm support (UI integration)
- OIDC login via Custom Tabs / AppAuth-Android
- VM/CT create wizard, clone, delete, migrate
- Storage overview (usage, contents)

### Phase 4 — Admin Depth

- Firewall (cluster/node/VM rules, security groups)
- User / group / pool management, ACLs
- Network configuration (bridges, bonds, VLANs — read first, write later)
- Backup jobs (schedules)

### Phase 5 — Cluster & Infrastructure

- HA resources and groups
- Ceph overview (OSDs, pools, monitors)
- SDN (read), replication status
- Android widgets / Quick Settings tiles (e.g. cluster status)

## 6. Security Model

### TLS & Certificates (TOFU)

- First connection extracts the full cert chain and computes SHA-256 fingerprint of the server cert
- Confirmation dialog shows subject, issuer, validity, SHA-256 + SHA-1 fingerprints, explicit trust button
- Fingerprint stored per server in Room
- Custom `X509TrustManager` in Ktor/OkHttp accepts *only* the pinned fingerprint (no system truststore fallback for that server)
- Fingerprint change: hard block with warning dialog showing diff (old vs new), no "continue anyway"
- Optional user preference: additionally require system truststore validation

### Credential Storage

- Metadata (name, URL, port, realm, username, fingerprint, last sync) in **Room** (no secrets)
- Secrets (API token secret, optional saved password, optional TOTP seed) **only** in **Android Keystore** (hardware-backed AES-256-GCM)
- Optional: keys require user authentication → unlocked via BiometricPrompt
- Tokens/passwords never logged, never included in crash artifacts

### Session & Token Handling

- Proxmox ticket auth: ticket + CSRFPreventionToken held **in memory only**, never persisted
- Ticket lifetime (2h) monitored, auto re-auth with stored credentials
- API token auth preferred: `Authorization: PVEAPIToken=<user>@<realm>!<tokenid>=<secret>` — stateless
- TOTP re-prompted per login by default (seed storage opt-in, Phase 3)

### App Hardening

- `android:allowBackup="false"`, secrets excluded from backups
- Optional `FLAG_SECURE` on login + detail screens (blocks screenshots / recents thumbnails)
- Network Security Config: cleartext HTTP disabled
- No crash reporters (no Firebase / Sentry)
- No analytics, no trackers

## 7. F-Droid Compliance

- 100% FOSS dependencies (Apache-2.0 / MIT / GPL-compatible)
- No Google Play Services, no Firebase, no proprietary SDKs
- Reproducible builds: pinned toolchain, no dynamic versions, Version Catalog locked
- No anti-features: verified trackers-free
- `fastlane/metadata/android/` with descriptions, screenshots, changelogs (de, en)
- F-Droid build recipe (`metadata/<package>.yml`)
- Upstream-signed APKs where possible
- License: GPLv3

## 8. Quality & Testing

- **Unit tests:** domain (UseCases), data (API parsing, TOFU trust manager, Keystore wrapper) — target ≥70% coverage at MVP
- **Instrumented / UI tests:** Compose tests for critical flows (add server, login, power action, fingerprint warning)
- **API tests:** MockWebServer with real Proxmox response fixtures
- **Lint:** Android Lint + Detekt, zero-warning policy in CI
- **CI (GitHub Actions):** lint → unit test → UI test (emulator) → release APK build → reproducible-build check
- **Accessibility:** TalkBack support, sufficient contrast, min 48dp touch targets, TalkBack tests for main flows

## 9. Openness & Transparency

- Public repo (GitHub + Codeberg mirror recommended)
- `CHANGELOG.md` + SemVer
- `SECURITY.md` with contact and responsible disclosure
- `CONTRIBUTING.md` + `CODE_OF_CONDUCT.md`
- Translations via Weblate (hosted.weblate.org — free for FOSS)
- All releases signed + checksums published

## 10. Out of Scope (for now)

- iOS / desktop clients (Kotlin-only, Android-only)
- Console (Phase 2)
- OIDC / LDAP login (Phase 3)
- VM/CT creation, storage write operations (Phase 3+)
- Firewall, ACL management (Phase 4)
- HA, Ceph, SDN management (Phase 5)
- Crash reporting / analytics (ever)
