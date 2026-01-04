pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "fractured-json-kotlin"

include(":fracturedjson-core")
include(":fracturedjson-parser")
include(":fracturedjson-kotlinx")
include(":fracturedjson-jackson")
include(":fracturedjson-bom")
