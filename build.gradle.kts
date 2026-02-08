import org.gradle.kotlin.dsl.dependencies

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    `maven-publish`
}

tasks.named("shadowJar") {
    enabled = false
}

group = "dev.nikdi"
version = libs.versions.periodic.worker.get()

kotlin {
    jvmToolchain(21)
}

java {
    withSourcesJar()
}

ktor {}

dependencies {
    implementation(libs.ktor.server.core)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.ktor.server.config.yaml)
    testImplementation(libs.kotlinx.coroutines.test)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "periodic-worker"
        }
    }
    repositories {
        val gprUser = project.findProperty("gpr.user") as String?
        val gprKey = project.findProperty("gpr.key") as String?

        if (gprUser != null && gprKey != null) {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/ItzNikDi/PeriodicWorker")
                credentials {
                    username = gprUser
                    password = gprKey
                }
            }
        }
    }
}