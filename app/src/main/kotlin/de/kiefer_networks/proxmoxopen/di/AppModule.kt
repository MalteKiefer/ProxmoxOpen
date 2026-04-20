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
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProxmoxDatabase {
        val dbKey = getOrCreateDatabaseKey(context)
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(dbKey)
        return Room.databaseBuilder(context, ProxmoxDatabase::class.java, ProxmoxDatabase.DATABASE_NAME)
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private fun getOrCreateDatabaseKey(context: Context): ByteArray {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "proxmoxopen_db_encryption_key"

        val prefs = context.getSharedPreferences("db_key_prefs", Context.MODE_PRIVATE)
        val existingEncrypted = prefs.getString("encrypted_db_key", null)

        if (existingEncrypted != null && keyStore.containsAlias(alias)) {
            // Decrypt existing key
            try {
                val parts = existingEncrypted.split(".")
                val iv = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)
                val encrypted = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
                val secretKey = (keyStore.getEntry(alias, null) as java.security.KeyStore.SecretKeyEntry).secretKey
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))
                return cipher.doFinal(encrypted)
            } catch (_: Exception) {
                // Key corrupted, regenerate
            }
        }

        // Generate new database key
        val dbKey = ByteArray(32).also { java.security.SecureRandom().nextBytes(it) }

        // Create or get AndroidKeyStore wrapping key
        if (!keyStore.containsAlias(alias)) {
            val keyGen = javax.crypto.KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
            )
            keyGen.init(
                KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
            )
            keyGen.generateKey()
        }

        // Encrypt db key with AndroidKeyStore key
        val secretKey = (keyStore.getEntry(alias, null) as java.security.KeyStore.SecretKeyEntry).secretKey
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
        val encryptedDbKey = cipher.doFinal(dbKey)
        val iv = cipher.iv
        val stored = android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP) + "." +
            android.util.Base64.encodeToString(encryptedDbKey, android.util.Base64.NO_WRAP)
        prefs.edit().putString("encrypted_db_key", stored).apply()

        return dbKey
    }

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
