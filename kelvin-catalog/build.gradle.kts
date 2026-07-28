plugins {
    `version-catalog`
    `maven-publish`
}

group = "no.nav.aap.kelvin"
version = project.findProperty("version")?.toString() ?: "0.0.0"

catalog {
    versionCatalog {
        from(files("../gradle/libs.versions.toml"))

        version("komponenter", project.version.toString())
        version("kelvin-bom", project.version.toString())
        version("tilgang", "1.0.234")
        version("behandlingsflyt-kontrakt", "0.0.640")

        library("kelvin-bom", "no.nav.aap.kelvin", "kelvin-bom").versionRef("kelvin-bom")

        library("json", "no.nav.aap.kelvin", "json").versionRef("komponenter")
        library("infrastructure", "no.nav.aap.kelvin", "infrastructure").versionRef("komponenter")
        library("dbconnect", "no.nav.aap.kelvin", "dbconnect").versionRef("komponenter")
        library("dbmigrering", "no.nav.aap.kelvin", "dbmigrering").versionRef("komponenter")
        library("dbtest", "no.nav.aap.kelvin", "dbtest").versionRef("komponenter")
        library("gateway", "no.nav.aap.kelvin", "gateway").versionRef("komponenter")
        library("httpklient", "no.nav.aap.kelvin", "httpklient").versionRef("komponenter")
        library("motor", "no.nav.aap.kelvin", "motor").versionRef("komponenter")
        library("motor-api", "no.nav.aap.kelvin", "motor-api").versionRef("komponenter")
        library("motor-test-utils", "no.nav.aap.kelvin", "motor-test-utils").versionRef("komponenter")
        library("server", "no.nav.aap.kelvin", "server").versionRef("komponenter")
        library("tidslinje", "no.nav.aap.kelvin", "tidslinje").versionRef("komponenter")
        library("verdityper", "no.nav.aap.kelvin", "verdityper").versionRef("komponenter")
        library("ktor-openapi-generator", "no.nav.aap.kelvin", "ktor-openapi-generator").versionRef("komponenter")

        library("tilgang-plugin", "no.nav.aap.tilgang", "plugin").versionRef("tilgang")
        library("behandlingsflyt-kontrakt", "no.nav.aap.behandlingsflyt", "kontrakt").versionRef("behandlingsflyt-kontrakt")

        bundle("kelvin-core", listOf("json", "infrastructure", "dbconnect", "dbmigrering", "httpklient", "server"))
        bundle("domain-tilgang", listOf("tilgang-plugin"))
        bundle("domain-behandlingsflyt", listOf("behandlingsflyt-kontrakt"))
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
