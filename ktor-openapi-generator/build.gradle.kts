import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from(files("docs/module.md"))
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Disabled
}

dependencies {
    // Ktor server dependencies
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.serialization.jackson)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)

    implementation(libs.slf4j.api)

    implementation(libs.jackson.datatype.jsr310) // needed for multipart parsing

    // when updating the version here, don't forge to update version in OpenAPIGen.kt line 68
    api(libs.swagger.ui)

    implementation(libs.reflections) // only used while initializing

    // testing
    testImplementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.server.auth)
    testImplementation(libs.ktor.server.auth.jwt)
    testImplementation(libs.ktor.server.content.negotiation)
    testImplementation(libs.ktor.serialization.jackson)
    testImplementation(libs.ktor.client.content.negotiation)

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))

    testImplementation(libs.logback.classic) // logging framework for the tests

    testImplementation(libs.junit.jupiter.api) // junit testing framework
    testImplementation(libs.junit.jupiter.params) // generated parameters for tests
    testRuntimeOnly(libs.junit.jupiter.engine) // testing runtime
    testImplementation(libs.assertj.core)
}
