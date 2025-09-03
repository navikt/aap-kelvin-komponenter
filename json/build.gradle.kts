import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("komponenter.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.0")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")

    testImplementation("org.assertj:assertj-core:3.27.4")
}
