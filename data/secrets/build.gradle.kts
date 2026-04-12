plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "de.kiefer_networks.proxmoxopen.data.secrets"
    compileSdk = 34
    defaultConfig {
        minSdk = 31
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":core:common"))
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.mockk.android)
}

tasks.withType<Test> { useJUnitPlatform() }
