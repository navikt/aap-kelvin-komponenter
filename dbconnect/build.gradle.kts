plugins {
    id("aap.conventions")
}

dependencies {
    implementation(libs.slf4j.api)
    implementation(project(":verdityper"))

    implementation(libs.opentelemetry.annotations)
    implementation(kotlin("reflect"))
    testImplementation(project(":dbtest"))
    testImplementation(libs.postgresql)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.assertj.core)
}
