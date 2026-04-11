package app.proxmoxopen.di

import android.content.Context
import androidx.room.Room
import app.proxmoxopen.core.common.DefaultDispatcherProvider
import app.proxmoxopen.core.common.DispatcherProvider
import app.proxmoxopen.data.api.ProxmoxClientFactory
import app.proxmoxopen.data.db.ProxmoxDatabase
import app.proxmoxopen.data.db.dao.ServerDao
import app.proxmoxopen.data.secrets.KeystoreSecretStore
import app.proxmoxopen.data.secrets.SecretStore
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
