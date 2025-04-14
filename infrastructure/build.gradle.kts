plugins {
    id("komponenter.conventions")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
}