plugins {
    id("komponenter.conventions")
    id("io.ktor.plugin") version "3.1.1" apply false
}

val ktorVersion = "3.1.0"

dependencies {
    implementation(project(":dbconnect"))
    implementation(project(":motor"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    // Skille ut disse til egen modul for motor-api
    implementation("no.nav:ktor-openapi-generator:1.0.81")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    testImplementation(project(":dbtest"))
    testImplementation(project(":server"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:postgresql:1.20.5")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("ch.qos.logback:logback-classic:1.5.16")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.0")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

sourceSets {
    test {
        resources {
            srcDirs(project(":motor").projectDir.resolve("src/test/resources"))
        }
    }
}