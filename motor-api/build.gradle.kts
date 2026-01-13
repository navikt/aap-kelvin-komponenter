plugins {
    id("komponenter.conventions")
}

val ktorVersion = "3.3.3"

dependencies {
    implementation(project(":dbconnect"))
    implementation(project(":motor"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    // Skille ut disse til egen modul for motor-api
    implementation("no.nav:ktor-openapi-generator:1.0.136")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.1")

    testImplementation(project(":dbtest"))
    testImplementation(project(":server"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.6")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:testcontainers:2.0.3")
    testImplementation("ch.qos.logback:logback-classic:1.5.24")
    testImplementation("net.logstash.logback:logstash-logback-encoder:9.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

sourceSets {
    test {
        resources {
            srcDirs(project(":motor").projectDir.resolve("src/test/resources"))
        }
    }
}