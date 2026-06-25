plugins {
    base
    id("aap.conventions")
}

dependencies {
    rootProject.subprojects.forEach { subproject ->
        dokka(project(":" + subproject.name))
    }
}

val detektReportMergeSarif = tasks.register<dev.detekt.gradle.report.ReportMergeTask>("detektReportMergeSarif") {
    description = "Merge Detekt reports from all subprojects"
    output.set(rootProject.layout.buildDirectory.file("reports/detekt/merge.sarif"))
}

subprojects {
    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        finalizedBy(detektReportMergeSarif)
        detektReportMergeSarif.configure {
            input.from(reports.sarif.outputLocation)
        }
    }
}

// Call the tasks of the subprojects
subprojects {
    // no-op; just ensuring subprojects are configured
}
for (taskName in listOf<String>("clean", "build", "assemble", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.path + ":$taskName" })
    }
}


