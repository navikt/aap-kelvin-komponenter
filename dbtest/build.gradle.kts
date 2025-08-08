plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.10.5")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    implementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
        implementation("org.apache.commons:commons-lang3:3.18.0")
    }
}
