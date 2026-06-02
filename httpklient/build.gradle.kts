import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning


dependencies {
    api(project(":json"))
    implementation(project(":infrastructure"))
    implementation(project(":verdityper"))
    implementation(libs.caffeine)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.logback.classic)
    implementation((project(":ktor-openapi-generator")))
    api(libs.micrometer.prometheus)

    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.jackson.databind)
    testImplementation(libs.jackson.datatype.jsr310)
    testImplementation(libs.ktor.server.content.negotiation)
}
