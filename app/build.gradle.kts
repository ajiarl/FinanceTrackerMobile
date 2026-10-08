import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val groqApiKey: String = localProperties.getProperty("GROQ_API_KEY") ?: ""

// Obfuscate GROQ_API_KEY into XOR masked array
val xorSalt: Byte = 0x5A
val groqKeyBytes = groqApiKey.toByteArray(Charsets.UTF_8)
val maskedBytes = groqKeyBytes.map { (it.toInt() xor xorSalt.toInt()).toByte() }
val maskedBytesLiteral = "new byte[] { " + maskedBytes.joinToString(", ") { "(byte) $it" } + " }"

android {
    namespace = "com.sena.financetracker"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.sena.financetracker"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        buildConfigField("byte[]", "GROQ_KEY_MASKED", maskedBytesLiteral)
        buildConfigField("byte", "GROQ_KEY_SALT", "(byte) $xorSalt")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.ui.tooling)
}
