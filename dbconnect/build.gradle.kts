plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.slfj)

    implementation(kotlin("reflect"))
    testImplementation(project(":dbtest"))
    testImplementation("org.postgresql:postgresql:42.7.10")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.0.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.0.0")
    testImplementation("org.assertj:assertj-core:3.27.7")
}
