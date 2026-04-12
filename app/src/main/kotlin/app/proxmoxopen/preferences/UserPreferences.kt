package app.proxmoxopen.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LanguageOption(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    GERMAN("de"),
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val language: LanguageOption = LanguageOption.SYSTEM,
    val appLockEnabled: Boolean = false,
)
