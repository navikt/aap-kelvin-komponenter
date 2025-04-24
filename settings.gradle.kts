pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}
rootProject.name = "kelvin-komponenter"


include(
    "infrastructure",
    "dbmigrering",
    "dbconnect",
    "dbtest",
    "gateway",
    "json",
    "motor",
    "motor-test-utils",
    "motor-api",
    "httpklient",
    "server",
    "verdityper",
    "tidslinje"
)
