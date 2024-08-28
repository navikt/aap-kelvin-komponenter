val ktorVersion = "2.3.12"

dependencies {
    implementation(project(":dbmigrering"))
    implementation(project(":dbconnect"))
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation(project(":dbtest"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation(kotlin("test"))
}