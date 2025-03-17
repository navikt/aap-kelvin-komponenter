
plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation(project(":dbconnect"))
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.1")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
