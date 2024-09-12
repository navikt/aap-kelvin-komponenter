
val mockkVersion = "1.13.12"

dependencies {
    testImplementation("io.mockk:mockk:${mockkVersion}")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
}