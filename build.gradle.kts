plugins {
    kotlin("jvm") version "1.9.24"
    application
    kotlin("kapt") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
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

//kotlin {
//    jvmToolchain(8)
//}

application {
    mainClass.set("MainKt")
}