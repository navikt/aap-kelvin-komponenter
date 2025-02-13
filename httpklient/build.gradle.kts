import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

val ktorVersion = "3.1.0"
val junitVersion = "5.11.4"

kotlin.explicitApi = ExplicitApiMode.Warning


dependencies {
    api(project(":json"))
    implementation(project(":infrastructure"))
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    implementation("ch.qos.logback:logback-classic:1.5.16")
    implementation("no.nav:ktor-openapi-generator:1.0.81")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:4.1.118.Final")
    }
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.2")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")

}
