plugins {
    base
    `version-catalog`
    `maven-publish`
}

group = "no.nav.aap.kelvin"
version = project.findProperty("version")?.toString() ?: "0.0.0"

catalog {
    versionCatalog {
        from(files("../gradle/libs.versions.toml"))
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenVersionCatalog") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
            from(components["versionCatalog"])

            pom {
                name.set("Kelvin Version Catalog")
                description.set(
                    "Shared Gradle version catalog with common dependency versions " +
                        "(ktor, jackson, logback, testcontainers, etc.) used by AAP repos consuming kelvin-komponenter."
                )
                url.set("https://github.com/navikt/aap-kelvin-komponenter/version-catalog")
                packaging = "toml"
                licenses {
                    license {
                        name.set("Apache-2.0 License")
                        url.set("https://github.com/navikt/aap-kelvin-komponenter/blob/master/LICENSE")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-kelvin-komponenter")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
