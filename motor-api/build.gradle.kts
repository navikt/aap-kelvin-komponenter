plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":dbconnect"))
    implementation(project(":infrastructure"))
    implementation(project(":motor"))
    implementation(libs.slf4j.api)
    // Skille ut disse til egen modul for motor-api
    implementation(project(":ktor-openapi-generator"))
    implementation(libs.ktor.http.jvm)
    implementation(libs.jackson.databind)

    testImplementation(project(":dbtest"))
    testImplementation(project(":server"))

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)

    testImplementation(project(":motor-test-utils"))
    testImplementation(libs.testcontainers)
    testImplementation(libs.logback.classic)
    testImplementation(libs.logstash.logback.encoder)
    testImplementation(libs.ktor.server.test.host)
}

sourceSets {
    test {
        resources {
            srcDirs(project(":motor").projectDir.resolve("src/test/resources"))
        }
    }
}