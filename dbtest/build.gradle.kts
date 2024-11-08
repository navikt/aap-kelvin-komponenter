dependencies {
    implementation("com.zaxxer:HikariCP:6.1.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.21.0")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    implementation("org.testcontainers:postgresql:1.20.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
}
