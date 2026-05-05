# 1. Performance: Allow shrinking but protect the core game loop
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''

# 2. Level Loading Protection (JSON Reflection)
# This keeps the fields in LevelData and EnemyData exactly as named 
# so the JSON parser can find them.
-keepclassmembers class com.glitchhollow.LevelData { *; }
-keepclassmembers class com.glitchhollow.LevelData$EnemyData { *; }

# 3. Android Hardware & UI Safety
-keep public class * extends android.app.Activity
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# 4. Remove Log Messages in Release
# This makes the game faster by removing the overhead of Log.d calls
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# 5. Metadata for Crash Reporting
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
