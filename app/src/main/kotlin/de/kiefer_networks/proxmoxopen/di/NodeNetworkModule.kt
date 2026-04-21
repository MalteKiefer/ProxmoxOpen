package de.kiefer_networks.proxmoxopen.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kiefer_networks.proxmoxopen.data.api.repository.NodeNetworkRepositoryImpl
import de.kiefer_networks.proxmoxopen.domain.repository.NodeNetworkRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NodeNetworkModule {
    @Binds
    @Singleton
    abstract fun bindNodeNetworkRepository(
        impl: NodeNetworkRepositoryImpl,
    ): NodeNetworkRepository
}
