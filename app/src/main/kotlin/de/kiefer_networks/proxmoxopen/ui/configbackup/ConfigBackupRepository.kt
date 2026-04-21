package de.kiefer_networks.proxmoxopen.ui.configbackup

import de.kiefer_networks.proxmoxopen.data.db.dao.ServerDao
import de.kiefer_networks.proxmoxopen.data.db.entity.ServerEntity
import de.kiefer_networks.proxmoxopen.preferences.LanguageOption
import de.kiefer_networks.proxmoxopen.preferences.RefreshInterval
import de.kiefer_networks.proxmoxopen.preferences.TerminalFontSize
import de.kiefer_networks.proxmoxopen.preferences.TerminalTheme
import de.kiefer_networks.proxmoxopen.preferences.ThemeMode
import de.kiefer_networks.proxmoxopen.preferences.UserPreferences
import de.kiefer_networks.proxmoxopen.preferences.UserPreferencesRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Collects the current server list + user preferences into an [ExportedConfig], and
 * applies an incoming [ExportedConfig] in either [ImportStrategy.Replace] or
 * [ImportStrategy.Merge] mode.
 *
 * Secrets (passwords / API token secrets) are intentionally omitted from the export
 * and never touched on import. Users re-enter credentials via the login screen.
 */
@Singleton
class ConfigBackupRepository @Inject constructor(
    private val serverDao: ServerDao,
    private val prefsRepo: UserPreferencesRepository,
) {

    suspend fun collect(): ExportedConfig {
        val servers = serverDao.observeAll().first()
        val prefs = prefsRepo.preferences.first()
        return ExportedConfig(
            version = 1,
            exportedAt = System.currentTimeMillis(),
            servers = servers.map { it.toExported() },
            preferences = prefs.toExported(),
        )
    }

    suspend fun apply(config: ExportedConfig, strategy: ImportStrategy): ImportSummary {
        val existing = serverDao.observeAll().first()
        var inserted = 0
        var skipped = 0
        var removed = 0

        if (strategy == ImportStrategy.Replace) {
            existing.forEach { serverDao.delete(it) }
            removed = existing.size
            config.servers.forEach { dto ->
                serverDao.insert(dto.toEntityForInsert())
                inserted++
            }
        } else {
            val existingKeys = existing.map { Triple(it.host, it.port, it.username) }.toSet()
            config.servers.forEach { dto ->
                val key = Triple(dto.host, dto.port, dto.username)
                if (existingKeys.contains(key)) {
                    skipped++
                } else {
                    serverDao.insert(dto.toEntityForInsert())
                    inserted++
                }
            }
        }

        applyPreferences(config.preferences)
        return ImportSummary(inserted = inserted, skipped = skipped, removed = removed)
    }

    private suspend fun applyPreferences(p: ExportedPreferences) {
        p.themeMode?.let { raw ->
            runCatching { ThemeMode.valueOf(raw) }.getOrNull()?.let { prefsRepo.setThemeMode(it) }
        }
        p.useDynamicColor?.let { prefsRepo.setDynamicColor(it) }
        p.language?.let { raw ->
            runCatching { LanguageOption.valueOf(raw) }.getOrNull()?.let { prefsRepo.setLanguage(it) }
        }
        p.appLockEnabled?.let { prefsRepo.setAppLockEnabled(it) }
        p.blockScreenshots?.let { prefsRepo.setBlockScreenshots(it) }
        p.refreshInterval?.let { raw ->
            runCatching { RefreshInterval.valueOf(raw) }.getOrNull()?.let {
                prefsRepo.setRefreshInterval(it)
            }
        }
        p.terminalFontSize?.let { raw ->
            runCatching { TerminalFontSize.valueOf(raw) }.getOrNull()?.let {
                prefsRepo.setTerminalFontSize(it)
            }
        }
        p.terminalTheme?.let { raw ->
            runCatching { TerminalTheme.valueOf(raw) }.getOrNull()?.let {
                prefsRepo.setTerminalTheme(it)
            }
        }
    }
}

enum class ImportStrategy { Replace, Merge }

data class ImportSummary(val inserted: Int, val skipped: Int, val removed: Int)

private fun ServerEntity.toExported(): ExportedServer = ExportedServer(
    id = id,
    name = name,
    host = host,
    port = port,
    realm = realm,
    username = username,
    tokenId = tokenId,
    fingerprintSha256 = fingerprintSha256,
    createdAt = createdAt,
    lastConnectedAt = lastConnectedAt,
)

/**
 * Build a [ServerEntity] for insertion. We reset [ServerEntity.id] to 0 so Room
 * autogenerates a new primary key — otherwise a replace-then-insert of the same ids
 * across devices would collide with existing rows on a merge.
 */
private fun ExportedServer.toEntityForInsert(): ServerEntity = ServerEntity(
    id = 0,
    name = name,
    host = host,
    port = port,
    realm = realm,
    username = username,
    tokenId = tokenId,
    fingerprintSha256 = fingerprintSha256,
    createdAt = createdAt,
    lastConnectedAt = lastConnectedAt,
)

private fun UserPreferences.toExported(): ExportedPreferences = ExportedPreferences(
    themeMode = themeMode.name,
    useDynamicColor = useDynamicColor,
    amoledBlack = null, // not present in this build's UserPreferences; exported for forward compat
    language = language.name,
    appLockEnabled = appLockEnabled,
    blockScreenshots = blockScreenshots,
    refreshInterval = refreshInterval.name,
    terminalFontSize = terminalFontSize.name,
    terminalTheme = terminalTheme.name,
)
