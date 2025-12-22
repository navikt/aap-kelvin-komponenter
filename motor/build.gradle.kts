plugins {
    id("komponenter.conventions")
}

dependencies {
    api(project(":dbconnect"))
    api(project(":json"))
    api(project(":gateway"))
    api("io.opentelemetry.instrumentation:opentelemetry-instrumentation-api:2.23.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    api("io.micrometer:micrometer-registry-prometheus:1.16.1")


    testImplementation(project(":dbtest"))

    testImplementation("io.micrometer:micrometer-registry-prometheus:1.16.1")
    testImplementation("ch.qos.logback:logback-classic:1.5.22")
    testImplementation("net.logstash.logback:logstash-logback-encoder:9.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.6")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:testcontainers:2.0.3")

    testImplementation(kotlin("test"))
}
