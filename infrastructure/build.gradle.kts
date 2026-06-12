plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.logback.classic)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}