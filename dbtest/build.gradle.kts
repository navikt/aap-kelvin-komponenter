plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")
    runtimeOnly("org.postgresql:postgresql:42.7.6")

    implementation("org.testcontainers:postgresql:1.21.1")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
}
