import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    api(libs.ktor.server.auth)
    api(libs.ktor.server.auth.jwt)
    api(libs.ktor.server.call.logging)
    api(libs.ktor.server.call.id)
    api(libs.ktor.server.content.negotiation)
    api(libs.ktor.server.metrics.micrometer)
    api(libs.ktor.server.netty)
    api(project(":verdityper"))
    api(libs.ktor.server.cors)
    api(libs.ktor.server.status.pages)

    api(libs.micrometer.prometheus)

    api(libs.ktor.serialization.jackson)
    api(libs.jackson.databind)
    api(project(":ktor-openapi-generator"))

    api(project(":httpklient"))
    implementation(libs.caffeine)
    api(project(":verdityper"))
    implementation(project(":infrastructure"))

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.server.test.host)
    constraints {
        implementation(libs.commons.codec)
    }
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}