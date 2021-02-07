import build.*
import org.jetbrains.dokka.gradle.DokkaTask

plugins {
    `java-library`
    kotlin("jvm")
    id("org.jetbrains.dokka")
    maven
    `maven-publish`
    signing
    jacoco
    id("com.github.ben-manes.versions")
}

base.archivesBaseName = "mmupnp"
group = ProjectProperties.groupId
version = ProjectProperties.versionName

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.test {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}

tasks.named<DokkaTask>("dokkaHtml") {
    outputDirectory.set(File(projectDir, "../docs"))
}

tasks.named<DokkaTask>("dokkaJavadoc") {
    outputDirectory.set(File(buildDir, "docs/javadoc"))
}

tasks.create("javadocJar", Jar::class) {
    dependsOn("dokkaJavadoc")
    archiveClassifier.set("javadoc")
    from(File(buildDir, "docs/javadoc"))
}

tasks.create("sourcesJar", Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

artifacts {
    archives(tasks.named<Jar>("sourcesJar"))
}

dependencies {
    implementation(kotlin("stdlib"))
    api("net.mm2d.log:log:0.9.4")

    testImplementation("junit:junit:4.13.1")
    testImplementation("io.mockk:mockk:1.10.5")
    testImplementation("com.google.truth:truth:1.1.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
}

uploadArchivesSettings()
publishingSettings()
jacocoSettings()
dependencyUpdatesSettings()
