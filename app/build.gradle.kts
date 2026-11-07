import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.text.SimpleDateFormat

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget("21")
        freeCompilerArgs.addAll(
            listOf(
                "-Xno-param-assertions",
                "-Xno-call-assertions",
                "-Xno-receiver-assertions",
                "-Xannotation-default-target=param-property"
            )
        )
    }
}

android {
    namespace = libs.versions.project.app.packageName.get()
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = libs.versions.project.app.packageName.get()
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionName = libs.versions.project.app.versionName.get()
        versionCode = libs.versions.project.app.versionCode.get().toInt()
        buildConfigField(
            "String",
            "BUILD_TIME",
            "\"" + SimpleDateFormat("yyyyMMddHHmmss").format(System.currentTimeMillis()) + "\""
        )
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    androidResources {
        // 与同进程内的其他 Xposed 模块共享资源时避免资源 ID 冲突
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x61")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("preview") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
        }
    }

    lint {
        checkReleaseBuilds = false
    }
}

dependencies {
    // Xposed 运行时由框架提供，编译期只需要 API
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)

    implementation(libs.hiddenapibypass)
    implementation(libs.kavaref.core)
    implementation(libs.kavaref.android)
    implementation(libs.kavaref.extension)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.koin.androidx.compose)
    implementation(libs.kotlinx.serialization.json)

    implementation(project(":hyperx-compose"))
}
