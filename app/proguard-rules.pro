# ── Kotlin ────────────────────────────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }
-keepclassmembers class kotlin.Lazy { *; }

# ── Android OpenGL ────────────────────────────────────────────────
# GL classes are loaded by name at runtime — must not be renamed
-keep class android.opengl.** { *; }
-keep class javax.microedition.khronos.** { *; }

# ── Our GL renderer (GLSurfaceView.Renderer) ─────────────────────
-keep class com.glitchhollow.gl.GLRenderer { *; }
-keep class com.glitchhollow.gl.GLGameView { *; }

# ── Game core ─────────────────────────────────────────────────────
-keep class com.glitchhollow.core.** { *; }

# ── Screen system ─────────────────────────────────────────────────
-keep interface com.glitchhollow.screen.Screen { *; }
-keep class com.glitchhollow.screen.** { *; }

# ── JSON parsing (org.json built-in — no rules needed) ────────────

# ── MediaPlayer + SoundPool ───────────────────────────────────────
-keep class android.media.** { *; }

# ── SharedPreferences ─────────────────────────────────────────────
-keepclassmembers class * {
    @android.content.SharedPreferences *;
}

# ── Remove logging in release ─────────────────────────────────────
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}

# ── Suppress common warnings ──────────────────────────────────────
-dontwarn java.lang.invoke.**
-dontwarn **$$serializer