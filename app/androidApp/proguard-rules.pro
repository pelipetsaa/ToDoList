# R8 rules for the release build. Libraries (Ktor, kotlinx.serialization, Koin, Compose) ship their own
# consumer rules; only what they do not cover lives here.

# Keep line numbers so crash reports from release builds point at real code; hide the source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Credential Manager loads its Play Services implementation by reflection.
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }

# Ktor references JVM-only classes that do not exist on Android.
-dontwarn java.lang.management.**
-dontwarn org.slf4j.impl.StaticLoggerBinder
