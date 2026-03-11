# kotlinx-serialization: TomTom SDK accesses Companion objects and serializer descriptors
# via reflection-like static field access. R8 cannot see these references in the pre-compiled
# TomTom AAR bytecode and strips the fields. Keep the entire public API surface.
-keep class kotlinx.serialization.** { *; }

# Belt-and-suspenders: explicitly keep the static Companion field in all JSON classes.
# R8 full mode (Proguard_compatibility=false) can devirtualize Kotlin companion object
# accesses and remove the Companion field even with a broad -keep rule. The pre-compiled
# TomTom AARs access JsonObject.Companion via getstatic bytecode instructions that R8
# treats as "unreachable" during its reachability analysis of opaque prebuilt code.
-keepclassmembers class kotlinx.serialization.json.** {
    public static final ** Companion;
}
# Also keep the Companion field on all @Serializable classes (matches TomTom's own rule).
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
