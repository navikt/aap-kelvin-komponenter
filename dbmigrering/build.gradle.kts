dependencies {
    implementation(project(":infrastructure"))
    implementation("org.flywaydb:flyway-database-postgresql:11.0.1")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}
