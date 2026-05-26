import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

fun org.gradle.api.Project.projectLibreExternalLibs() =
    fileTree(rootProject.file("projectlibre_contrib/lib")) {
        include("**/*.jar")
        exclude("flamingo-6.2.jar", "trident-6.2.jar")
    }

plugins {
    base
}

group = "com.projectlibre"
version = "0.0.1"

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(26))
        }
        withSourcesJar()
    }

    extensions.configure<SourceSetContainer>("sourceSets") {
        named("main") {
            java.setSrcDirs(listOf("src"))
            java.exclude("test/**")

            resources.setSrcDirs(listOf("src"))
            resources.exclude("**/*.java", "test/**")
        }
        named("test") {
            java.setSrcDirs(emptyList<String>())
            resources.setSrcDirs(emptyList<String>())
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(26)
    }

    tasks.withType<ProcessResources>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Test>().configureEach {
        enabled = false
    }

    if (name != "projectlibre_contrib") {
        dependencies.add("implementation", projectLibreExternalLibs())
    }
}

tasks.register("stageAppDist") {
    group = "distribution"
    description = "Builds the installable application layout for ProjectLibre."
    dependsOn(":projectlibre_ui:installDist")
}
