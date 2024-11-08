dependencies {
    implementation(project(":infrastructure"))
    implementation("org.flywaydb:flyway-database-postgresql:10.21.0")
    runtimeOnly("org.postgresql:postgresql:42.7.4")
}
