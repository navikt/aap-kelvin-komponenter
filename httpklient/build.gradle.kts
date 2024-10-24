import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

val ktorVersion = "3.0.0"
val junitVersion = "5.11.3"

kotlin.explicitApi = ExplicitApiMode.Warning


dependencies {
    implementation(project(":infrastructure"))
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    implementation("ch.qos.logback:logback-classic:1.5.11")
    implementation("no.nav:ktor-openapi-generator:1.0.46")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.0")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.0")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

}
