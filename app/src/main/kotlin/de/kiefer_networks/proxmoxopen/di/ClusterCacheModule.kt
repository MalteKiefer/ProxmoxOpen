package de.kiefer_networks.proxmoxopen.di

import de.kiefer_networks.proxmoxopen.data.api.repository.ClusterCacheRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.db.ProxmoxDatabase
import de.kiefer_networks.proxmoxopen.data.db.dao.ClusterCacheDao
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterCacheRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for the offline cluster-resource cache.
 *
 * Kept separate from [AppModule] so the cache layer can be added/removed without
 * touching the main graph — follows the same pattern as `AptModule` and `HaModule`.
 */
@Module
@InstallIn(SingletonComponent::class)
object ClusterCacheModule {

    @Provides
    @Singleton
    fun provideClusterCacheDao(db: ProxmoxDatabase): ClusterCacheDao = db.clusterCacheDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ClusterCacheBindModule {

    @Binds
    @Singleton
    abstract fun bindClusterCacheRepository(
        impl: ClusterCacheRepositoryImpl,
    ): ClusterCacheRepository
}
