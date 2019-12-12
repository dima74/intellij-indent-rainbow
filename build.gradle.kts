import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.14"
    java
    kotlin("jvm") version "1.3.61"
}

group = "indent-rainbow"
version = "1.0.4"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testCompile("junit", "junit", "4.12")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2019.3"

    // setPlugins("com.chrisrm.idea.MaterialThemeUI:4.11.0")
}
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.getByName<org.jetbrains.intellij.tasks.PatchPluginXmlTask>("patchPluginXml") {
    sinceBuild("182")
    untilBuild("300")
    changeNotes(file("$projectDir/CHANGELOG.html").readText())
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.withType<PublishTask> {
    token(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
}
