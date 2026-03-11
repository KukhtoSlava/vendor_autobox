# kotlinx-serialization 1.7.3: keep Companion objects and serializer descriptors.
# TomTom SDK accesses these via static field references in pre-compiled AAR bytecode.
# R8 cannot trace the references through opaque prebuilt classes and strips the fields.
#
# NOTE: This file is NOT automatically picked up by Soong's java_import module type —
# Import.GenerateAndroidBuildActions ignores optimize.proguard_flags_files.
# These rules are replicated in the android_app's proguard-rules.pro instead.
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    public static final ** Companion;
}
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
