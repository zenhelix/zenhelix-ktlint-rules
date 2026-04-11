plugins {
    id("io.github.zenhelix.maven-central-publish")
}

publishing {
    repositories {
        mavenLocal()
        mavenCentralPortal {
            credentials {
                username = System.getProperty("MAVEN_SONATYPE_USERNAME") ?: System.getenv("MAVEN_SONATYPE_USERNAME")
                password = System.getProperty("MAVEN_SONATYPE_TOKEN") ?: System.getenv("MAVEN_SONATYPE_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set(
                    when (project.name) {
                        "zenhelix-ktlint-rules-spring" -> "Spring-specific KtLint rules by ZenHelix — enforces explicit return types on controller endpoints"
                        else -> "Opinionated KtLint rule set with 30 rules for compact, consistent Kotlin formatting — collapse, ordering, blank lines, and more"
                    }
                )
                url.set("https://github.com/zenhelix/zenhelix-ktlint-rules")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("dm.medakin")
                        name.set("Dmitrii Medakin")
                        email.set("dm.medakin.online@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/zenhelix/zenhelix-ktlint-rules.git")
                    developerConnection.set("scm:git:ssh://github.com/zenhelix/zenhelix-ktlint-rules.git")
                    url.set("https://github.com/zenhelix/zenhelix-ktlint-rules")
                }
            }
        }
    }
}

signing {
    val signingKeyId: String? by project
    val signingKey: String? by project
    val signingPassword: String? by project

    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKeyId, signingKey, signingPassword)
        sign(publishing.publications)
    }
}
