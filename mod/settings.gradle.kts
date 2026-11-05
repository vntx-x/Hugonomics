pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases")
    }
    plugins {
        id("fabric-loom") version "1.17.21"
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        versions("1.21.5", "1.21.8", "1.21.10", "1.21.11")
        vcsVersion = "1.21.5"
    }
}

rootProject.name = "hugomarket"
