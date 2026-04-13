# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\rozetkaimg\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools-proguard.html

# Add any project-specific rules here that should be preserved.
# Napier
-keep class io.github.aakira.napier.** { *; }

# Keep domain and data classes to prevent NoClassDefFoundError
-keep class com.rozetka.domain.** { *; }
-keep class com.rozetka.data.** { *; }
-keep class com.rozetka.presentation.** { *; }

# Koin rules
-keepclassmembers class * {
    @org.koin.core.annotation.KoinInternalApi *;
}
