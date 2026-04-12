package de.kiefer_networks.proxmoxopen.di

import android.content.Context
import androidx.room.Room
import de.kiefer_networks.proxmoxopen.core.common.DefaultDispatcherProvider
import de.kiefer_networks.proxmoxopen.core.common.DispatcherProvider
import de.kiefer_networks.proxmoxopen.data.api.ProxmoxClientFactory
import de.kiefer_networks.proxmoxopen.data.db.ProxmoxDatabase
import de.kiefer_networks.proxmoxopen.data.db.dao.ServerDao
import de.kiefer_networks.proxmoxopen.data.secrets.KeystoreSecretStore
import de.kiefer_networks.proxmoxopen.data.secrets.SecretStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProxmoxDatabase =
        Room.databaseBuilder(context, ProxmoxDatabase::class.java, ProxmoxDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideServerDao(db: ProxmoxDatabase): ServerDao = db.serverDao()

    @Provides
    @Singleton
    fun provideSecretStore(@ApplicationContext context: Context): SecretStore =
        KeystoreSecretStore(context)

    @Provides
    @Singleton
    fun provideJson(): Json = ProxmoxClientFactory.DefaultJson

    @Provides
    @Singleton
    fun provideProxmoxClientFactory(json: Json): ProxmoxClientFactory = ProxmoxClientFactory(json)

    @Provides
    @Singleton
    fun provideDispatcherProvider(): DispatcherProvider = DefaultDispatcherProvider()
}
