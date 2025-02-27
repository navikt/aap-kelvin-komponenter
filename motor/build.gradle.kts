plugins {
    id("komponenter.conventions")
}

dependencies {
    api(project(":dbconnect"))
    api(project(":json"))
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.13.1")
    implementation("org.slf4j:slf4j-api:2.0.17")
    api("io.micrometer:micrometer-registry-prometheus:1.14.4")


    testImplementation(project(":dbtest"))

    testImplementation("io.micrometer:micrometer-registry-prometheus:1.14.4")
    testImplementation("ch.qos.logback:logback-classic:1.5.17")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.0")
    testImplementation("org.assertj:assertj-core:3.27.3")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:postgresql:1.20.5")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kotlin("test"))
}
