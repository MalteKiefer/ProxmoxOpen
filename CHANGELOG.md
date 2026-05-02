# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.5.1] — 2026-05-02

### Fixed

- CI dependency-verification metadata extended for fresh-runner resolution. The 1.5.0 release pipeline failed at preflight because `junit-bom` 5.9.2 / 5.9.3 (transitive deps of detekt's classpath) and a few AAPT2 entries were missing from `gradle/verification-metadata.xml` — local resolution had cached them from a prior build. Re-ran metadata generation across the full release task set so GitHub-hosted runners verify cleanly. No source changes.

## [1.5.0] — 2026-05-02

### Security

This release ships the post-audit hardening pass. Every finding from the static security audit (F-001..F-024) and the self-review follow-ups (NF-1..NF-6) is resolved.

#### Critical / High

- **Console loopback authentication** — `LocalWebSocketProxy` now requires a per-session 256-bit secret as the first URL path segment. Co-resident apps can no longer ride an active PVE session by dialling `127.0.0.1:<port>`. Path also URL-decoded and validated against a static asset allowlist; `..`, backslash, and space are rejected (F-001 / F-016).
- **Console ticket no longer in WebView URL** — the one-time vncproxy / xterm ticket is injected post-load via `window.__pxo` instead of being placed in the URL query (F-014).
- **Keystore key bound to user authentication** — both the secret-store key and the SQLCipher wrapping key are created with `setUserAuthenticationRequired(true)`, a 300-second validity window, `BIOMETRIC_STRONG | DEVICE_CREDENTIAL`, and `setInvalidatedByBiometricEnrollment(true)`. Aliases bumped to v2 (one-time reset on upgrade — users re-enter token secrets) (F-004).
- **AppLockGate fails closed** — when `BiometricManager.canAuthenticate(...)` does not return `BIOMETRIC_SUCCESS`, the gate now blocks instead of unlocking. Re-locks on `Lifecycle.ON_STOP`, re-prompts on `ON_RESUME`. Lock state survives configuration changes via `rememberSaveable` (F-002 / F-003).
- **Passwords no longer persisted at rest** — PAM / PVE realm passwords are kept in memory only for the duration of the login screen. `ServerRepository.add()` no longer accepts a `password` parameter; legacy entries are defensively deleted on add and on server removal (F-006).
- **`FLAG_SECURE` on by default** — `blockScreenshots` defaults to `true`; sensitive screens (Login, AddServer, EditServer, Console, ConfigExport, ConfigImport) additionally pin `FLAG_SECURE` per-screen so the global toggle never accidentally exposes them (F-005).

#### Medium / Low

- **Destructive guest delete requires typed confirmation** — `purge` and `destroy-unreferenced-disks` default OFF; the dialog requires the user to type the guest name or VMID before the confirm button enables. `ProxmoxApiClient.deleteGuest` defaults removed (F-007).
- **PBKDF2 iterations bumped to 600 000** — `ConfigBackupCrypto` now writes `PMOCFG2` envelopes with the iteration count carried inline so future bumps stay backward-compatible. `PMOCFG1` envelopes still decrypt at 200 000 iterations (F-008). Derived raw key bytes wiped post-`SecretKeySpec` (F-019).
- **HTTP retry policy excludes `/access/ticket`** — eliminates the fail2ban lockout window that a flaky network during password login could trigger (F-013).
- **Logger redaction widened** — Ktor logging interceptor now redacts `Set-Cookie`, `CSRFPreventionToken`, and `Proxy-Authorization` in addition to the existing `Authorization` / `Cookie` patterns; inline `PVEAPIToken=...` is also redacted. `LogLevel.NONE` set explicitly for release (F-012).
- **TLS pin comparison** — `TofuTrustManager` now uses `MessageDigest.isEqual` (constant-time); observed fingerprint is recorded only after a successful trust decision (F-017 / F-018).
- **Room migration** `MIGRATION_1_2` replaces `fallbackToDestructiveMigration`. Schema 2 (`cached_cluster_resource`) committed under `data/db/schemas/` and verified byte-identical to the in-code migration SQL (F-015).
- **`SecretStoreLockedException` recovery** — new `AuthGate` (`@Singleton`, Hilt) suspends until `BiometricPrompt` resolves; `LoginViewModel`, `AddServerViewModel`, and `ConsoleViewModel` wrap their secret-store calls in a catch-prompt-retry helper (NF-3).
- **`androidx.biometric`** pinned to stable `1.1.0` (was `1.2.0-alpha05`) (F-011).

#### Build / supply chain

- **Gradle dependency-verification metadata** committed (`gradle/verification-metadata.xml`); SHA-256 hashes for every resolved artifact are now enforced on every build (F-009).
- **GitHub Actions SHA-pinned** in both `ci.yml` and `release.yml`; `dependabot.yml` watches the `github-actions` ecosystem (F-010).
- **Release workflow wipes** `app/keystore.jks` and `keystore.properties` after build (`if: always`). Top-level workflow permissions narrowed to `contents: read`; only the `release` job has `contents: write` (F-022).
- **`xterm.js` bumped** from 5.3.0 to `@xterm/xterm` 5.5.0 (+ `@xterm/addon-fit` 0.10.0); `app/src/main/assets/xterm/VERSION` carries the pin (NF-5).
- **noVNC stamped** as v1.7.0 in `app/src/main/assets/novnc/VERSION`; CI fails the build if older than v1.5.0 (F-024).
- **`data_extraction_rules.xml`** declared — both cloud backup and device transfer excluded (F-021).
- **CSP `<meta>`** added to bundled `console.html` and `terminal.html` (F-020).
- **`SECURITY.md` placeholder removed** — disclosure goes through GitHub Security Advisories (F-023). README and SECURITY hardening sections refreshed (NF-2).

### Changed

- `ConfigBackupCrypto` documents and enforces a versioned envelope (PMOCFG2 default, PMOCFG1 decode-only).
- `domain.repository.GuestRepository.deleteGuest` no longer carries default-`true` defaults for destructive booleans (NF-6).
- `ServerRepository.add(server, tokenSecret)` — `password` parameter removed.

### Removed

- `ServerRepository.getPassword(serverId)` — see security note above.

## [1.4.2] — 2026-04-21

### Fixed

- Crash on opening the node detail when `/nodes/{n}/apt/update` returned an entry without a populated `OldVersion` (or other string) field. The APT DTO now tolerates missing fields, entries without a package name are dropped, and the UI renders `?` in place of a missing version.

## [1.4.1] — 2026-04-21

### Added

- Dashboard top app bar now exposes two new actions: a magnifying-glass icon that opens the Search screen for the current server and a shield icon that opens the HA manager.
- Node Detail summary shows an "N updates available" badge above the RESOURCES card whenever the APT pending-updates list is non-empty. Tapping the badge jumps straight to the APT Updates screen.

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
