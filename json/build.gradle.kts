import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("komponenter.conventions")
}

kotlin.explicitApi = ExplicitApiMode.Warning

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
}
