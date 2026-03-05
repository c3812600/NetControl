plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.netcontrol"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.netcontrol"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val ksPath = (System.getenv("RELEASE_STORE_FILE") ?: (project.findProperty("RELEASE_STORE_FILE") as String?))?.ifBlank { null }
    val ksPass = (System.getenv("RELEASE_STORE_PASSWORD") ?: (project.findProperty("RELEASE_STORE_PASSWORD") as String?))?.ifBlank { null }
    val keyAlias = (System.getenv("RELEASE_KEY_ALIAS") ?: (project.findProperty("RELEASE_KEY_ALIAS") as String?))?.ifBlank { null }
    val keyPass = (System.getenv("RELEASE_KEY_PASSWORD") ?: (project.findProperty("RELEASE_KEY_PASSWORD") as String?))?.ifBlank { null }

    signingConfigs {
        if (ksPath != null && ksPass != null && keyAlias != null && keyPass != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = ksPass
                this.keyAlias = keyAlias
                keyPassword = keyPass
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
                enableV4Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (signingConfigs.findByName("release") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("org.nanohttpd:nanohttpd:2.3.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
