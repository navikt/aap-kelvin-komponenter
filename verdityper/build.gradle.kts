
dependencies {
    implementation(project(":dbconnect"))
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.0")
}
