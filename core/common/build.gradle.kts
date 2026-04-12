plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "de.kiefer_networks.proxmoxopen.core.common"
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

tasks.withType<Test> { useJUnitPlatform() }
