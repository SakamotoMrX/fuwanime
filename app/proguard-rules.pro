# Keep line numbers so crash stack traces stay readable.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Moshi models use generated adapters (@JsonClass(generateAdapter = true));
# keep model class names so the generated *JsonAdapter lookups resolve.
-keep class com.fuwanime.data.remote.model.** { *; }
-keep class com.example.data.model.** { *; }

# Retrofit service interfaces are accessed via reflection.
-keep,allowobfuscation interface com.fuwanime.data.remote.AnimeApiService
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
