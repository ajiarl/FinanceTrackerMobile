# Add project specific ProGuard rules here.

# Security & Anti-tampering: Obfuscate SecurityConfig class & internal methods while retaining runtime access
-keep,allowobfuscation class com.sena.financetracker.util.SecurityConfig {
    public static java.lang.String getGroqApiKey();
    public static java.lang.String getGroqApiKey(android.content.Context);
    public static java.lang.String deobfuscate(byte[], byte);
}

# Jetpack Security / ApiKeyStorage keep rules
-keep,allowobfuscation class com.sena.financetracker.security.ApiKeyStorage {
    public static java.lang.String getGroqApiKey(android.content.Context);
    public static java.lang.String getGroqApiKey();
    public static void setGroqApiKey(android.content.Context, java.lang.String);
    public static void clearGroqApiKey(android.content.Context);
    public static boolean hasCustomApiKey(android.content.Context);
}
