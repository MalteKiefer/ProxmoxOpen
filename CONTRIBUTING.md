# Contributing

Thank you for taking the time to contribute to ProxMoxOpen. The goal of this project is a secure, transparent, open-source Proxmox VE client that ships on F-Droid and respects the user.

## Ground rules

- Be kind. See [`CODE_OF_CONDUCT.md`](CODE_OF_CONDUCT.md).
- One focused change per pull request. Keep refactors separate from feature work.
- Follow the existing architecture (Clean Architecture with `:app`, `:domain`, `:data:*`, `:core:*` modules).
- Never introduce a proprietary dependency, tracker, or network service that is not strictly required for the app to function. F-Droid compliance is not negotiable.

## Development

```sh
./gradlew :app:assembleDebug          # build debug APK
./gradlew testDebugUnitTest           # unit tests
./gradlew :app:connectedDebugAndroidTest  # instrumented tests
./gradlew detekt :app:lintDebug       # static analysis
```

A quick combined check you can run before every commit:

```sh
./gradlew detekt :app:lintDebug testDebugUnitTest :app:assembleDebug
```

## Commit messages

We use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`, `test:`).

## Pull requests

- Target `main`
- Describe *why* the change is needed, not just *what* changes
- Link to any related issue or design spec in `docs/superpowers/specs`
- Include tests for user-facing behavior

## Translations

Translations are handled via Weblate (see `docs/TRANSLATING.md` when available). Please do **not** submit translation PRs directly.

## Reporting bugs

Open an issue on the source repository. Include:

- Android version and device model
- Proxmox VE version
- Steps to reproduce and expected vs actual behavior
- Logs if available (with credentials redacted)
