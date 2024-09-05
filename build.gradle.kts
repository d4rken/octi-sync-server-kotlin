import java.io.ByteArrayOutputStream

plugins {
    kotlin("jvm") version "1.9.24"
    application
    kotlin("kapt") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("io.ktor.plugin") version "3.0.0-beta-2"
}

group = "eu.darken"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.dagger:dagger:2.51")
    kapt("com.google.dagger:dagger-compiler:2.51")

    val ktor_version = "3.0.0-beta-2"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-body-limit:$ktor_version")
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("ch.qos.logback:logback-classic:1.3.14")

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("eu.darken.octi.kserver.App")
}

tasks.register("generateBuildInfo") {
    doLast {
        val gitSHA = try {
            ByteArrayOutputStream().use { outputStream ->
                exec {
                    commandLine = "git rev-parse --short HEAD".split(" ")
                    standardOutput = outputStream
                }
                outputStream.toString().trim()
            }
        } catch (e: Exception) {
            print("gitSHA error: $e")
            "?"
        }

        val gitDate = try {
            ByteArrayOutputStream().use { outputStream ->
                exec {
                    commandLine = "git show -s --format=%ci $gitSHA".split(" ")
                    standardOutput = outputStream
                }
                outputStream.toString().trim()
            }
        } catch (e: Exception) {
            print("gitDate error: $e")
            "?"
        }

        val outputDir = File(buildDir, "generated/buildinfo")
        outputDir.mkdirs()
        File(outputDir, "BuildInfo.kt").apply {
            writeText(
                """
                    package eu.darken.octi.kserver
        
                    object BuildInfo {
                        const val GIT_SHA: String = "$gitSHA"
                        const val GIT_DATE: String = "$gitDate"
                    }
                """.trimIndent()
            )
        }
    }
}

tasks.named("compileKotlin") {
    dependsOn("generateBuildInfo")
}

sourceSets["main"].java.srcDir("build/generated/buildinfo")