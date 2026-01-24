import java.security.MessageDigest

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    kotlin("kapt") version "2.0.21"
    `maven-publish`
    signing
    jacoco
}

group = "io.github.habinkim"
version = "0.7.5"
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
val fastjson2Version = "2.0.54"
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

    // Fastjson2 integration
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:$fastjson2Version")

    // Coroutines for async formatting
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.26.3")

    // JMH benchmark dependencies (run via test)
    testImplementation("org.openjdk.jmh:jmh-core:1.37")
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    kaptTest("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}

tasks.test {
    useJUnitPlatform {
        excludeTags("benchmark")
    }
    finalizedBy(tasks.jacocoTestReport)

    testLogging {
        events("failed", "skipped")
        showExceptions = true
        showCauses = true
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    // Collect results per test class
    val testClassResults = mutableMapOf<String, Triple<Long, Long, Long>>() // passed, failed, skipped

    afterTest(KotlinClosure2<TestDescriptor, TestResult, Unit>({ desc, result ->
        val className = desc.className?.substringAfterLast('.') ?: return@KotlinClosure2
        val current = testClassResults.getOrDefault(className, Triple(0L, 0L, 0L))
        testClassResults[className] = when (result.resultType) {
            TestResult.ResultType.SUCCESS -> Triple(current.first + 1, current.second, current.third)
            TestResult.ResultType.FAILURE -> Triple(current.first, current.second + 1, current.third)
            TestResult.ResultType.SKIPPED -> Triple(current.first, current.second, current.third + 1)
        }
    }))

    doLast {
        println("")
        println("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        println("┃  TEST RESULTS BY CLASS                                                       ┃")
        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")

        testClassResults.toSortedMap().forEach { (className, counts) ->
            val (passed, failed, skipped) = counts
            val status = when {
                failed > 0 -> "✗"
                skipped > 0 && passed > 0 -> "◐"
                skipped > 0 && passed == 0L -> "○"
                else -> "✓"
            }
            val total = passed + failed + skipped
            println("┃  $status ${className.take(40).padEnd(40)} $passed/$total passed".padEnd(78) + "┃")
        }

        val totalPassed = testClassResults.values.sumOf { it.first }
        val totalFailed = testClassResults.values.sumOf { it.second }
        val totalSkipped = testClassResults.values.sumOf { it.third }
        val totalTests = totalPassed + totalFailed + totalSkipped
        val status = if (totalFailed > 0) "FAILED" else "SUCCESS"

        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        println("┃  SUMMARY: $status | Total: $totalTests | Passed: $totalPassed | Failed: $totalFailed | Skipped: $totalSkipped".padEnd(78) + "┃")
        println("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }
}

jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/tokenizing/**",  // Tokenizer internals
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        // Overall project coverage
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.70".toBigDecimal()
            }
        }

        // Core formatting classes - higher standard
        rule {
            element = "CLASS"
            includes = listOf(
                "io.github.fracturedjson.core.Formatter",
                "io.github.fracturedjson.core.JsonItem"
            )
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = "0.80".toBigDecimal()
            }
        }
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/tokenizing/**",           // Tokenizer internals
                    "**/*Exception*",             // Exception classes
                    "**/*Companion*",             // Companion objects
                    "**/NullBuffer*",             // Testing utility
                )
            }
        })
    )
}

// Cross-language test synchronization verification
tasks.register("verifyTestSync") {
    group = "verification"
    description = "Verifies that Java and Kotlin tests are synchronized"

    doLast {
        val kotlinTestDir = file("src/test/kotlin/io/github/fracturedjson")
        val javaTestDir = file("src/test/java/io/github/fracturedjson")

        // Define which test classes should be mirrored (Kotlin -> Java with 'JavaTest' suffix)
        val mirroredTests = mapOf(
            "core/FormatterTest" to "core/FormatterJavaTest",
            "parser/ParserTest" to "parser/ParserJavaTest",
            "jackson/JsonNodeConverterTest" to "jackson/JsonNodeConverterJavaTest",
            "gson/GsonElementConverterTest" to "gson/GsonElementConverterJavaTest"
        )

        println("")
        println("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        println("┃  CROSS-LANGUAGE TEST SYNCHRONIZATION CHECK                                  ┃")
        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")

        var allSynced = true
        mirroredTests.forEach { (kotlinPath, javaPath) ->
            val kotlinFile = file("$kotlinTestDir/$kotlinPath.kt")
            val javaFile = file("$javaTestDir/$javaPath.java")

            val kotlinExists = kotlinFile.exists()
            val javaExists = javaFile.exists()

            val status = when {
                kotlinExists && javaExists -> "✓ SYNCED"
                kotlinExists && !javaExists -> { allSynced = false; "✗ MISSING JAVA" }
                !kotlinExists && javaExists -> { allSynced = false; "✗ MISSING KOTLIN" }
                else -> { allSynced = false; "✗ BOTH MISSING" }
            }

            val displayName = kotlinPath.padEnd(45)
            println("┃  $status  $displayName".padEnd(78) + "┃")
        }

        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")

        if (allSynced) {
            println("┃  STATUS: ALL TESTS SYNCHRONIZED                                            ┃")
        } else {
            println("┃  STATUS: SYNCHRONIZATION ISSUES DETECTED                                   ┃")
        }
        println("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")

        if (!allSynced) {
            throw GradleException("Java and Kotlin tests are not synchronized!")
        }
    }
}

tasks.register("testWithCoverage") {
    group = "verification"
    description = "Runs tests with JaCoCo coverage report and verification"

    dependsOn("verifyTestSync")
    dependsOn(tasks.test)
    dependsOn(tasks.jacocoTestReport)
    dependsOn(tasks.jacocoTestCoverageVerification)

    doLast {
        // Parse test results
        val testResultsDir = layout.buildDirectory.dir("test-results/test").get().asFile
        var totalTests = 0
        var passedTests = 0
        var failedTests = 0
        var skippedTests = 0

        testResultsDir.listFiles()?.filter { it.extension == "xml" }?.forEach { file ->
            val content = file.readText()
            val testsMatch = Regex("""tests="(\d+)"""").find(content)
            val failuresMatch = Regex("""failures="(\d+)"""").find(content)
            val errorsMatch = Regex("""errors="(\d+)"""").find(content)
            val skippedMatch = Regex("""skipped="(\d+)"""").find(content)

            val tests = testsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val failures = failuresMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val errors = errorsMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val skipped = skippedMatch?.groupValues?.get(1)?.toIntOrNull() ?: 0

            totalTests += tests
            failedTests += failures + errors
            skippedTests += skipped
        }
        passedTests = totalTests - failedTests - skippedTests

        // Parse coverage from XML
        val coverageXml = layout.buildDirectory.file("reports/jacoco/test/jacocoTestReport.xml").get().asFile
        var lineCovered = 0
        var lineMissed = 0
        var branchCovered = 0
        var branchMissed = 0

        if (coverageXml.exists()) {
            val content = coverageXml.readText()
            // Get the last (overall) counters
            val lineMatches = Regex("""<counter type="LINE" missed="(\d+)" covered="(\d+)"/>""").findAll(content).toList()
            val branchMatches = Regex("""<counter type="BRANCH" missed="(\d+)" covered="(\d+)"/>""").findAll(content).toList()

            if (lineMatches.isNotEmpty()) {
                val last = lineMatches.last()
                lineMissed = last.groupValues[1].toInt()
                lineCovered = last.groupValues[2].toInt()
            }
            if (branchMatches.isNotEmpty()) {
                val last = branchMatches.last()
                branchMissed = last.groupValues[1].toInt()
                branchCovered = last.groupValues[2].toInt()
            }
        }

        val lineCoverage = if (lineCovered + lineMissed > 0)
            "%.1f".format(lineCovered * 100.0 / (lineCovered + lineMissed)) else "0.0"
        val branchCoverage = if (branchCovered + branchMissed > 0)
            "%.1f".format(branchCovered * 100.0 / (branchCovered + branchMissed)) else "0.0"

        val reportDir = layout.buildDirectory.dir("reports/jacoco/test/html").get().asFile

        println("")
        println("╔═══════════════════════════════════════════════════════════╗")
        println("║              TEST & COVERAGE SUMMARY                      ║")
        println("╠═══════════════════════════════════════════════════════════╣")
        println("║  Tests:                                                   ║")
        println("║    Total:   ${totalTests.toString().padEnd(6)} Passed: ${passedTests.toString().padEnd(6)} Failed: ${failedTests.toString().padEnd(6)}  ║")
        println("║    Skipped: ${skippedTests.toString().padEnd(47)}║")
        println("╠═══════════════════════════════════════════════════════════╣")
        println("║  Coverage:                                                ║")
        println("║    Line:   ${lineCoverage.padEnd(6)}%  (${lineCovered}/${lineCovered + lineMissed})".padEnd(60) + "║")
        println("║    Branch: ${branchCoverage.padEnd(6)}%  (${branchCovered}/${branchCovered + branchMissed})".padEnd(60) + "║")
        println("╠═══════════════════════════════════════════════════════════╣")
        println("║  Report: file://${reportDir.absolutePath}/index.html".take(59).padEnd(59) + "║")
        println("╚═══════════════════════════════════════════════════════════╝")
    }
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

tasks.named("publishMavenPublicationToMavenLocal") {
    mustRunAfter("testWithCoverage")
}

tasks.register<Zip>("createMavenCentralBundle") {
    description = "Creates Maven Central bundle with test verification and coverage check"
    group = "publishing"

    dependsOn("testWithCoverage")
    dependsOn("publishMavenPublicationToMavenLocal")

    val groupPath = project.group.toString().replace(".", "/")
    val artifactId = project.name
    val ver = project.version.toString()
    val repoDir = file("${System.getProperty("user.home")}/.m2/repository/$groupPath/$artifactId/$ver")

    doFirst {
        println("")
        println("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        println("┃  GENERATING MAVEN CENTRAL BUNDLE                                            ┃")
        println("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
        println("")

        repoDir.listFiles()?.filter {
            it.isFile && !it.name.endsWith(".md5") && !it.name.endsWith(".sha1") &&
            !it.name.endsWith(".sha256") && !it.name.endsWith(".sha512")
        }?.forEach { file ->
            generateChecksums(file)
            println("  ✓ Generated checksums for: ${file.name}")
        }
    }

    from(repoDir) {
        into("$groupPath/$artifactId/$ver")
    }

    archiveFileName.set("${artifactId}-${ver}-bundle.zip")
    destinationDirectory.set(layout.buildDirectory.dir("maven-central"))

    doLast {
        println("")
        println("┏━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┓")
        println("┃  MAVEN CENTRAL BUNDLE READY                                                 ┃")
        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        println("┃  Bundle: ${archiveFileName.get()}".padEnd(78) + "┃")
        println("┃  Location: ${destinationDirectory.get().asFile.absolutePath}".take(77).padEnd(78) + "┃")
        println("┣━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┫")
        println("┃  Next step: Upload to https://central.sonatype.com                          ┃")
        println("┗━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━┛")
    }
}

// ============================================================
// JMH Benchmark Task (runs benchmarks via test infrastructure)
// ============================================================
tasks.register<Test>("benchmark") {
    group = "benchmark"
    description = "Runs JMH benchmarks via JUnit test runner"

    useJUnitPlatform {
        includeTags("benchmark")
    }

    // JVM arguments optimized for benchmarking
    jvmArgs = listOf("-Xms2g", "-Xmx2g", "-XX:+UseG1GC")

    // Disable JaCoCo for accurate benchmark results
    extensions.configure<JacocoTaskExtension> {
        isEnabled = false
    }

    // Pass benchmark configuration as system properties
    systemProperty("bench.include", findProperty("bench.include") ?: ".*Benchmark.*")
    systemProperty("bench.quick", findProperty("bench.quick") ?: "false")
    systemProperty("bench.profiler", findProperty("bench.profiler") ?: "")

    testLogging {
        events("started", "passed", "failed")
        showStandardStreams = true
    }
}
