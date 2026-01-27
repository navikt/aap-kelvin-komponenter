import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
    `maven-publish`
    `java-library`
}

group = "no.nav.aap.kelvin"
version = project.findProperty("version")?.toString() ?: "0.0.0"

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
}

// https://docs.gradle.org/8.12.1/userguide/jvm_test_suite_plugin.html
testing {
    suites {
        @Suppress("UnstableApiUsage") val test by getting(JvmTestSuite::class) {
            useJUnitJupiter()
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            remoteUrl("https://github.com/navikt/aap-kelvin-komponenter/")
            localDirectory.set(rootDir)
        }
    }
}

tasks.test {
    useJUnitPlatform()
    reports.html.required.set(false)
    maxParallelForks = Runtime.getRuntime().availableProcessors() / 2
}

kotlin {
    jvmToolchain(21)
    explicitApi = ExplicitApiMode.Warning
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
    }
}

// Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme språk-versjon som vi kompilerer til
val toolchainLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.withType<Test>().configureEach { javaLauncher.set(toolchainLauncher) }
tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }

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

            if (project.name == "ktor-openapi-generator") {
                pom {
                    name.set("Ktor OpenAPI/Swagger 3 Generator")
                    description.set("The Ktor OpenAPI Generator is a library to automatically generate the descriptor as you route your ktor application.")
                    url.set("https://github.com/navikt/aap-kelvin-komponenter/ktor-openapi-generator")
                    packaging = "jar"
                    licenses {
                        license {
                            name.set("Apache-2.0 License")
                            url.set("https://github.com/navikt/aap-kelvin-komponenter/blob/master/ktor-openapi-generator/LICENSE")
                        }
                    }
                }
            }
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

kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")