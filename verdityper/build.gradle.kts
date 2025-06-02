
plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation(project(":dbconnect"))
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.19.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.0")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
