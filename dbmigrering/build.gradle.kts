plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation(project(":infrastructure"))
    implementation("org.flywaydb:flyway-database-postgresql:11.9.1")
    runtimeOnly("org.postgresql:postgresql:42.7.6")
}
