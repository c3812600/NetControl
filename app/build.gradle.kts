plugins {
    alias(libs.plugins.android.application)
}

val signProps = java.util.Properties()
val signPropsFile = rootProject.file("keystore.properties")
if (signPropsFile.exists()) {
    signPropsFile.inputStream().use { signProps.load(it) }
}

// 读取顺序：环境变量 → -P 属性 → keystore.properties
val ksPathRaw: String? = (
    System.getenv("RELEASE_STORE_FILE")
        ?: (project.findProperty("RELEASE_STORE_FILE") as String?)
        ?: signProps.getProperty("storeFile")
    )?.trim()?.ifEmpty { null }
val ksPassRaw: String? = (
    System.getenv("RELEASE_STORE_PASSWORD")
        ?: (project.findProperty("RELEASE_STORE_PASSWORD") as String?)
        ?: signProps.getProperty("storePassword")
    )?.trim()?.ifEmpty { null }
val keyAliasRaw: String? = (
    System.getenv("RELEASE_KEY_ALIAS")
        ?: (project.findProperty("RELEASE_KEY_ALIAS") as String?)
        ?: signProps.getProperty("keyAlias")
    )?.trim()?.ifEmpty { null }
val keyPassRaw: String? = (
    System.getenv("RELEASE_KEY_PASSWORD")
        ?: (project.findProperty("RELEASE_KEY_PASSWORD") as String?)
        ?: signProps.getProperty("keyPassword")
    )?.trim()?.ifEmpty { null }

android {
    namespace = "com.example.netcontrol"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.c3812600.netcontrol"
        minSdk = 23
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val storePath = ksPathRaw
        val storePass = ksPassRaw
        val alias = keyAliasRaw
        val keyPass = keyPassRaw
        if (storePath != null && storePass != null && alias != null && keyPass != null && project.file(storePath).exists()) {
            create("release") {
                storeFile = project.file(storePath)
                storePassword = storePass
                this.keyAlias = alias
                this.keyPassword = keyPass
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
