import java.security.MessageDigest

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    `maven-publish`
    signing
}

group = "io.github.habinkim"
version = "0.5.5"
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
val gsonVersion = "2.11.0"
val coroutinesVersion = "1.9.0"
val serializationVersion = "1.7.3"

dependencies {
    // Jackson integration
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")

    // kotlinx.serialization integration
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    // Gson integration
    implementation("com.google.code.gson:gson:$gsonVersion")

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
                name.set("FracturedJson-Kotlin")
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
                        id.set("habinkim")
                        name.set("Habin Kim")
                        url.set("https://github.com/habinkim")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/habinkim/FracturedJson-Kotlin.git")
                    developerConnection.set("scm:git:ssh://github.com/habinkim/FracturedJson-Kotlin.git")
                    url.set("https://github.com/habinkim/FracturedJson-Kotlin")
                }
            }
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project

    if (signingKeyId != null && signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
    } else {
        useGpgCmd()
    }

    sign(publishing.publications["maven"])
}

fun generateChecksums(file: File) {
    if (!file.exists() || file.isDirectory) return

    val md5 = MessageDigest.getInstance("MD5")
    val sha1 = MessageDigest.getInstance("SHA-1")
    val sha256 = MessageDigest.getInstance("SHA-256")
    val sha512 = MessageDigest.getInstance("SHA-512")

    val bytes = file.readBytes()

    File("${file.absolutePath}.md5").writeText(md5.digest(bytes).joinToString("") { "%02x".format(it) })
    File("${file.absolutePath}.sha1").writeText(sha1.digest(bytes).joinToString("") { "%02x".format(it) })
    File("${file.absolutePath}.sha256").writeText(sha256.digest(bytes).joinToString("") { "%02x".format(it) })
    File("${file.absolutePath}.sha512").writeText(sha512.digest(bytes).joinToString("") { "%02x".format(it) })
}

tasks.register("publishToLocalRepo") {
    dependsOn("publishMavenPublicationToMavenLocal")
    doLast {
        println("Published to local Maven repository: ~/.m2/repository")
    }
}

tasks.register<Zip>("createMavenCentralBundle") {
    dependsOn("publishMavenPublicationToMavenLocal")

    val groupPath = project.group.toString().replace(".", "/")
    val artifactId = project.name
    val ver = project.version.toString()
    val repoDir = file("${System.getProperty("user.home")}/.m2/repository/$groupPath/$artifactId/$ver")

    doFirst {
        repoDir.listFiles()?.filter {
            it.isFile && !it.name.endsWith(".md5") && !it.name.endsWith(".sha1") &&
            !it.name.endsWith(".sha256") && !it.name.endsWith(".sha512")
        }?.forEach { file ->
            generateChecksums(file)
            println("Generated checksums for: ${file.name}")
        }
    }

    from(repoDir) {
        into("$groupPath/$artifactId/$ver")
    }

    archiveFileName.set("${artifactId}-${ver}-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("maven-central"))

    doLast {
        println("=====================================")
        println("Bundle created: ${destinationDirectory.get().asFile}/${archiveFileName.get()}")
        println("Upload this file to https://central.sonatype.com")
        println("=====================================")
    }
}
