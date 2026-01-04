plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.serialization") version "2.0.21" apply false
    `maven-publish`
}

allprojects {
    group = "io.github.fracturedjson"
    version = "1.0.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    // BOM 모듈은 java-platform을 사용하므로 제외
    if (name != "fracturedjson-bom") {
        apply(plugin = "org.jetbrains.kotlin.jvm")
        apply(plugin = "maven-publish")

        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21))
            }
            withJavadocJar()
            withSourcesJar()
        }

        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                freeCompilerArgs.addAll(
                    "-Xjsr305=strict",
                    "-opt-in=kotlin.RequiresOptIn"
                )
            }
        }

        tasks.withType<Test> {
            useJUnitPlatform()
        }

        configure<PublishingExtension> {
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])

                    pom {
                        name.set(project.name)
                        description.set("FracturedJson Kotlin - Human-readable JSON formatting")
                        url.set("https://github.com/user/fractured-json-kotlin")

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
                            connection.set("scm:git:git://github.com/user/fractured-json-kotlin.git")
                            developerConnection.set("scm:git:ssh://github.com/user/fractured-json-kotlin.git")
                            url.set("https://github.com/user/fractured-json-kotlin")
                        }
                    }
                }
            }
        }
    }
}
