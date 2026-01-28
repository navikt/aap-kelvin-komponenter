
plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":dbconnect"))
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
    testImplementation(project(":json"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
}
