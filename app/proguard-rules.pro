# Add project specific ProGuard rules here.

# Security & Anti-tampering: Obfuscate SecurityConfig class & internal methods while retaining runtime access
-keep,allowobfuscation class com.sena.financetracker.util.SecurityConfig {
    public static java.lang.String getGroqApiKey();
    public static java.lang.String deobfuscate(byte[], byte);
}

