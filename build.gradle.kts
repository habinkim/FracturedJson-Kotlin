plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    `maven-publish`
}

group = "io.github.fracturedjson"
version = "1.0.0"
description = "FracturedJson - Human-readable JSON formatting for Kotlin"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withJavadocJar()
    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

val jacksonVersion = "2.18.2"
val coroutinesVersion = "1.9.0"
val serializationVersion = "1.7.3"

dependencies {
    // Jackson integration
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // kotlinx.serialization integration
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    // Coroutines for async formatting
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set("FracturedJson")
                description.set("Human-readable JSON formatting for Kotlin with Jackson and kotlinx.serialization support")
                url.set("https://github.com/habinkim/FracturedJson-Kotlin")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("developer")
                        name.set("Developer")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com:habinkim/FracturedJson-Kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com:habinkim/FracturedJson-Kotlin.git")
                    url.set("https://github.com/habinkim/FracturedJson-Kotlin")
                }
            }
        }
    }
}
