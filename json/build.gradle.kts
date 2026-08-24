import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    api(libs.jackson.module.kotlin)

    testImplementation(libs.assertj.core)
    testImplementation(project(":tidslinje"))
    testImplementation(project(":verdityper"))
}
