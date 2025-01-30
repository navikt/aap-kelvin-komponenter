import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

kotlin.explicitApi = ExplicitApiMode.Warning

val junitVersion = "5.11.4"


dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine:3.2.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.assertj:assertj-core:3.27.3")
}
