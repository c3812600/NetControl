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
        // 商店包名不可使用 com.example.*（小米等商店会拒）
        applicationId = "com.c3812600.netcontrol"
        minSdk = 23
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreProps = java.util.Properties()
    val keystorePropsFile = rootProject.file("keystore.properties")
    if (keystorePropsFile.exists()) {
        keystorePropsFile.inputStream().use { keystoreProps.load(it) }
    }
    fun signProp(envKey: String, propKey: String): String? {
        val fromEnv = System.getenv(envKey)?.ifBlank { null }
        if (fromEnv != null) return fromEnv
        val fromGradle = (project.findProperty(envKey) as String?)?.ifBlank { null }
        if (fromGradle != null) return fromGradle
        return keystoreProps.getProperty(propKey)?.ifBlank { null }
    }

    val ksPath = signProp("RELEASE_STORE_FILE", "storeFile")
    val ksPass = signProp("RELEASE_STORE_PASSWORD", "storePassword")
    val keyAlias = signProp("RELEASE_KEY_ALIAS", "keyAlias")
    val keyPass = signProp("RELEASE_KEY_PASSWORD", "keyPassword")

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
