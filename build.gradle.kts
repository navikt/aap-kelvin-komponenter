plugins {
    base
    id("komponenter.conventions")
}

dependencies {
    rootProject.subprojects.forEach { subproject ->
        dokka(project(":" + subproject.name))
    }
}