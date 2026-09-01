plugins {
    `java-platform`
    `maven-publish`
}

group = "no.nav.aap.kelvin"
version = project.findProperty("version")?.toString() ?: "0.0.0"

javaPlatform {
    allowDependencies()
}

val libs = the<VersionCatalogsExtension>().named("libs")

fun versionOf(alias: String): String =
    libs.findVersion(alias).orElseThrow { IllegalArgumentException("Missing version alias: $alias") }.requiredVersion

val kelvinVersion = project.version.toString()

dependencies {
    constraints {
        // Core runtime
        api("io.ktor:ktor-server-core:${versionOf("ktor")}")
        api("io.ktor:ktor-server-netty:${versionOf("ktor")}")
        api("io.ktor:ktor-server-content-negotiation:${versionOf("ktor")}")
        api("io.ktor:ktor-server-call-logging:${versionOf("ktor")}")
        api("io.ktor:ktor-server-call-id:${versionOf("ktor")}")
        api("io.ktor:ktor-server-status-pages:${versionOf("ktor")}")
        api("io.ktor:ktor-server-auth:${versionOf("ktor")}")
        api("io.ktor:ktor-server-auth-jwt:${versionOf("ktor")}")
        api("io.ktor:ktor-serialization-jackson:${versionOf("ktor")}")
        api("io.ktor:ktor-client-core:${versionOf("ktor")}")
        api("io.ktor:ktor-client-cio:${versionOf("ktor")}")
        api("io.ktor:ktor-client-content-negotiation:${versionOf("ktor")}")

        api("com.fasterxml.jackson.core:jackson-databind:${versionOf("jackson")}")
        api("com.fasterxml.jackson.module:jackson-module-kotlin:${versionOf("jackson")}")
        api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:${versionOf("jackson")}")

        api("org.slf4j:slf4j-api:${libs.findLibrary("slf4j-api").get().get().versionConstraint.requiredVersion}")
        api("ch.qos.logback:logback-classic:${libs.findLibrary("logback-classic").get().get().versionConstraint.requiredVersion}")
        api("net.logstash.logback:logstash-logback-encoder:${libs.findLibrary("logstash-logback-encoder").get().get().versionConstraint.requiredVersion}")
        api("io.micrometer:micrometer-registry-prometheus:${libs.findLibrary("micrometer-prometheus").get().get().versionConstraint.requiredVersion}")

        // Database and migration
        api("org.postgresql:postgresql:${libs.findLibrary("postgresql").get().get().versionConstraint.requiredVersion}")
        api("com.zaxxer:HikariCP:${libs.findLibrary("hikaricp").get().get().versionConstraint.requiredVersion}")
        api("org.flywaydb:flyway-database-postgresql:${libs.findLibrary("flyway-postgresql").get().get().versionConstraint.requiredVersion}")

        // Tests
        api("org.junit.jupiter:junit-jupiter-api:${versionOf("junit-jupiter")}")
        api("org.junit.jupiter:junit-jupiter-engine:${versionOf("junit-jupiter")}")
        api("org.junit.jupiter:junit-jupiter-params:${versionOf("junit-jupiter")}")
        api("org.assertj:assertj-core:${libs.findLibrary("assertj-core").get().get().versionConstraint.requiredVersion}")
        api("org.testcontainers:testcontainers:${versionOf("testcontainers")}")
        api("org.testcontainers:testcontainers-postgresql:${versionOf("testcontainers")}")

        // Kelvin components
        api("no.nav.aap.kelvin:dbconnect:$kelvinVersion")
        api("no.nav.aap.kelvin:dbmigrering:$kelvinVersion")
        api("no.nav.aap.kelvin:dbtest:$kelvinVersion")
        api("no.nav.aap.kelvin:gateway:$kelvinVersion")
        api("no.nav.aap.kelvin:httpklient:$kelvinVersion")
        api("no.nav.aap.kelvin:infrastructure:$kelvinVersion")
        api("no.nav.aap.kelvin:json:$kelvinVersion")
        api("no.nav.aap.kelvin:motor:$kelvinVersion")
        api("no.nav.aap.kelvin:motor-api:$kelvinVersion")
        api("no.nav.aap.kelvin:motor-test-utils:$kelvinVersion")
        api("no.nav.aap.kelvin:server:$kelvinVersion")
        api("no.nav.aap.kelvin:tidslinje:$kelvinVersion")
        api("no.nav.aap.kelvin:verdityper:$kelvinVersion")
        api("no.nav.aap.kelvin:ktor-openapi-generator:$kelvinVersion")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "kelvin-bom"
            from(components["javaPlatform"])
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
