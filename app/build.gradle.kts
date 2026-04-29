@file:Suppress("ANNOTATION_WITH_USE_SITE_TARGET_ON_EXPRESSION_WARNING")

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

val rawCookie = localProperties.getProperty("cookie") ?: ""
val ua = localProperties.getProperty("ua") ?: ""
val xZse96 = localProperties.getProperty("x_zse_96") ?: ""
val xZse93 = localProperties.getProperty("x_zse_93") ?: ""
val authorization = localProperties.getProperty("authorization") ?: ""
val escapedCookie = rawCookie.replace("\"", "\\\"")

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

        buildConfigField("String", "cookie", "\"$escapedCookie\"")
        buildConfigField("String", "ua", "\"$ua\"")
        buildConfigField("String", "x_zse_96", "\"$xZse96\"")
        buildConfigField("String", "x_zse_93", "\"$xZse93\"")
        buildConfigField("String", "authorization", "\"$authorization\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            multiDexEnabled = true
            vcsInfo.include = false
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            isDebuggable = true
            isMinifyEnabled = false
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
}

dependencies {
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
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.androidx.navigation.compose)

    // okhttp
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)

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

    // latex
    implementation(libs.latex.renderer)
}