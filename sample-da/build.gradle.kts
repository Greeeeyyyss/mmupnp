import build.ProjectProperties

plugins {
    id("kotlin")
}

group = ProjectProperties.groupId
version = ProjectProperties.versionName

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

dependencies {
    implementation(project(":mmupnp"))
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.4.2")
    testImplementation("junit:junit:4.13.1")
}