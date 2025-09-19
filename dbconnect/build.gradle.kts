plugins {
    id("komponenter.conventions")
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.17")

    implementation(kotlin("reflect"))
    testImplementation(project(":dbtest"))
    testImplementation("org.postgresql:postgresql:42.7.7")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.13.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.13.1")
    testImplementation("org.assertj:assertj-core:3.27.5")
}
