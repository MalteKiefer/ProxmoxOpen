# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
