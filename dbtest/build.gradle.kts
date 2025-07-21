plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation("com.zaxxer:HikariCP:6.3.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.10.3")
    runtimeOnly("org.postgresql:postgresql:42.7.7")

    implementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
}
