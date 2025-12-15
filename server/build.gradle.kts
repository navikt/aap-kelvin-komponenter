import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("komponenter.conventions")
}

val ktorVersion = "3.3.3"

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    api("io.ktor:ktor-server-auth:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    api("io.ktor:ktor-server-call-logging:$ktorVersion")
    api("io.ktor:ktor-server-call-id:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    api("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-codec-http2:4.2.8.Final")
    }
    api(project(":verdityper"))
    api("io.ktor:ktor-server-cors:$ktorVersion")
    api("io.ktor:ktor-server-status-pages:$ktorVersion")

    api("io.micrometer:micrometer-registry-prometheus:1.16.1")

    api("io.ktor:ktor-serialization-jackson:$ktorVersion")
    api("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    api("no.nav:ktor-openapi-generator:1.0.131")


    api(project(":httpklient"))
    api(project(":verdityper"))

    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:1.20.0")
    }
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.6")
}