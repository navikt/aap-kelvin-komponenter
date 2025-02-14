import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("komponenter.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    implementation(project(":dbconnect")) // Periode
    implementation(project(":verdityper"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.4")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
