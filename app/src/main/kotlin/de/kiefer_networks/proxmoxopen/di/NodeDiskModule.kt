package de.kiefer_networks.proxmoxopen.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kiefer_networks.proxmoxopen.data.api.repository.NodeDiskRepositoryImpl
import de.kiefer_networks.proxmoxopen.domain.repository.NodeDiskRepository
import javax.inject.Singleton

/** Binds the read-only node-disk repository for list + SMART lookups. */
@Module
@InstallIn(SingletonComponent::class)
abstract class NodeDiskModule {
    @Binds
    @Singleton
    abstract fun bindNodeDiskRepository(impl: NodeDiskRepositoryImpl): NodeDiskRepository
}
