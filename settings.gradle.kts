plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}
rootProject.name = "kelvin-komponenter"

include(
    "infrastructure",
    "dbmigrering",
    "dbconnect",
    "dbtest",
    "json",
    "motor",
    "motor-test-utils",
    "motor-api",
    "httpklient",
    "server",
    "verdityper",
    "tidslinje",
    "cache"
)
