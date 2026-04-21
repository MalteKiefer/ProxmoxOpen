package de.kiefer_networks.proxmoxopen.preferences

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class LanguageOption(val languageTag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    GERMAN("de"),
    FRENCH("fr"),
    SPANISH("es"),
    ITALIAN("it"),
}

enum class RefreshInterval(val seconds: Int, val label: String) {
    SEC_5(5, "5s"),
    SEC_10(10, "10s"),
    SEC_15(15, "15s"),
    SEC_30(30, "30s"),
    SEC_60(60, "60s"),
    OFF(0, "Off"),
}

enum class TerminalFontSize(val px: Int) {
    TINY(10), SMALL(12), MEDIUM(14), LARGE(16), XLARGE(18), XXLARGE(20), HUGE(24), MASSIVE(28)
}

enum class TerminalTheme {
    DARK, LIGHT, SOLARIZED_DARK, SOLARIZED_LIGHT, DRACULA, MONOKAI, NORD, GRUVBOX_DARK, GRUVBOX_LIGHT, ONE_DARK, TOKYO_NIGHT, CATPPUCCIN, PROXMOX
}

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useDynamicColor: Boolean = false,
    val amoledBlack: Boolean = false,
    val language: LanguageOption = LanguageOption.SYSTEM,
    val appLockEnabled: Boolean = false,
    val blockScreenshots: Boolean = false,
    val refreshInterval: RefreshInterval = RefreshInterval.SEC_15,
    val terminalFontSize: TerminalFontSize = TerminalFontSize.MEDIUM,
    val terminalTheme: TerminalTheme = TerminalTheme.DARK,
    /**
     * When true, the dashboard persists the live cluster-resource list on every
     * successful fetch and falls back to the cached snapshot if the device goes
     * offline. Defaults to on so users see continuity of data after a lost
     * connection; disable to opt out of on-disk caching entirely.
     */
    val offlineCacheEnabled: Boolean = true,
)
