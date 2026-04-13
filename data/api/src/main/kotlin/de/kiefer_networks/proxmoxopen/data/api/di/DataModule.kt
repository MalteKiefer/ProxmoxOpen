package de.kiefer_networks.proxmoxopen.data.api.di

import de.kiefer_networks.proxmoxopen.data.api.repository.AuthRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.BackupJobRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.ClusterRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.ConsoleRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.GuestRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.PowerRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.ServerRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.StorageRepositoryImpl
import de.kiefer_networks.proxmoxopen.data.api.repository.TaskRepositoryImpl
import de.kiefer_networks.proxmoxopen.domain.repository.AuthRepository
import de.kiefer_networks.proxmoxopen.domain.repository.BackupJobRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ClusterRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ConsoleRepository
import de.kiefer_networks.proxmoxopen.domain.repository.GuestRepository
import de.kiefer_networks.proxmoxopen.domain.repository.PowerRepository
import de.kiefer_networks.proxmoxopen.domain.repository.ServerRepository
import de.kiefer_networks.proxmoxopen.domain.repository.StorageRepository
import de.kiefer_networks.proxmoxopen.domain.repository.TaskRepository
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
    abstract fun bindBackupJobRepository(impl: BackupJobRepositoryImpl): BackupJobRepository

    @Binds @Singleton
    abstract fun bindClusterRepository(impl: ClusterRepositoryImpl): ClusterRepository

    @Binds @Singleton
    abstract fun bindConsoleRepository(impl: ConsoleRepositoryImpl): ConsoleRepository

    @Binds @Singleton
    abstract fun bindGuestRepository(impl: GuestRepositoryImpl): GuestRepository

    @Binds @Singleton
    abstract fun bindPowerRepository(impl: PowerRepositoryImpl): PowerRepository

    @Binds @Singleton
    abstract fun bindTaskRepository(impl: TaskRepositoryImpl): TaskRepository

    @Binds @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository
}
