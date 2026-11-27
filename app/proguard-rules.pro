# Pertahankan model data & struktur JSON
-keep class com.udyne.notecash.Tx { *; }
-keep class com.udyne.notecash.Goal { *; }
-keep class com.udyne.notecash.SyncBundle { *; }
-keep class com.udyne.notecash.Sum { *; }

# org.json & java.time
-keep class org.json.** { *; }
-dontwarn java.time.**

# Compose & Kotlin reflection/coroutines metadata
-keepattributes SourceFile,LineNumberTable
-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# BiometricPrompt memakai fragment & callback; aturan consumer dari library sudah cukup.
