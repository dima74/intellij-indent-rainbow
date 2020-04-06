import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.18"
    java
    kotlin("jvm") version "1.3.71"
}

group = "indent-rainbow"
version = "1.4.2"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))

    testImplementation("junit", "junit", "4.13")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2019.3"

    setPlugins(
        "PsiViewer:193-SNAPSHOT"
        // "com.chrisrm.idea.MaterialThemeUI:4.11.0"
        // "org.toml.lang:0.2.115.36-193",
        // "org.rust.lang:0.2.118.2171-193"
    )
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    sinceBuild("182")
    untilBuild("700")
    changeNotes(file("$projectDir/CHANGELOG.html").readText())
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<PublishTask> {
    token(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
}
