import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("komponenter.conventions")
}

val ktorVersion = "3.1.3"
val junitVersion = "5.12.2"

kotlin.explicitApi = ExplicitApiMode.Warning


dependencies {
    api(project(":json"))
    implementation(project(":infrastructure"))
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("no.nav:ktor-openapi-generator:1.0.81")
    api("io.micrometer:micrometer-registry-prometheus:1.15.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.2.1.Final")
    }
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.0")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

}
