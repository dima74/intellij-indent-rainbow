import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.intellij.tasks.RunIdeTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij") version "0.4.21"
    java
    kotlin("jvm") version "1.4.0"
}

group = "indent-rainbow"
version = "1.5.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(kotlin("stdlib-jdk8"))
    implementation("io.sentry:sentry:1.7.30") {
        // we need to exclude the slf4j transitive dependency
        // IntelliJ already bundles it and will report a classloader
        // problem if this isn't excluded
        exclude("org.slf4j")
    }

    testImplementation("junit", "junit", "4.13")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    version = "2020.2"
    // version = "203-EAP-SNAPSHOT"

    setPlugins(
        "PsiViewer:202-SNAPSHOT.3"
        // "com.chrisrm.idea.MaterialThemeUI:4.11.0"
        // "org.toml.lang:0.2.115.36-193",
        // "org.rust.lang:0.2.118.2171-193"
    )
}

tasks.withType<RunIdeTask> {
    jvmArgs("-Xmx2G", "-XX:+UseG1GC", "-XX:SoftRefLRUPolicyMSPerMB=50")
    jvmArgs("-Didea.ProcessCanceledException=disabled")
}
tasks.getByName<PatchPluginXmlTask>("patchPluginXml") {
    sinceBuild("193")
    untilBuild("700")
    changeNotes(file("$projectDir/CHANGELOG.html").readText())
}
tasks.withType<PublishTask> {
    token(System.getenv("ORG_GRADLE_PROJECT_intellijPublishToken"))
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        languageVersion = "1.4"
        apiVersion = "1.3"
    }
}
