plugins {
    kotlin("jvm")
}

description = "FracturedJson Jackson - Jackson adapter"

val jacksonVersion = "2.18.2"

dependencies {
    // Core 모듈 의존
    api(project(":fracturedjson-core"))

    // Jackson
    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // 테스트 의존성
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
