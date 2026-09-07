plugins {
    base
    id("aap.conventions")
}

// Rotprosjektet er kun et aggregeringspunkt for dokka/detekt på tvers av subprosjektene og
// inneholder ingen kode selv. `aap.conventions` setter opp maven-publish for alle prosjekter
// (inkludert rot), noe som uten denne sperren ville publisert et tomt no.nav.aap.kelvin:kelvin-komponenter-artifakt.
tasks.withType<org.gradle.api.publish.maven.tasks.AbstractPublishToMaven>().configureEach {
    enabled = false
}

dependencies {
    rootProject.subprojects
        .filterNot { it.name == "version-catalog" } // ren version-catalog-modul, ikke et JVM/dokka-bibliotek
        .forEach { subproject ->
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
for (taskName in listOf<String>("clean", "build", "assemble", "check")) {
    tasks.named(taskName) {
        dependsOn(subprojects.map { it.path + ":$taskName" })
    }
}


