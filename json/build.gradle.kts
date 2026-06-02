import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("aap.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    api(libs.jackson.module.kotlin)
    api(libs.jackson.datatype.jsr310)

    testImplementation(libs.assertj.core)
    testImplementation(project(":tidslinje"))
    testImplementation(project(":dbconnect")) // her bor Periode :)
}
