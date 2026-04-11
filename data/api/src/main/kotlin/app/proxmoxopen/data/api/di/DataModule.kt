package app.proxmoxopen.data.api.di

import app.proxmoxopen.data.api.repository.AuthRepositoryImpl
import app.proxmoxopen.data.api.repository.ClusterRepositoryImpl
import app.proxmoxopen.data.api.repository.GuestRepositoryImpl
import app.proxmoxopen.data.api.repository.PowerRepositoryImpl
import app.proxmoxopen.data.api.repository.ServerRepositoryImpl
import app.proxmoxopen.data.api.repository.TaskRepositoryImpl
import app.proxmoxopen.domain.repository.AuthRepository
import app.proxmoxopen.domain.repository.ClusterRepository
import app.proxmoxopen.domain.repository.GuestRepository
import app.proxmoxopen.domain.repository.PowerRepository
import app.proxmoxopen.domain.repository.ServerRepository
import app.proxmoxopen.domain.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindClusterRepository(impl: ClusterRepositoryImpl): ClusterRepository

    @Binds @Singleton
    abstract fun bindGuestRepository(impl: GuestRepositoryImpl): GuestRepository

    @Binds @Singleton
    abstract fun bindPowerRepository(impl: PowerRepositoryImpl): PowerRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository
}
