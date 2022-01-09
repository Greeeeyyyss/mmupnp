import build.ProjectProperties
import build.dependencyUpdatesSettings

plugins {
    id("kotlin")
    id("com.github.ben-manes.versions")
}

group = ProjectProperties.groupId
version = ProjectProperties.versionName

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":mmupnp"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.6.0")
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation("junit:junit:4.13.2")
}

dependencyUpdatesSettings()
