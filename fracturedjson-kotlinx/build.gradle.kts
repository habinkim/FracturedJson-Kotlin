plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

description = "FracturedJson Kotlinx - kotlinx.serialization adapter"

dependencies {
    // Core 모듈 의존
    api(project(":fracturedjson-core"))

    // kotlinx.serialization
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // Coroutines for async formatting
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // 테스트 의존성
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
