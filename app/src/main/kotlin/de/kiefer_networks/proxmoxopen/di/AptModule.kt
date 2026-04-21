package de.kiefer_networks.proxmoxopen.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kiefer_networks.proxmoxopen.data.api.repository.AptRepositoryImpl
import de.kiefer_networks.proxmoxopen.domain.repository.AptRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AptModule {

    @Binds
    @Singleton
    abstract fun bindAptRepository(impl: AptRepositoryImpl): AptRepository
}
