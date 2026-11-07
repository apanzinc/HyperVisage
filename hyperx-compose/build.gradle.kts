@file:Suppress("UseTomlInstead")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("kotlin-parcelize")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}

android {
    namespace = "dev.lackluster.hyperx"
    compileSdk = 37

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    defaultConfig {
        minSdk = 33
        consumerProguardFiles("consumer-rules.pro")
    }
    buildFeatures {
        compose = true
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    api("top.yukonga.miuix.kmp:miuix-ui:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-preference:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-icons:0.9.4")
    api("top.yukonga.miuix.kmp:miuix-blur:0.9.4")
    api("androidx.compose.foundation:foundation:1.11.3")
    api("androidx.activity:activity-compose:1.13.0")
    // 0.9.4 起 miuix 不再发布这个 artifact，但 0.9.0 的 fork 仍然可用：
    // 它提供 NavDisplayTransitionEffects（切换圆角裁剪 / 背后变暗 / 拦截输入 / 跟手返回方向）。
    // 只额外依赖 miuix-shapes:0.9.0，与 0.9.4 的其他模块不冲突；
    // 它会打包 androidx.navigation3.ui / scene 下的类，因此不能再同时引入官方 navigation3-ui。
    api("top.yukonga.miuix.kmp:miuix-navigation3-ui:0.9.0")
    api("androidx.navigation3:navigation3-runtime:1.1.3")
    api("org.jetbrains.androidx.navigationevent:navigationevent-compose:1.1.0")
    api("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("com.github.promeg:tinypinyin:2.0.3") // maven("https://maven.aliyun.com/repository/public")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
}