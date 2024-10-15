dependencies {
    implementation("org.slf4j:slf4j-api:2.0.16")

    testImplementation(project(":dbtest"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.2")
    testImplementation("org.assertj:assertj-core:3.26.3")
}
