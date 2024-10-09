import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

val ktorVersion = "2.3.12"

kotlin.explicitApi = ExplicitApiMode.Warning


dependencies {
    implementation(project(":infrastructure"))
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("no.nav:ktor-openapi-generator:1.0.42")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.1")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

}
