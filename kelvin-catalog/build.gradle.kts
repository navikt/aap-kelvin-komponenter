plugins {
    `version-catalog`
    `maven-publish`
}

group = "no.nav.aap.kelvin"
version = project.findProperty("version")?.toString() ?: "0.0.0"

catalog {
    versionCatalog {
        from(files("gradle/libs.versions.toml"))
        version("komponenter", project.version.toString())
        version("kelvin-bom", project.version.toString())
        library("kelvin-bom", "no.nav.aap.kelvin", "kelvin-bom").versionRef("kelvin-bom")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "kelvin-catalog"
            from(components["versionCatalog"])
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
