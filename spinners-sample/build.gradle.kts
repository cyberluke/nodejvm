import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version("5.1.0")
}

group = "net.plan99.nodejs"
version = "1.0"

application {
    mainClassName = "net.plan99.nodejs.sample.spinners.SpinnerDemoKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(rootProject)   // This would be:   implementation("net.plan99.nodejs:nodejs-interop:1.0") in a real project
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<JavaExec> {
    dependsOn(":build")
    executable("${rootProject.buildDir}/nodejvm/nodejvm")
}