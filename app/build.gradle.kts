plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") // Jetpack Compose compiler
    id("com.google.devtools.ksp")             // generates the Room database code
}

android {
    // Robolectric runs the touch and detection tests (AmenHoldTest, OpenCountTest)
    // on the computer, without a phone.
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { it.maxHeapSize = "3g" }
        }
    }
    namespace = "com.prayergate.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.prayergate.app"
        minSdk = 26        // Android 8.0
        targetSdk = 35     // Android 15
        versionCode = 1
        versionName = "1.0"
    }

    // Sign debug builds with a key that lives in this repository
    // (app/debug.keystore), not one generated on each computer. Every build
    // then has the same signature, so a new APK installs as an UPDATE and
    // keeps the user's data. This is a debug key with the standard public
    // password; a Play Store release needs its own private key.
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    // Only English and Korean: leave out the other languages that libraries bring along.
    androidResources {
        localeFilters += listOf("en", "ko")
    }
}

dependencies {
    // Jetpack Compose (the UI toolkit). The BOM keeps all Compose versions in sync.
    implementation(platform("androidx.compose:compose-bom:2025.04.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Room (the on-device database)
    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")

    // Unit tests (run on your computer, no phone needed)
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    testImplementation("org.json:json:20240303")
}
