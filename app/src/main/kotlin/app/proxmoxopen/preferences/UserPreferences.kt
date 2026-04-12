package app.proxmoxopen.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LanguageOption(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    GERMAN("de"),
}

enum class RefreshInterval(val seconds: Int, val label: String) {
    SEC_5(5, "5s"),
    SEC_10(10, "10s"),
    SEC_15(15, "15s"),
    SEC_30(30, "30s"),
    SEC_60(60, "60s"),
    OFF(0, "Off"),
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColor: Boolean = false,
    val language: LanguageOption = LanguageOption.SYSTEM,
    val appLockEnabled: Boolean = false,
    val refreshInterval: RefreshInterval = RefreshInterval.SEC_15,
)
