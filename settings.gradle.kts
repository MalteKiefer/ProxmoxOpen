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
        ":core:ui", ":core:common",
        ":terminal-emulator", ":terminal-view")
