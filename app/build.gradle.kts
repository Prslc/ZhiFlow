@file:Suppress("ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING")

import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    require(file.exists()) {
        """
        keystore.properties is missing. Create ${file.path} with:
            storeFile=/path/to/zhiflow.jks
            storePassword=...
            keyAlias=...
            keyPassword=...
        """.trimIndent()
    }
    file.inputStream().use { load(it) }
    val missing = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        .filter { getProperty(it).isNullOrBlank() }
    require(missing.isEmpty()) {
        "keystore.properties is missing ${missing.joinToString(", ")}"
    }
}

@file:Suppress("UnstableApiUsage")
android {
    namespace = "com.prslc.zhiflow"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.prslc.zhiflow"
        minSdk = 33
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("storeFile"))
            storePassword = keystoreProperties.getProperty("storePassword")
            keyAlias = keystoreProperties.getProperty("keyAlias")
            keyPassword = keystoreProperties.getProperty("keyPassword")

            enableV1Signing = false
            enableV2Signing = true
            enableV3Signing = false
            enableV4Signing = false
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = true
            vcsInfo.include = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            packaging {
                resources { excludes += "**" }
            }
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    android.sourceSets.named("main") {
        jniLibs.directories += "libs"
    }
}

// https://stackoverflow.com/a/77745844
tasks.withType<PackageAndroidArtifact> {
    doFirst { appMetadata.asFile.orNull?.writeText("") }
}

dependencies {
    lintChecks(libs.compose.lint)

    implementation(libs.androidx.core.ktx)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // okhttp
    implementation(libs.okhttp.core)

    // Koin
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    // Utils
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)
    implementation(libs.coil.network.okhttp)
    implementation(libs.telephoto.zoomable.image.coil)
    implementation(libs.highlights)
}