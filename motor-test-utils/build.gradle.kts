plugins {
    id("aap.conventions")
}

dependencies {
    implementation(project(":dbconnect"))
    implementation(project(":motor"))
    implementation(libs.slf4j.api)
}