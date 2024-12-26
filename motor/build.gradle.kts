dependencies {
    api(project(":dbconnect"))
    api(project(":json"))
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.11.0")
    implementation("org.slf4j:slf4j-api:2.0.16")
    api("io.micrometer:micrometer-registry-prometheus:1.14.2")


    testImplementation(project(":dbtest"))

    testImplementation("io.micrometer:micrometer-registry-prometheus:1.14.2")
    testImplementation("ch.qos.logback:logback-classic:1.5.15")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.0")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:postgresql:1.20.4")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kotlin("test"))
}
