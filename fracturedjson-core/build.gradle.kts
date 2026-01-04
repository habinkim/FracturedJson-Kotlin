plugins {
    kotlin("jvm")
}

description = "FracturedJson Core - JSON formatting engine"

dependencies {
    // 순수 Kotlin - 외부 의존성 없음

    // 테스트 의존성
    testImplementation(project(":fracturedjson-parser"))
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
