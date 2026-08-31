plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "me.mrashidi.bayqush"
    compileSdk {
        version = release(37)
    }

    val keystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
    val keystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD")
    val keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS")
    val keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD")
    val hasReleaseSigning =
        keystorePath.isPresent &&
            keystorePassword.isPresent &&
            keyAlias.isPresent &&
            keyPassword.isPresent

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath.get())
                storePassword = keystorePassword.get()
                this.keyAlias = keyAlias.get()
                this.keyPassword = keyPassword.get()
            }
        }
    }

    defaultConfig {
        applicationId = "me.mrashidi.bayqush"
        minSdk = 24
        targetSdk = 36
        versionCode = providers.gradleProperty("VERSION_CODE").get().toInt()
        versionName = providers.gradleProperty("VERSION_NAME").get()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}