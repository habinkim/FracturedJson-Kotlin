plugins {
    kotlin("jvm")
}

description = "FracturedJson Parser - JSON tokenizer and parser with comment support"

dependencies {
    // Core 모듈 의존
    api(project(":fracturedjson-core"))

    // 테스트 의존성
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
