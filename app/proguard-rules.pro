# ---------- kotlinx.serialization ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep `Companion` objects of @Serializable classes.
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep,includedescriptorclasses class de.kiefer_networks.proxmoxopen.**$$serializer { *; }
-keepclassmembers class de.kiefer_networks.proxmoxopen.** {
    *** Companion;
}
-keepclasseswithmembers class de.kiefer_networks.proxmoxopen.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Ktor / OkHttp / SLF4J ----------
-dontwarn io.ktor.**
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
-dontwarn org.slf4j.**
-dontwarn org.slf4j.impl.**

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------- Hilt / Dagger ----------
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-dontwarn dagger.hilt.internal.**

# ---------- SQLCipher ----------
-keep class net.zetetic.database.** { *; }
-keep interface net.zetetic.database.** { *; }
-dontwarn net.zetetic.database.**

# ---------- App types ----------
-keep class de.kiefer_networks.proxmoxopen.data.api.dto.** { *; }
-keep class de.kiefer_networks.proxmoxopen.domain.model.** { *; }
