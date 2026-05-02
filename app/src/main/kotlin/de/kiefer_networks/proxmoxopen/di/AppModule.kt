package de.kiefer_networks.proxmoxopen.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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

    /**
     * Schema v1 -> v2 adds the `cached_cluster_resource` table (offline cluster cache).
     * v1 contained only the `servers` table, so the migration is purely additive.
     * The CREATE statement mirrors the entity definition in
     * [de.kiefer_networks.proxmoxopen.data.db.entity.CachedClusterResourceEntity] and
     * matches what Room would generate for it.
     */
    private val MIGRATION_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "CREATE TABLE IF NOT EXISTS `cached_cluster_resource` (" +
                    "`serverId` INTEGER NOT NULL, " +
                    "`type` TEXT NOT NULL, " +
                    "`node` TEXT NOT NULL, " +
                    "`vmid` INTEGER NOT NULL, " +
                    "`name` TEXT, " +
                    "`status` TEXT, " +
                    "`cpu` REAL, " +
                    "`mem` INTEGER, " +
                    "`maxmem` INTEGER, " +
                    "`diskUsed` INTEGER, " +
                    "`maxdisk` INTEGER, " +
                    "`tags` TEXT, " +
                    "`capturedAt` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`serverId`, `type`, `vmid`, `node`)" +
                    ")",
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ProxmoxDatabase {
        val dbKey = getOrCreateDatabaseKey(context)
        val factory = net.zetetic.database.sqlcipher.SupportOpenHelperFactory(dbKey)
        // NOTE: MIGRATION_1_2 was verified against the emitted schemas/2.json (cached_cluster_resource createSql).
        return Room.databaseBuilder(context, ProxmoxDatabase::class.java, ProxmoxDatabase.DATABASE_NAME)
            .openHelperFactory(factory)
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    private fun getOrCreateDatabaseKey(context: Context): ByteArray {
        val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val alias = "proxmoxopen_db_encryption_key_v2"

        val prefs = context.getSharedPreferences("db_key_prefs", Context.MODE_PRIVATE)
        // v2 key is bound to user authentication and stored under a new pref key so it
        // does not collide with any v1 blob written by a previous install. v1 entries
        // are intentionally not migrated (one-time reset; user re-adds servers).
        val existingEncrypted = prefs.getString("encrypted_db_key_v2", null)

        if (existingEncrypted != null && keyStore.containsAlias(alias)) {
            // Decrypt existing key
            try {
                val aad = alias.toByteArray(Charsets.UTF_8)
                val isV2 = existingEncrypted.startsWith("v2.")
                val payload = if (isV2) existingEncrypted.removePrefix("v2.") else existingEncrypted
                val parts = payload.split(".")
                val iv = android.util.Base64.decode(parts[0], android.util.Base64.NO_WRAP)
                val encrypted = android.util.Base64.decode(parts[1], android.util.Base64.NO_WRAP)
                val secretKey = (keyStore.getEntry(alias, null) as java.security.KeyStore.SecretKeyEntry).secretKey
                val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(javax.crypto.Cipher.DECRYPT_MODE, secretKey, javax.crypto.spec.GCMParameterSpec(128, iv))
                if (isV2) {
                    cipher.updateAAD(aad)
                }
                val dbKey = cipher.doFinal(encrypted)
                if (!isV2) {
                    // Silently migrate legacy v1 blob to v2 with AAD.
                    try {
                        val reCipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
                        reCipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
                        reCipher.updateAAD(aad)
                        val reCt = reCipher.doFinal(dbKey)
                        val reIv = reCipher.iv
                        val reStored = "v2." +
                            android.util.Base64.encodeToString(reIv, android.util.Base64.NO_WRAP) + "." +
                            android.util.Base64.encodeToString(reCt, android.util.Base64.NO_WRAP)
                        prefs.edit().putString("encrypted_db_key_v2", reStored).apply()
                    } catch (_: Exception) {
                        // Migration failure is non-fatal; v1 blob remains usable.
                    }
                }
                return dbKey
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
            val specBuilder = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setUserAuthenticationParameters(
                    /* timeoutSeconds = */ 300,
                    KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                )
                .setInvalidatedByBiometricEnrollment(true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val pm = context.packageManager
                if (pm.hasSystemFeature(android.content.pm.PackageManager.FEATURE_STRONGBOX_KEYSTORE)) {
                    specBuilder.setIsStrongBoxBacked(true)
                }
            }
            try {
                keyGen.init(specBuilder.build())
                keyGen.generateKey()
            } catch (_: java.security.ProviderException) {
                // StrongBox may be advertised but unavailable; retry without it.
                val fallback = KeyGenParameterSpec.Builder(
                    alias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .setRandomizedEncryptionRequired(true)
                    .setUserAuthenticationRequired(true)
                    .setUserAuthenticationParameters(
                        /* timeoutSeconds = */ 300,
                        KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL,
                    )
                    .setInvalidatedByBiometricEnrollment(true)
                    .build()
                keyGen.init(fallback)
                keyGen.generateKey()
            }
        }

        // Encrypt db key with AndroidKeyStore key
        val secretKey = (keyStore.getEntry(alias, null) as java.security.KeyStore.SecretKeyEntry).secretKey
        val cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)
        cipher.updateAAD(alias.toByteArray(Charsets.UTF_8))
        val encryptedDbKey = cipher.doFinal(dbKey)
        val iv = cipher.iv
        val stored = "v2." +
            android.util.Base64.encodeToString(iv, android.util.Base64.NO_WRAP) + "." +
            android.util.Base64.encodeToString(encryptedDbKey, android.util.Base64.NO_WRAP)
        prefs.edit().putString("encrypted_db_key_v2", stored).apply()

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
