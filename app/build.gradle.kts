plugins {
    alias(libs.plugins.android.application)
}

val releaseSignProps = java.util.Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) {
        f.inputStream().use { load(it) }
    }
}

val releaseStoreFile = listOfNotNull(
    System.getenv("RELEASE_STORE_FILE"),
    project.findProperty("RELEASE_STORE_FILE") as String?,
    releaseSignProps.getProperty("storeFile")
).map { it.trim() }.firstOrNull { it.isNotEmpty() }

val releaseStorePassword = listOfNotNull(
    System.getenv("RELEASE_STORE_PASSWORD"),
    project.findProperty("RELEASE_STORE_PASSWORD") as String?,
    releaseSignProps.getProperty("storePassword")
).map { it.trim() }.firstOrNull { it.isNotEmpty() }

val releaseKeyAlias = listOfNotNull(
    System.getenv("RELEASE_KEY_ALIAS"),
    project.findProperty("RELEASE_KEY_ALIAS") as String?,
    releaseSignProps.getProperty("keyAlias")
).map { it.trim() }.firstOrNull { it.isNotEmpty() }

val releaseKeyPassword = listOfNotNull(
    System.getenv("RELEASE_KEY_PASSWORD"),
    project.findProperty("RELEASE_KEY_PASSWORD") as String?,
    releaseSignProps.getProperty("keyPassword")
).map { it.trim() }.firstOrNull { it.isNotEmpty() }

val canSignRelease = releaseStoreFile != null
        && project.file(releaseStoreFile).exists()
        && releaseStorePassword != null
        && releaseKeyAlias != null
        && releaseKeyPassword != null

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

    signingConfigs {
        if (canSignRelease) {
            create("release") {
                storeFile = project.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
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
            if (canSignRelease) {
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
