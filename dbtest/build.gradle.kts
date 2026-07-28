plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.hikaricp)
    implementation(libs.flyway.postgresql)
    runtimeOnly(libs.postgresql)
    api(libs.junit.jupiter.api)

    implementation(libs.testcontainers.postgresql)

    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}
