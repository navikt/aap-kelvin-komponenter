val ktorVersion = "3.0.1"

dependencies {
    implementation(project(":dbconnect"))
    implementation(project(":motor"))
    implementation("org.slf4j:slf4j-api:2.0.16")
    // Skille ut disse til egen modul for motor-api
    implementation("no.nav:ktor-openapi-generator:1.0.50")
    implementation("io.ktor:ktor-http-jvm:$ktorVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.1")

    testImplementation(project(":dbtest"))
    testImplementation(project(":server"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.3")
    testImplementation("org.assertj:assertj-core:3.26.3")

    testImplementation(project(":motor-test-utils"))
    testImplementation("org.testcontainers:postgresql:1.20.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("ch.qos.logback:logback-classic:1.5.12")
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