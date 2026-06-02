import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    implementation(project(":dbconnect")) // Periode
    implementation(libs.jackson.annotations)
    implementation(project(":verdityper"))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}
