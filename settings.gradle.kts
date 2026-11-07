@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io/")
        maven("https://api.xposed.info/")
        maven("https://maven.aliyun.com/repository/public")
    }
}

rootProject.name = "HyperVisage"

include(":app")
include(":hyperx-compose")
