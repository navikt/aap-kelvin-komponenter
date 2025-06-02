plugins {
    id("komponenter.conventions")
}

val ktorVersion = "3.1.3"

dependencies {
    implementation(project(":dbconnect"))
    implementation(project(":motor"))
    implementation("org.slf4j:slf4j-api:2.0.17")
    // Skille ut disse til egen modul for motor-api
    implementation("no.nav:ktor-openapi-generator:1.0.81")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.0")

    testImplementation(project(":dbtest"))
    testImplementation(project(":server"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.0")
    testImplementation("org.assertj:assertj-core:3.27.3")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:postgresql:1.21.1")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("ch.qos.logback:logback-classic:1.5.18")
    testImplementation("net.logstash.logback:logstash-logback-encoder:8.1")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}

sourceSets {
    test {
        resources {
            srcDirs(project(":motor").projectDir.resolve("src/test/resources"))
        }
    }
}