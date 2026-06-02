plugins {
    id("aap.conventions")
}

dependencies {
    api(project(":dbconnect"))
    api(project(":json"))
    api(project(":gateway"))
    api(libs.opentelemetry.api)
    implementation(libs.slf4j.api)
    api(libs.micrometer.prometheus)


    testImplementation(project(":dbtest"))

    testImplementation(libs.micrometer.prometheus)
    testImplementation(libs.logback.classic)
    testImplementation(libs.logstash.logback.encoder)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)

    testImplementation(project(":motor-test-utils"))
    testImplementation(libs.testcontainers)

    testImplementation(kotlin("test"))
}
