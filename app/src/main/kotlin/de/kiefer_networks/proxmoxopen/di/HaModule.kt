package de.kiefer_networks.proxmoxopen.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.kiefer_networks.proxmoxopen.data.api.repository.HaRepositoryImpl
import de.kiefer_networks.proxmoxopen.domain.repository.HaRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HaModule {
    @Binds
    @Singleton
    abstract fun bindHaRepository(impl: HaRepositoryImpl): HaRepository
}
