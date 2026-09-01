pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}


plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "kelvin-komponenter"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

include(
    "kelvin-bom",
    "kelvin-catalog",
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
    "tidslinje",
    "ktor-openapi-generator"
)
