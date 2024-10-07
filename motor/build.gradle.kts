dependencies {
    api(project(":dbconnect"))
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.8.0")
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(project(":dbtest"))

    testImplementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
    testImplementation("ch.qos.logback:logback-classic:1.5.8")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.2")

    testImplementation(kotlin("test"))
}
