dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.1")
    runtimeOnly("org.postgresql:postgresql:42.7.4")

    implementation("org.testcontainers:postgresql:1.20.1")
}
