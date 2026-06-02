plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":infrastructure"))
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
}
