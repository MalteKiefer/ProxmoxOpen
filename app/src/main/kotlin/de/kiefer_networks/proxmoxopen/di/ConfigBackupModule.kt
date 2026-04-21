package de.kiefer_networks.proxmoxopen.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module placeholder for the config-backup feature.
 *
 * [de.kiefer_networks.proxmoxopen.ui.configbackup.ConfigBackupRepository] is provided via
 * its `@Singleton @Inject constructor(...)` directly, so this module currently holds no
 * bindings. It exists so future feature-level bindings (e.g. a future test-only
 * [dagger.Binds] abstraction) land next to other backup plumbing rather than in
 * [AppModule].
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigBackupModule
