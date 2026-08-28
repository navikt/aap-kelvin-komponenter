plugins {
    id("aap.conventions")
    id("java-test-fixtures")
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from(files("docs/module.md"))
    }
}

dependencies {
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)
    compileOnly(libs.papertrail.logback.syslog4j)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
    testImplementation(libs.papertrail.logback.syslog4j)

    testFixturesImplementation(libs.logback.classic)
}