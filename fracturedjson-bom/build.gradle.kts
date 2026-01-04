plugins {
    `java-platform`
    `maven-publish`
}

description = "FracturedJson BOM - Bill of Materials for version management"

javaPlatform {
    allowDependencies()
}

dependencies {
    constraints {
        api(project(":fracturedjson-core"))
        api(project(":fracturedjson-parser"))
        api(project(":fracturedjson-kotlinx"))
        api(project(":fracturedjson-jackson"))
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["javaPlatform"])

            pom {
                name.set("FracturedJson BOM")
                description.set("Bill of Materials for FracturedJson Kotlin modules")
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
