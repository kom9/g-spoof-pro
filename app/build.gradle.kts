import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Google Maps API key: put MAPS_API_KEY=... in local.properties (never commit it)
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val mapsApiKey: String =
    (localProps.getProperty("MAPS_API_KEY") ?: System.getenv("MAPS_API_KEY") ?: "").trim()

android {
    namespace = "com.gspoof.pro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.gspoof.pro"
        minSdk = 26
        targetSdk = 34
        versionCode = 13
        versionName = "2.2"

        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
    }

    val ciStore = System.getenv("KEYSTORE_FILE")
    val ciReady = ciStore != null && file(ciStore).exists()

    signingConfigs {
        if (ciReady) {
            create("ci") {
                storeFile = file(ciStore!!)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "android"
                keyAlias = System.getenv("KEY_ALIAS") ?: "androiddebugkey"
                keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (ciReady) signingConfig = signingConfigs.getByName("ci")
        }
        getByName("release") {
            isMinifyEnabled = false
            if (ciReady) signingConfig = signingConfigs.getByName("ci")
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
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    lint {
        // App ini memang aplikasi mock location — lint default menganggap
        // izin ACCESS_MOCK_LOCATION di manifest utama sebagai error di release.
        disable += "MockLocation"
        abortOnError = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
