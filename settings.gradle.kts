plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
rootProject.name = "kelvin-komponenter"

include(
    "infrastructure",
    "dbmigrering",
    "dbconnect",
    "dbtest",
    "motor",
    "motor-api",
    "httpklient",
    "server"
)
