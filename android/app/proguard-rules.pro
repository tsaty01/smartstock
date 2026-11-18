# kotlinx.serialization and Retrofit ship their own consumer rules; keep the API models' serializers.
-keep,includedescriptorclasses class com.smartstock.app.data.**$$serializer { *; }
-keepclassmembers class com.smartstock.app.data.** {
    *** Companion;
}
