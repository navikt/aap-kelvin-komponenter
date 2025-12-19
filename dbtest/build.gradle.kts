plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.19.0")
    runtimeOnly("org.postgresql:postgresql:42.7.8")
    api("org.junit.jupiter:junit-jupiter-api:6.0.0")

    implementation("org.testcontainers:testcontainers-postgresql:2.0.3")
}
