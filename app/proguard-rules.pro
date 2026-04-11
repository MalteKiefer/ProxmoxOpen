# ---------- kotlinx.serialization ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# Keep `Companion` objects of @Serializable classes.
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keep,includedescriptorclasses class app.proxmoxopen.**$$serializer { *; }
-keepclassmembers class app.proxmoxopen.** {
    *** Companion;
}
-keepclasseswithmembers class app.proxmoxopen.** {
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

# ---------- App types ----------
-keep class app.proxmoxopen.data.api.dto.** { *; }
-keep class app.proxmoxopen.domain.model.** { *; }
