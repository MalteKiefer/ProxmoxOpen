# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.4.0] — 2026-04-21

### Added

- **APT updates per node** — list pending package updates from `/nodes/{n}/apt/update` and trigger a cluster-safe upgrade-all with confirmation; the result routes to the live Task Detail screen.
- **Global search** — new search screen filters the cluster resources list by name, VMID, node, tag, or type and jumps to the matching detail screen. 250ms debounce; AND-combined tokens.
- **Tags** — guest detail shows tag chips parsed from the PVE tags field; guest config has a free-text tag editor that persists via `setGuestConfig` / `setVmConfig`.
- **HA manager (read-only)** — three tabs backed by `/cluster/ha/status/current`, `/cluster/ha/resources`, and `/cluster/ha/groups` — quorum state, HA-managed guests, and failover groups.
- **Node network (read-only)** — list of bridges, bonds, physical NICs, and VLANs from `/nodes/{n}/network` shown on the node detail screen. Edits intentionally omitted: misconfiguring a bridge from a phone can take a node offline.
- **Node disks + SMART** — inventory from `/nodes/{n}/disks/list` with health, wearout and type chips; tap a disk to view the SMART report from `/nodes/{n}/disks/smart`.
- **Offline dashboard cache** — successful cluster-resource fetches are persisted to Room; on network failure the dashboard falls back to the last snapshot with an "offline — cached HH:mm" banner. New Settings toggle to opt out.
- **Encrypted config export / import** — export server records (without secrets) and user preferences to a passphrase-protected `.pmoconfig` file (AES-256-GCM with PBKDF2WithHmacSHA256, 200k iterations). Import offers merge or replace, rejects wrong files via magic header, and surfaces wrong passphrase explicitly.
- **AMOLED black theme** — Settings → Appearance toggle that forces pure-black backgrounds in dark mode for OLED displays. Translated into all five supported languages.

## [1.3.0] — 2026-04-21

### Security

- **TLS trust manager hardening** — `TofuTrustManager.checkClientTrusted` now throws `CertificateException` per the `X509TrustManager` contract instead of returning silently. The constructor rejects pins that are not exactly 64 hex characters with `IllegalArgumentException`, preventing malformed pins from being silently accepted.
- **Token-login auth verification** — API-token login no longer infers success from an exception-free `/nodes` call. The login flow now explicitly rejects empty node lists and issues a second, resource-scoped probe (`getNodeStatus`) that trips per-path ACLs, catching revoked or under-privileged tokens that `/nodes` alone would miss.
- **Session refresh** — added `refreshTicket` and `ensureValidSession` helpers on the auth repository. PAM/PVE sessions can now be re-issued before the 2-hour PVE ticket expires (10-minute skew); API-token sessions are passed through since tokens do not expire on a TTL.
- **Console proxy ticket validation** — `LocalWebSocketProxy` now rejects tickets or CSRF tokens that contain anything outside printable ASCII minus `;` `,` CR LF SP. The existing CRLF-only check was a strict subset and missed cookie-attribute smuggling. Logs never print the ticket contents — only its length and 4-character prefix.
- **Console log filter** — the WebView `onConsoleMessage` handler is now gated behind `BuildConfig.DEBUG`, truncates messages to 200 characters, and redacts any line mentioning `password` or `token` (case-insensitive) to `[redacted]`. Prevents accidental credential leaks into logcat on debug builds.
- **AES-GCM AAD binding (versioned)** — locally persisted encrypted blobs (SQLCipher DB-key wrap + each entry in `KeystoreSecretStore`) now bind the ciphertext to its storage context via `updateAAD`: the DB wrapping blob to the keystore alias, each secret to its logical key name. New blobs are prefixed `v2.`; legacy `v1` blobs still decrypt without AAD and are silently re-encrypted to `v2.` on first read — no data loss for existing users.

## [1.2.0] — 2026-04-21

### Added

- New security setting **Block screenshots** (off by default). When enabled, `FLAG_SECURE` is set on the main window to prevent screenshots and hide the app contents in the recent apps overview. Translated into all 5 supported languages.

### Security

- Hardened WebView configuration for the noVNC console: `allowFileAccess`, `allowContentAccess`, `allowFileAccessFromFileURLs`, and `allowUniversalAccessFromFileURLs` are now explicitly set to `false`.
- AndroidKeystore AES keys for the SQLCipher wrapping key and the secret store are now created with `setRandomizedEncryptionRequired(true)` and try to be `setIsStrongBoxBacked(true)` on devices advertising `FEATURE_STRONGBOX_KEYSTORE`, with a safe fallback to TEE-backed keys if StrongBox is unavailable.
- Removed the deprecated SHA-1 certificate fingerprint from the TOFU trust dialog; only the SHA-256 fingerprint is shown.

## [1.1.0] — 2026-04-20

### Fixed

- Startup crash on release builds caused by R8 stripping SQLCipher JNI classes. Native `JNI_OnLoad` aborted the runtime during Room initialisation (`register_android_database_SQLiteCompiledSql`).

### Changed

- Migrated from the deprecated `net.zetetic:android-database-sqlcipher` to the maintained `net.zetetic:sqlcipher-android:4.6.1`.
- Load native SQLCipher library explicitly via `System.loadLibrary("sqlcipher")` in `Application.onCreate()`.
- Added R8 keep rules for `net.zetetic.database.**` to prevent future JNI stripping regressions.

## [Unreleased]

### Added

- Initial project structure with Clean Architecture modules (`:app`, `:domain`, `:core:common`, `:core:ui`, `:data:api`, `:data:db`, `:data:secrets`)
- Material 3 theme with dynamic color support for Android 12+
- TOFU `X509TrustManager` that pins a server's SHA-256 fingerprint
- Hardware-backed `KeystoreSecretStore` for persisting API tokens and passwords
- Room `ProxmoxDatabase` with a `servers` entity and DAO
- Hilt wiring for database, secret store, HTTP client factory and dispatcher provider
- Type-safe Navigation Compose routes for the MVP flows
- `ProxMoxOpenTheme` and typography
- English and German string resources for the MVP screens
- GitHub Actions CI (lint, detekt, unit tests, debug APK build)
- F-Droid fastlane metadata in English and German
- Project documentation: README, SECURITY, CONTRIBUTING, CODE_OF_CONDUCT, design spec, implementation plan

## [0.1.0] — unreleased

First tagged development release (target).
