
plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":dbconnect"))
    implementation(libs.jackson.annotations)
    testImplementation(project(":json"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}
