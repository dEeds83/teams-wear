import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
}

// Read secrets/config from local.properties (NOT committed). Falls back to
// placeholders so the project still compiles in CI without real values.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun cfg(key: String, default: String): String =
    (localProps.getProperty(key) ?: System.getenv(key) ?: default)

android {
    namespace = "de.streamonkey.teamswear"
    compileSdk = 34

    defaultConfig {
        applicationId = "de.streamonkey.teamswear"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Azure AD app registration (public client, device-code enabled).
        // "common" lets any work/school tenant sign in (multi-tenant).
        buildConfigField("String", "AZURE_CLIENT_ID", "\"${cfg("AZURE_CLIENT_ID", "00000000-0000-0000-0000-000000000000")}\"")
        buildConfigField("String", "AZURE_TENANT", "\"${cfg("AZURE_TENANT", "common")}\"")
        // Relay-Server-URL fuer Push-Benachrichtigungen (Phase 2). Leer = Push deaktiviert.
        buildConfigField("String", "RELAY_BASE_URL", "\"${cfg("RELAY_BASE_URL", "")}\"")
    }

    // Release-Signing nur konfigurieren, wenn ein Keystore hinterlegt ist
    // (Werte aus local.properties; Datei selbst ist gitignored).
    val releaseStoreFile = cfg("RELEASE_STORE_FILE", "")
    val hasReleaseKeystore = releaseStoreFile.isNotBlank() &&
        rootProject.file(releaseStoreFile).exists()

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = cfg("RELEASE_STORE_PASSWORD", "")
                keyAlias = cfg("RELEASE_KEY_ALIAS", "")
                keyPassword = cfg("RELEASE_KEY_PASSWORD", "")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseKeystore) signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Compose for Wear OS
    implementation("androidx.wear.compose:compose-material:1.3.1")
    implementation("androidx.wear.compose:compose-foundation:1.3.1")
    implementation("androidx.wear.compose:compose-navigation:1.3.1")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Wear RemoteInput (Voice / Keyboard / Quick-Reply Chooser)
    implementation("androidx.wear:wear-input:1.1.0")

    // Bild-Anzeige in Chats (inkl. animierte GIF/WebP)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.coil-kt:coil-gif:2.6.0")

    // Horologist (Wear helpers)
    implementation("com.google.android.horologist:horologist-compose-layout:0.6.17")

    // Lifecycle / coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Networking: Retrofit + OkHttp + kotlinx.serialization
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Secure token storage + cache
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager (token refresh / catch-up)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    ksp("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Firebase Cloud Messaging (Push). Kompiliert ohne google-services.json;
    // FCM bleibt zur Laufzeit inaktiv bis google-services.json + Plugin da sind.
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-messaging:24.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
