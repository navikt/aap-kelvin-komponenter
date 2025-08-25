plugins {
    id("komponenter.conventions")
}

dependencies {
    api(project(":dbconnect"))
    api(project(":json"))
    api(project(":gateway"))
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.19.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    api("io.micrometer:micrometer-registry-prometheus:1.15.3")


    testImplementation(project(":dbtest"))

    testImplementation("io.micrometer:micrometer-registry-prometheus:1.15.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.1")
    testImplementation("org.assertj:assertj-core:3.27.4")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
        implementation("org.apache.commons:commons-lang3:3.18.0")
    }
    testImplementation(kotlin("test"))
}
