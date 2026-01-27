import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

val ktorVersion = "3.3.3"
val junitVersion = "6.0.0"

kotlin.explicitApi = ExplicitApiMode.Warning


dependencies {
    api(project(":json"))
    implementation(project(":infrastructure"))
    implementation(project(":verdityper"))
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    implementation("ch.qos.logback:logback-classic:1.5.24")
    implementation("no.nav:ktor-openapi-generator:1.0.136")
    api("io.micrometer:micrometer-registry-prometheus:1.16.2")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.1")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

}
