# kotlinx.serialization and Retrofit ship their own consumer rules; keep the API models' serializers.
-keep,includedescriptorclasses class com.smartstock.app.data.**$$serializer { *; }
-keepclassmembers class com.smartstock.app.data.** {
    *** Companion;
}

# ML Kit finds its registrars by the class names in manifest metadata, which R8 cannot see.
-keep class * implements com.google.firebase.components.ComponentRegistrar { <init>(); }
