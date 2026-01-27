plugins {
    id("aap.conventions")
}

val ktorVersion = "3.4.0"
val swaggerUiVersion = "5.31.0"
val junitVersjon = "6.0.0"

dependencies {
    // Ktor server dependencies
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")

    implementation("org.slf4j:slf4j-api:2.0.17")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.21.0") // needed for multipart parsing

    // when updating the version here, don't forge to update version in OpenAPIGen.kt line 68
    api("org.webjars:swagger-ui:$swaggerUiVersion")

    implementation("org.reflections:reflections:0.10.2") // only used while initializing

    // testing
    testImplementation("io.ktor:ktor-server-netty:$ktorVersion")
    testImplementation("io.ktor:ktor-server-core:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth:$ktorVersion")
    testImplementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    testImplementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("stdlib-jdk8"))

    testImplementation("ch.qos.logback:logback-classic:1.5.26") // logging framework for the tests

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon") // junit testing framework
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersjon") // generated parameters for tests
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon") // testing runtime
}