# Add project specific ProGuard rules here.

# Keep data classes for Room
-keep class com.moneytracker.simplebudget.data.local.entity.** { *; }

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.lifecycle.HiltViewModel { *; }

# iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**

# Apache POI OOXML
-keep class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-dontwarn com.fasterxml.aalto.**
-dontwarn java.awt.**
-dontwarn org.apache.logging.**
-dontwarn org.apache.log4j.**

# Google Play Billing
-keep class com.android.vending.billing.**

# Google Play In-App Update
# The library's internal classes use IPC to communicate with the Play Store app.
# R8 must not rename them — the Play Store side binds to these class names exactly.
-keep class com.google.android.play.core.** { *; }

# AdMob
-keep class com.google.android.gms.ads.** { *; }

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.** { *; }

# Suppress missing SLF4J implementation warning
-dontwarn org.slf4j.impl.StaticLoggerBinder
