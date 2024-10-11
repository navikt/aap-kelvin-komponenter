import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    kotlin("jvm") version "2.0.20"
    id("io.ktor.plugin") version "3.0.0" apply false
    id("org.jetbrains.dokka") version "1.9.20"
    `maven-publish`
    `java-library`
}

allprojects {
    repositories {
        mavenCentral()
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}

subprojects {
    group = "no.nav.aap.kelvin"

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "maven-publish")
    apply(plugin = "java-library")
    apply(plugin = "org.jetbrains.dokka")

    kotlin {
        explicitApi = ExplicitApiMode.Warning
        compilerOptions {
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    tasks {
        withType<ShadowJar> {
            mergeServiceFiles()
        }

        withType<Test> {
            reports.html.required.set(false)
            useJUnitPlatform()
            maxParallelForks = Runtime.getRuntime().availableProcessors()
        }
    }

    java {
        withSourcesJar()
        withJavadocJar()
    }

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = project.name
                version = project.findProperty("version")?.toString() ?: "0.0.0"
                from(components["java"])
            }
        }

        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/navikt/aap-kelvin-komponenter")
                credentials {
                    username = "x-access-token"
                    password = System.getenv("GITHUB_TOKEN")
                }
            }
        }
    }

    kotlin.sourceSets["main"].kotlin.srcDirs("main/kotlin")
    kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")
    sourceSets["main"].resources.srcDirs("main/resources")
    sourceSets["test"].resources.srcDirs("test/resources")
}
