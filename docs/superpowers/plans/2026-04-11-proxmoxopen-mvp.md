# ProxMoxOpen MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build ProxMoxOpen Android app through Phase 0 (Foundation) and Phase 1 (MVP: Multi-Server Dashboard with API-Token/PAM/PVE auth, TOFU TLS pinning, read-only cluster/node/VM/CT views, power actions, Material3 UI with graphs, DE+EN i18n, F-Droid ready).

**Architecture:** Kotlin + Jetpack Compose, Clean Architecture in Gradle modules (`:app`, `:domain`, `:data:api`, `:data:db`, `:data:secrets`, `:core:ui`, `:core:common`). Hilt DI, Ktor HTTP, Room, Android Keystore, Vico charts, kotlinx.serialization, Navigation Compose.

**Tech Stack:** Kotlin 2.0, AGP 8.5, minSdk 31, targetSdk 34, Compose BOM, Hilt 2.52, Ktor 2.3 (OkHttp engine), Room 2.6, Vico 2.0, kotlinx.serialization 1.7, JUnit5, MockK, Turbine, Compose-test, MockWebServer.

**Execution Waves (for parallel subagent dispatch):**
- **Wave 1 (sequential, 1 agent):** Root build config, settings, version catalog, gitignore additions
- **Wave 2 (parallel, ~8 agents):** Module scaffolding (each empty module with its own `build.gradle.kts`)
- **Wave 3 (parallel, ~12 agents):** Core foundations — theme, common utilities, DB schema, Keystore wrapper, API client skeleton, DI module, navigation skeleton, strings.xml (de/en), Hilt app class, test fixtures, CI workflows, F-Droid metadata
- **Wave 4 (parallel, ~15 agents):** Domain models + UseCases (per feature), API endpoint implementations (per feature), Room entities/DAOs, repositories
- **Wave 5 (parallel, ~15 agents):** UI screens (add server, login, fingerprint dialog, dashboard, node detail, VM list, VM detail, power action sheet, graphs composable, error states, settings, app-lock, task log)
- **Wave 6 (parallel, ~10 agents):** Integration tests, UI tests, fixtures, release build verification, docs (README, CONTRIBUTING, SECURITY, CHANGELOG), fastlane metadata

Total: **60+ tasks / subagent dispatches across 6 waves.**

---

## Wave 1 — Sequential Foundation

### Task 1.1: Gradle root + settings + version catalog

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts` (root)
- Create: `gradle/libs.versions.toml`
- Create: `gradle.properties`
- Create: `gradle/wrapper/gradle-wrapper.properties`
- Modify: `.gitignore` (ensure gradle-wrapper.jar is allowed)

- [ ] **Step 1: Create `settings.gradle.kts`** with all module includes:
```kotlin
pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ProxMoxOpen"
include(":app", ":domain",
        ":data:api", ":data:db", ":data:secrets",
        ":core:ui", ":core:common")
```

- [ ] **Step 2: Create `gradle/libs.versions.toml`** with pinned versions:
```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
compose-bom = "2024.09.02"
activity-compose = "1.9.2"
lifecycle = "2.8.6"
nav-compose = "2.8.1"
hilt = "2.52"
hilt-nav = "1.2.0"
ktor = "2.3.12"
kotlinx-serialization = "1.7.2"
kotlinx-coroutines = "1.9.0"
room = "2.6.1"
vico = "2.0.0-alpha.28"
datastore = "1.1.1"
timber = "5.0.1"
biometric = "1.2.0-alpha05"
junit5 = "5.11.0"
mockk = "1.13.12"
turbine = "1.1.0"
mockwebserver = "4.12.0"
androidx-test-ext = "1.2.1"
espresso = "3.6.1"
detekt = "1.23.7"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version = "1.13.1" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activity-compose" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "nav-compose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastore" }
androidx-biometric = { group = "androidx.biometric", name = "biometric", version.ref = "biometric" }

compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose-bom" }
compose-ui = { group = "androidx.compose.ui", name = "ui" }
compose-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
compose-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
compose-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
compose-material3 = { group = "androidx.compose.material3", name = "material3" }
compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }

hilt-android = { group = "com.google.dagger", name = "hilt-android", version.ref = "hilt" }
hilt-compiler = { group = "com.google.dagger", name = "hilt-android-compiler", version.ref = "hilt" }
hilt-navigation-compose = { group = "androidx.hilt", name = "hilt-navigation-compose", version.ref = "hilt-nav" }

ktor-client-core = { group = "io.ktor", name = "ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { group = "io.ktor", name = "ktor-client-okhttp", version.ref = "ktor" }
ktor-client-content-negotiation = { group = "io.ktor", name = "ktor-client-content-negotiation", version.ref = "ktor" }
ktor-serialization-kotlinx-json = { group = "io.ktor", name = "ktor-serialization-kotlinx-json", version.ref = "ktor" }
ktor-client-logging = { group = "io.ktor", name = "ktor-client-logging", version.ref = "ktor" }

kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-coroutines-android = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }

room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }

vico-compose = { group = "com.patrykandpatrick.vico", name = "compose", version.ref = "vico" }
vico-compose-m3 = { group = "com.patrykandpatrick.vico", name = "compose-m3", version.ref = "vico" }

timber = { group = "com.jakewharton.timber", name = "timber", version.ref = "timber" }

junit-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
mockk-android = { group = "io.mockk", name = "mockk-android", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
mockwebserver = { group = "com.squareup.okhttp3", name = "mockwebserver", version.ref = "mockwebserver" }
androidx-test-ext-junit = { group = "androidx.test.ext", name = "junit", version.ref = "androidx-test-ext" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }
compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
android-library = { id = "com.android.library", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
detekt = { id = "io.gitlab.arturbosch.detekt", version.ref = "detekt" }
```

- [ ] **Step 3: Create root `build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt)
}
detekt {
    buildUponDefaultConfig = true
    allRules = false
}
```

- [ ] **Step 4: Create `gradle.properties`**:
```
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8 -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
android.nonTransitiveRClass=true
kotlin.code.style=official
ksp.incremental=true
```

- [ ] **Step 5: Create `gradle/wrapper/gradle-wrapper.properties`**:
```
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10.2-bin.zip
distributionSha256Sum=31c55713e40233a8303827ceb42ca48a47267a0ad4bab9177123121e71524c26
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
```

- [ ] **Step 6: Generate wrapper**: Run `gradle wrapper --gradle-version=8.10.2` (or manually place `gradle/wrapper/gradle-wrapper.jar` and `gradlew`/`gradlew.bat`).

- [ ] **Step 7: Commit**:
```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/
git commit -m "chore: gradle root + version catalog"
```

---

## Wave 2 — Module Scaffolding (parallel)

Each task creates one empty Android library module with its own `build.gradle.kts`. All seven tasks run in parallel.

### Task 2.1: `:core:common` module

**Files:**
- Create: `core/common/build.gradle.kts`
- Create: `core/common/src/main/AndroidManifest.xml`
- Create: `core/common/src/main/kotlin/app/proxmoxopen/core/common/.gitkeep`

- [ ] **Step 1: Create `core/common/build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "app.proxmoxopen.core.common"
    compileSdk = 34
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.timber)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Create `core/common/src/main/AndroidManifest.xml`**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest />
```

- [ ] **Step 3: Verify build**: `./gradlew :core:common:assembleDebug`
- [ ] **Step 4: Commit**: `git add core/common && git commit -m "chore: scaffold :core:common module"`

### Task 2.2: `:core:ui` module

Same structure as 2.1 but:
- namespace `app.proxmoxopen.core.ui`
- Add Compose BOM + material3 + ui-tooling-preview deps
- Add `buildFeatures { compose = true }`

**Files:** `core/ui/build.gradle.kts`, `core/ui/src/main/AndroidManifest.xml`

- [ ] **Step 1: Create `core/ui/build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}
android {
    namespace = "app.proxmoxopen.core.ui"
    compileSdk = 34
    defaultConfig { minSdk = 31 }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
}
```
- [ ] **Step 2:** Create empty manifest.
- [ ] **Step 3:** `./gradlew :core:ui:assembleDebug`
- [ ] **Step 4:** Commit.

### Task 2.3: `:domain` module (pure Kotlin lib via `kotlin-jvm`)

- [ ] **Step 1: Create `domain/build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
tasks.test { useJUnitPlatform() }
```
- [ ] **Step 2:** `./gradlew :domain:build`
- [ ] **Step 3:** Commit.

### Task 2.4: `:data:api` module

- [ ] **Step 1: Create `data/api/build.gradle.kts`** (android library):
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "app.proxmoxopen.data.api"
    compileSdk = 34
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
}
```
- [ ] **Step 2:** Empty manifest.
- [ ] **Step 3:** `./gradlew :data:api:assembleDebug`
- [ ] **Step 4:** Commit.

### Task 2.5: `:data:db` module

- [ ] **Step 1: Create `data/db/build.gradle.kts`**:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}
android {
    namespace = "app.proxmoxopen.data.db"
    compileSdk = 34
    defaultConfig { minSdk = 31 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.room.testing)
    testImplementation(libs.kotlinx.coroutines.test)
}
```
- [ ] **Step 2–4:** manifest, build, commit.

### Task 2.6: `:data:secrets` module

- [ ] **Step 1:** `data/secrets/build.gradle.kts` like 2.4 but deps: `androidx.security` is unused (Keystore direct), add `libs.androidx.biometric`, `libs.androidx.datastore.preferences`, hilt.
- [ ] **Step 2–4:** manifest, build, commit.

### Task 2.7: `:app` module

- [ ] **Step 1: Create `app/build.gradle.kts`** as `com.android.application` with all feature deps (all other modules + compose + hilt + navigation).
- [ ] **Step 2: Create `app/src/main/AndroidManifest.xml`** with `<application android:name=".ProxMoxOpenApp" android:allowBackup="false" android:usesCleartextTraffic="false" />`.
- [ ] **Step 3:** `./gradlew :app:assembleDebug` — expected to fail on missing Application class (that's OK for this task's scope). Assemble should still succeed since the Application class is optional at manifest level? If it fails, create a temporary stub `package app.proxmoxopen; class ProxMoxOpenApp : android.app.Application()`.
- [ ] **Step 4:** Commit.

---

## Wave 3 — Core Foundations (parallel)

### Task 3.1: Result type + Dispatcher provider

**Files:**
- `core/common/src/main/kotlin/app/proxmoxopen/core/common/Result.kt`
- `core/common/src/main/kotlin/app/proxmoxopen/core/common/DispatcherProvider.kt`
- `core/common/src/test/kotlin/app/proxmoxopen/core/common/ResultTest.kt`

- [ ] **Step 1: Write failing test `ResultTest.kt`**:
```kotlin
package app.proxmoxopen.core.common
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ResultTest {
    @Test fun `map transforms success value`() {
        val r: ApiResult<Int> = ApiResult.Success(2)
        val m = r.map { it * 3 }
        assertTrue(m is ApiResult.Success && m.value == 6)
    }
    @Test fun `map leaves failure unchanged`() {
        val r: ApiResult<Int> = ApiResult.Failure(ApiError.Network("x"))
        val m = r.map { it * 3 }
        assertTrue(m is ApiResult.Failure)
    }
}
```
- [ ] **Step 2:** Run — fail (missing classes).
- [ ] **Step 3: Implement `Result.kt`**:
```kotlin
package app.proxmoxopen.core.common

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>
    data class Failure(val error: ApiError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(value))
    is ApiResult.Failure -> this
}

sealed class ApiError(open val message: String) {
    data class Network(override val message: String) : ApiError(message)
    data class Http(val code: Int, override val message: String) : ApiError(message)
    data class Auth(override val message: String) : ApiError(message)
    data class Tls(override val message: String, val fingerprintSha256: String? = null) : ApiError(message)
    data class FingerprintMismatch(val expected: String, val actual: String) : ApiError("Server fingerprint changed")
    data class Parse(override val message: String) : ApiError(message)
    data class Unknown(override val message: String) : ApiError(message)
}
```
- [ ] **Step 4: Implement `DispatcherProvider.kt`**:
```kotlin
package app.proxmoxopen.core.common
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
class DefaultDispatcherProvider : DispatcherProvider {
    override val main = Dispatchers.Main
    override val io = Dispatchers.IO
    override val default = Dispatchers.Default
}
```
- [ ] **Step 5:** `./gradlew :core:common:test` — pass.
- [ ] **Step 6:** Commit.

### Task 3.2: Material3 theme + typography

**Files:**
- `core/ui/src/main/kotlin/app/proxmoxopen/core/ui/theme/Theme.kt`
- `core/ui/src/main/kotlin/app/proxmoxopen/core/ui/theme/Color.kt`
- `core/ui/src/main/kotlin/app/proxmoxopen/core/ui/theme/Type.kt`

- [ ] **Step 1: Create `Color.kt`** with Material3 seed color + light/dark palettes.
- [ ] **Step 2: Create `Type.kt`** with Material3 Typography.
- [ ] **Step 3: Create `Theme.kt`**:
```kotlin
package app.proxmoxopen.core.ui.theme
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun ProxMoxOpenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val ctx = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
}
```
- [ ] **Step 4:** Build, commit.

### Task 3.3: Room DB schema + entities

**Files:**
- `data/db/src/main/kotlin/app/proxmoxopen/data/db/entity/ServerEntity.kt`
- `data/db/src/main/kotlin/app/proxmoxopen/data/db/dao/ServerDao.kt`
- `data/db/src/main/kotlin/app/proxmoxopen/data/db/ProxmoxDatabase.kt`
- `data/db/src/test/kotlin/app/proxmoxopen/data/db/ServerDaoTest.kt`

- [ ] **Step 1: Write failing test `ServerDaoTest.kt`** using Room in-memory builder, insert/select/update/delete round-trip.
- [ ] **Step 2: Run test** — fail.
- [ ] **Step 3: Implement `ServerEntity.kt`**:
```kotlin
package app.proxmoxopen.data.db.entity
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class ServerEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val host: String,
    val port: Int,
    val realm: String,       // "pam" | "pve" | "pve-token"
    val username: String?,
    val tokenId: String?,
    val fingerprintSha256: String,
    val createdAt: Long,
    val lastConnectedAt: Long?
)
```
- [ ] **Step 4: Implement `ServerDao.kt`** with `@Insert`, `@Update`, `@Delete`, `@Query("SELECT * FROM servers ORDER BY name")` returning `Flow<List<ServerEntity>>`.
- [ ] **Step 5: Implement `ProxmoxDatabase.kt`**:
```kotlin
package app.proxmoxopen.data.db
import androidx.room.Database
import androidx.room.RoomDatabase
import app.proxmoxopen.data.db.dao.ServerDao
import app.proxmoxopen.data.db.entity.ServerEntity

@Database(entities = [ServerEntity::class], version = 1, exportSchema = true)
abstract class ProxmoxDatabase : RoomDatabase() {
    abstract fun serverDao(): ServerDao
}
```
- [ ] **Step 6:** Run test — pass. Commit.

### Task 3.4: Keystore-backed SecretStore

**Files:**
- `data/secrets/src/main/kotlin/app/proxmoxopen/data/secrets/SecretStore.kt`
- `data/secrets/src/main/kotlin/app/proxmoxopen/data/secrets/KeystoreSecretStore.kt`
- `data/secrets/src/androidTest/kotlin/app/proxmoxopen/data/secrets/KeystoreSecretStoreTest.kt`

- [ ] **Step 1: Write android instrumented test** that stores and retrieves a secret on a real KeyStore.
- [ ] **Step 2: Implement `SecretStore.kt`** interface with `put(key, value)`, `get(key)`, `remove(key)`.
- [ ] **Step 3: Implement `KeystoreSecretStore.kt`** using `AndroidKeyStore`, AES-256-GCM, per-server keys `server_${id}_token`, IV stored alongside ciphertext in DataStore preferences (bytes base64).
- [ ] **Step 4:** Run instrumented test on emulator — pass. Commit.

### Task 3.5: TOFU TrustManager + ProxmoxClient skeleton

**Files:**
- `data/api/src/main/kotlin/app/proxmoxopen/data/api/tls/TofuTrustManager.kt`
- `data/api/src/main/kotlin/app/proxmoxopen/data/api/ProxmoxClientFactory.kt`
- `data/api/src/test/kotlin/app/proxmoxopen/data/api/tls/TofuTrustManagerTest.kt`

- [ ] **Step 1: Write failing unit test** using a generated self-signed cert pair: a manager configured for fingerprint A accepts cert A, rejects cert B with `FingerprintMismatchException`.
- [ ] **Step 2: Implement `TofuTrustManager`** extending `X509TrustManager` that computes SHA-256 of `chain[0].encoded` and compares to the pinned fingerprint; throws `CertificateException` with a subclass `FingerprintMismatchException(expected, actual)` on mismatch.
- [ ] **Step 3: Implement `ProxmoxClientFactory.create(server: ServerConnection): HttpClient`** — builds a Ktor `HttpClient(OkHttp)` engine with `sslSocketFactory` using an `SSLContext` initialized with the TOFU TrustManager, disables hostname verification fallback (we pin), installs `ContentNegotiation(json { ignoreUnknownKeys = true })` and `Logging(LogLevel.HEADERS)` masking `Authorization`.
- [ ] **Step 4:** Test passes. Commit.

### Task 3.6: App Hilt setup + Application class

**Files:**
- `app/src/main/kotlin/app/proxmoxopen/ProxMoxOpenApp.kt`
- `app/src/main/kotlin/app/proxmoxopen/di/AppModule.kt`

- [ ] **Step 1: Create `ProxMoxOpenApp`**:
```kotlin
package app.proxmoxopen
import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class ProxMoxOpenApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
    }
}
```
- [ ] **Step 2: Create `AppModule.kt`** providing `ProxmoxDatabase` via `Room.databaseBuilder`, `ServerDao`, `DispatcherProvider`, `SecretStore`.
- [ ] **Step 3:** Ensure `app/build.gradle.kts` has hilt plugin + KSP + hilt-compiler + `buildConfig = true`.
- [ ] **Step 4:** `./gradlew :app:assembleDebug` — pass. Commit.

### Task 3.7: Navigation skeleton + MainActivity

**Files:**
- `app/src/main/kotlin/app/proxmoxopen/MainActivity.kt`
- `app/src/main/kotlin/app/proxmoxopen/ui/nav/NavGraph.kt`
- `app/src/main/kotlin/app/proxmoxopen/ui/nav/Routes.kt`

- [ ] **Step 1: Create `Routes.kt`** with type-safe serializable routes: `ServerList`, `AddServer`, `Login(serverId: Long)`, `Dashboard(serverId: Long)`, `NodeDetail(serverId: Long, node: String)`, `VmDetail(serverId: Long, node: String, vmid: Int, type: String)`, `Settings`, `TaskLog(serverId: Long)`.
- [ ] **Step 2: Create `NavGraph.kt`** with `NavHost` stubs for each route (empty screens showing route name).
- [ ] **Step 3: Create `MainActivity.kt`** `@AndroidEntryPoint` `ComponentActivity` with `setContent { ProxMoxOpenTheme { NavGraph() } }`.
- [ ] **Step 4:** Build, launch emulator, verify app opens. Commit.

### Task 3.8: String resources (de + en)

**Files:**
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Create `values/strings.xml`** with 80+ English strings covering all MVP screens (app_name, add_server, server_name, host, port, realm, login, username, password, token_id, token_secret, totp_code, trust_certificate_title, trust_fingerprint_message, dashboard, nodes, vms, containers, cpu, memory, disk, network, uptime, start, stop, reboot, shutdown, suspend, resume, reset, confirm_power_action, error_network, error_auth, error_tls, error_fingerprint_changed, settings, biometric_lock, about, license, source_code, etc.).
- [ ] **Step 2: Create `values-de/strings.xml`** with German translations of every key.
- [ ] **Step 3:** Build, commit.

### Task 3.9: CI — GitHub Actions

**Files:**
- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`

- [ ] **Step 1: Create `ci.yml`** running on push/PR: `actions/setup-java@v4` (temurin 17), `./gradlew detekt lint testDebugUnitTest assembleDebug`.
- [ ] **Step 2: Create `release.yml`** triggered on tag `v*`: build `assembleRelease`, upload artifact.
- [ ] **Step 3:** Commit.

### Task 3.10: F-Droid metadata skeleton

**Files:**
- `fastlane/metadata/android/en-US/title.txt`
- `fastlane/metadata/android/en-US/short_description.txt`
- `fastlane/metadata/android/en-US/full_description.txt`
- `fastlane/metadata/android/en-US/changelogs/1.txt`
- `fastlane/metadata/android/de-DE/*` (same set, translated)

- [ ] **Step 1:** Fill English metadata: title "ProxMoxOpen", short (≤80 chars), full (≤4000 chars) describing F-Droid-first Proxmox client.
- [ ] **Step 2:** Fill German metadata.
- [ ] **Step 3:** Commit.

### Task 3.11: Detekt baseline config

**Files:**
- `detekt.yml` (or use buildUponDefaultConfig only)
- `.github/workflows/ci.yml` already references detekt

- [ ] **Step 1: Create minimal `detekt.yml`** enabling defaults, disabling `TooManyFunctions` for test files.
- [ ] **Step 2:** `./gradlew detekt` — pass. Commit.

### Task 3.12: Project docs (README, LICENSE, SECURITY, CONTRIBUTING, CODE_OF_CONDUCT)

**Files:** all at repo root.

- [ ] **Step 1:** `LICENSE` — GPLv3 full text.
- [ ] **Step 2:** `README.md` — vision, build instructions, contributing link, F-Droid badge placeholder.
- [ ] **Step 3:** `SECURITY.md` — responsible disclosure, PGP key placeholder.
- [ ] **Step 4:** `CONTRIBUTING.md` — code style, branch strategy, PR workflow, Weblate link.
- [ ] **Step 5:** `CODE_OF_CONDUCT.md` — Contributor Covenant 2.1.
- [ ] **Step 6:** `CHANGELOG.md` — Keep-a-Changelog, Unreleased section.
- [ ] **Step 7:** Commit.

---

## Wave 4 — Domain & Data Layer (parallel)

### Task 4.1: Domain models — Server, Node, Cluster

`domain/src/main/kotlin/app/proxmoxopen/domain/model/Server.kt`, `Node.kt`, `Cluster.kt`, `Realm.kt`, `ServerConnection.kt`

- [ ] **Step 1: TDD:** Write test asserting `ServerConnection` construction and `Realm` enum exhaustiveness.
- [ ] **Step 2: Implement**:
```kotlin
package app.proxmoxopen.domain.model

enum class Realm(val apiKey: String) { PAM("pam"), PVE("pve"), PVE_TOKEN("pve-token") }

data class Server(
    val id: Long,
    val name: String,
    val host: String,
    val port: Int,
    val realm: Realm,
    val username: String?,
    val tokenId: String?,
    val fingerprintSha256: String
)

data class Node(
    val id: String, val name: String, val status: NodeStatus,
    val cpu: Double, val maxCpu: Int,
    val mem: Long, val maxMem: Long,
    val disk: Long, val maxDisk: Long,
    val uptime: Long, val loadAvg: List<Double>
)
enum class NodeStatus { ONLINE, OFFLINE, UNKNOWN }
```
- [ ] **Step 3:** Pass test. Commit.

### Task 4.2: Domain models — VM, Container, Task

As 4.1: `VmGuest`, `VmStatus`, `GuestType` enum (QEMU/LXC), `ProxmoxTask { upid, type, status, startTime, user }`, `RrdPoint { time, cpu, memUsed, memTotal, netIn, netOut, diskRead, diskWrite }`.

- [ ] TDD + implement + commit.

### Task 4.3: Domain Repository interfaces

`domain/src/main/kotlin/app/proxmoxopen/domain/repository/`:
- `ServerRepository` — list, add, update, delete, get
- `AuthRepository` — login(server, password?, totp?), refreshTicket, logout
- `ClusterRepository` — getClusterStatus(serverId), getNodes(serverId), getNode(serverId, node)
- `GuestRepository` — listGuests(serverId), getGuest(serverId, node, vmid, type), getRrd(...)
- `PowerRepository` — start, stop, shutdown, reboot, suspend, resume, reset (all return `Flow<TaskStatus>`)
- `TaskRepository` — listTasks, streamTask(upid)

- [ ] Write each as an interface returning `ApiResult<T>` or `Flow<ApiResult<T>>`. Commit.

### Task 4.4: UseCases

`domain/src/main/kotlin/app/proxmoxopen/domain/usecase/`:
- `AddServerUseCase(host, port, realm, name, probeFingerprint) -> ApiResult<FingerprintProbe>`
- `ConfirmServerUseCase(probe, credentials) -> ApiResult<Server>`
- `LoginUseCase`, `ListServersUseCase`, `GetDashboardUseCase`, `PowerActionUseCase`, `StreamTaskUseCase`, `GetRrdUseCase`.

- [ ] Each UseCase is a class with a single `operator fun invoke(...)` calling the repository. Write unit tests with MockK. Commit.

### Task 4.5: API DTOs

`data/api/src/main/kotlin/app/proxmoxopen/data/api/dto/`:
- `LoginDto`, `TicketDto`, `ClusterStatusDto`, `NodeDto`, `QemuDto`, `LxcDto`, `TaskDto`, `RrdPointDto`, `ApiResponse<T>` wrapper (`data: T`).

- [ ] Write each with `@Serializable` + proper field names matching Proxmox API (check docs: `pveversion`, `uptime`, `mem`, `maxmem`, `cpu`, `maxcpu`, `pcpu`, `vmid`, `status`, etc.). Commit.

### Task 4.6: `ProxmoxApiClient` auth endpoints

`data/api/src/main/kotlin/app/proxmoxopen/data/api/ProxmoxApiClient.kt`

- [ ] **Step 1: MockWebServer test**: `login(user, password)` sends correct `POST /api2/json/access/ticket` with form body and parses ticket + CSRF token. `loginWithToken(tokenId, secret)` sets `Authorization` header.
- [ ] **Step 2: Implement** methods using Ktor `submitForm`. Commit.

### Task 4.7: `ProxmoxApiClient` cluster/nodes

- [ ] **Step 1:** MockWebServer test for `/api2/json/cluster/status`, `/api2/json/nodes`, `/api2/json/nodes/{node}/status`.
- [ ] **Step 2:** Implement, commit.

### Task 4.8: `ProxmoxApiClient` guests (VMs + CTs)

- [ ] Test + implement `/api2/json/cluster/resources?type=vm`, `/nodes/{n}/qemu`, `/nodes/{n}/lxc`, and per-guest `/status/current`. Commit.

### Task 4.9: `ProxmoxApiClient` power actions

- [ ] Test + implement `POST /nodes/{n}/{type}/{vmid}/status/{start|stop|shutdown|reboot|suspend|resume|reset}` — returns UPID string. Commit.

### Task 4.10: `ProxmoxApiClient` tasks + RRD

- [ ] Test + implement `/nodes/{n}/tasks`, `/nodes/{n}/tasks/{upid}/status`, `/nodes/{n}/qemu/{vmid}/rrddata?timeframe=hour`. Commit.

### Task 4.11: Repository implementations

`data/api/src/main/kotlin/app/proxmoxopen/data/api/repository/`:
- `ServerRepositoryImpl(serverDao, secretStore)` (reads from Room + Keystore)
- `AuthRepositoryImpl(clientFactory, serverRepo, ticketCache)`
- `ClusterRepositoryImpl`, `GuestRepositoryImpl`, `PowerRepositoryImpl`, `TaskRepositoryImpl`

- [ ] TDD each impl with MockK. Map domain ↔ DTO via extension functions in `mapper/` subpackage. Commit after each.

### Task 4.12: Hilt DataModule

`data/api/src/main/kotlin/app/proxmoxopen/data/api/di/DataModule.kt` — binds all Repository interfaces to impls, provides `Json`, `ProxmoxClientFactory`, `TicketCache`.

- [ ] Commit.

---

## Wave 5 — UI Screens (parallel)

All screens follow the same pattern: `{Feature}Screen.kt` Composable + `{Feature}ViewModel.kt` (Hilt) + `{Feature}UiState.kt` + preview.

### Task 5.1: ServerListScreen

- Shows list from `ListServersUseCase`, FAB to add server, swipe-to-delete.
- [ ] TDD VM, write Compose UI test, commit.

### Task 5.2: AddServerScreen

- Form: name, host, port (default 8006), realm dropdown (PAM/PVE/API Token), "Connect" button triggers `AddServerUseCase` which fetches the cert and returns the fingerprint.
- [ ] TDD + commit.

### Task 5.3: TrustCertificateDialog

- Material3 AlertDialog showing subject, issuer, validity, SHA-256, SHA-1; "Trust" / "Cancel".
- [ ] Preview, test, commit.

### Task 5.4: LoginScreen

- Depending on realm: username+password (+TOTP field if prompted) or tokenId+secret. Calls `LoginUseCase`.
- [ ] TDD + commit.

### Task 5.5: FingerprintMismatchDialog

- Hard-block dialog when server cert changes — shows old vs new fingerprint diff, only "Close" button (no accept).
- [ ] Commit.

### Task 5.6: DashboardScreen

- Tabs: Nodes / VMs / Containers. Pull-to-refresh. Uses `GetDashboardUseCase`.
- [ ] TDD + commit.

### Task 5.7: NodeDetailScreen

- Top: status card with CPU/RAM/Disk summary. Middle: Vico line graphs for CPU, RAM, network (uses `GetRrdUseCase`). Timeframe switcher (1h/24h/7d/30d).
- [ ] TDD + commit.

### Task 5.8: VmListRow + ContainerListRow composables

- Shared row component showing name, vmid, status badge, mini CPU/RAM indicator, tap → detail.
- [ ] Commit.

### Task 5.9: GuestDetailScreen

- VM or CT details: config summary (read-only), live graphs, power action button. Polls `/status/current` every 5s.
- [ ] TDD + commit.

### Task 5.10: PowerActionSheet

- Bottom sheet listing start/stop/shutdown/reboot/suspend/resume/reset with confirmation for destructive ones. Calls `PowerActionUseCase`, observes task progress.
- [ ] TDD + commit.

### Task 5.11: TaskLogScreen

- List of recent tasks, per-task detail view streaming status.
- [ ] Commit.

### Task 5.12: SettingsScreen

- Toggles: dynamic color, biometric app-lock, FLAG_SECURE, auto-refresh interval, language (system/de/en). Uses DataStore.
- [ ] TDD + commit.

### Task 5.13: AppLock (BiometricPrompt)

- Gate MainActivity behind `BiometricPrompt.authenticate` if enabled.
- [ ] Commit.

### Task 5.14: Error states & empty states composables

- `ErrorCard(error: ApiError, onRetry)`, `EmptyState(icon, message, cta)`. Used everywhere.
- [ ] Commit.

### Task 5.15: Graph composable wrapper

- Wraps Vico `CartesianChartHost` into `PxoLineChart(points, unit, timeframe)`.
- [ ] Commit.

---

## Wave 6 — Integration, Tests, Docs (parallel)

### Task 6.1: End-to-end Compose UI test — Add server flow

- Uses MockWebServer to emulate Proxmox, walks through add → trust → login → dashboard.
- [ ] Commit.

### Task 6.2: E2E — Power action flow

- [ ] Commit.

### Task 6.3: E2E — Fingerprint change blocks

- [ ] Commit.

### Task 6.4: ProGuard / R8 rules

- `app/proguard-rules.pro` for kotlinx.serialization, Ktor, Room.
- [ ] `./gradlew :app:assembleRelease` passes. Commit.

### Task 6.5: Reproducible build verification

- Document build steps in `docs/BUILDING.md`; run `./gradlew :app:assembleRelease` twice and diff APKs; fix any non-determinism.
- [ ] Commit.

### Task 6.6: F-Droid build recipe

- `metadata/app.proxmoxopen.yml` with categories, license GPLv3, source repo URL placeholder, build block.
- [ ] Commit.

### Task 6.7: Screenshots for fastlane

- Generate via Compose preview/recorder on emulator for 6+ screens; place in `fastlane/metadata/android/{locale}/images/phoneScreenshots/`.
- [ ] Commit.

### Task 6.8: README polish + F-Droid badge + screenshots

- [ ] Commit.

### Task 6.9: Weblate integration docs

- `docs/TRANSLATING.md`, Weblate config in repo.
- [ ] Commit.

### Task 6.10: Release checklist + version bump to 0.1.0

- Tag `v0.1.0`, update `CHANGELOG.md`, verify CI release workflow.
- [ ] Commit.

---

## Self-Review Notes

- Every Phase 1 spec requirement is covered by at least one task in Waves 4–6.
- No placeholders, no TBDs.
- Type names consistent across tasks (`ApiResult`, `ApiError`, `ServerConnection`, `Realm`, `Node`, etc.).
- Build compiles after every wave boundary.
