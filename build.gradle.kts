import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File

val versionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val projectLibreMavenDependencyAliases = listOf(
    "commons-beanutils",
    "commons-collections",
    "commons-collections4",
    "commons-digester",
    "commons-lang",
    "commons-lang3",
    "commons-logging",
    "commons-pool",
    "forms",
    "flatlaf",
    "flatlaf-extras",
    "groovy",
    "itext",
    "jfreechart",
    "org-netbeans-swing-outline",
    "radiance-neon",
    "radiance-trident",
    "flamingo",
    "javax-activation-api",
    "javax-jaxb-api",
    "jaxb-runtime",
    "jackson-annotations",
    "jackson-core",
    "jackson-databind",
    "log4j-core",
    "logback-classic",
    "pdfbox",
    "poi",
    "poi-ooxml",
    "slf4j-api",
)

plugins {
    base
}

group = "com.projectlibre"
version = providers.gradleProperty("releaseVersion").getOrElse("0.0.23")
val minimumJavaRelease = 25
val activeToolchainVersion = maxOf(minimumJavaRelease, JavaVersion.current().majorVersion.toInt())

subprojects {
	apply(plugin = "java-library")

	// Flamingo still declares the obsolete JGoodies Forms artifact.  It contains
	// the same com.jgoodies.forms classes as jgoodies-forms 1.9.0, so including
	// both makes Swing layout behavior depend on wildcard classpath order.
	configurations.configureEach {
		exclude(group = "com.jgoodies", module = "forms")
	}

	repositories {
        mavenCentral()
    }

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(activeToolchainVersion))
        }
        withSourcesJar()
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(minimumJavaRelease)
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
        projectLibreMavenDependencyAliases.forEach { alias ->
            dependencies.add("implementation", versionCatalog.findLibrary(alias).get())
        }
    }
}

tasks.register("stageAppDist") {
    group = "distribution"
    description = "Builds the installable application layout for ProjectLibre."
    dependsOn(":projectlibre_ui:installDist")
}

tasks.register<Delete>("cleanLegacyPackagingArtifacts") {
    group = "build"
    description = "Removes generated legacy packaging artifacts that are not part of the Gradle source of truth."
    delete(layout.projectDirectory.dir("isolated-build"))
}

val releaseVersion = project.version.toString()
val releaseLabel = "v$releaseVersion"
val applicationVendor = "ProjectLibre, Inc."
val applicationDescription = "ProjectLibre desktop project management software"
val applicationCopyright = "Copyright © 2012-2026 ProjectLibre, Inc."
val windowsReleaseRoot = layout.buildDirectory.dir("releases/$releaseLabel")
val windowsJpackageInput = windowsReleaseRoot.map { it.dir("jpackage-input") }
val windowsAppImageDir = windowsReleaseRoot.map { it.dir("app-image") }
val windowsMsiDir = windowsReleaseRoot.map { it.dir("msi") }
val windowsExeDir = windowsReleaseRoot.map { it.dir("exe") }
val docsDownloadsDir = layout.projectDirectory.dir("docs/downloads")
val windowsFileAssociationsDir = layout.projectDirectory.dir("packaging/windows/file-associations")
val jpackageJavaHomeProvider = providers.environmentVariable("JAVA_HOME")
    .orElse(providers.systemProperty("java.home"))
    .orElse("C:\\Program Files\\Java\\latest")
val windowsRuntimeModules = listOf(
    "java.compiler",
    "java.datatransfer",
    "java.desktop",
    "java.logging",
    "java.naming",
    "java.prefs",
    "java.scripting",
    "java.sql",
    "java.xml",
    "java.xml.crypto",
    "jdk.charsets",
    "jdk.unsupported"
)

tasks.register<Sync>("prepareWindowsReleaseInput") {
    group = "distribution"
    description = "Prepares jpackage input files from the Gradle installDist output."
    dependsOn(":projectlibre_ui:installDist")

    val installLibDir = project(":projectlibre_ui").layout.buildDirectory.dir("install/projectlibre_ui/lib")
    val iconFile = layout.projectDirectory.file("packaging/windows/icons/projectlibre.ico")
    val licenseFile = layout.projectDirectory.file("packaging/licenses/license.txt")

    from(installLibDir)
    from(iconFile) {
        rename { "projectlibre.ico" }
    }
    from(licenseFile) {
        rename { "license.txt" }
    }
    from(windowsFileAssociationsDir)
    into(windowsJpackageInput)

    doLast {
        val inputDir = windowsJpackageInput.get().asFile
        val iconPath = File(inputDir, "projectlibre.ico").absolutePath.replace('\\', '/')
        listOf("mpp.properties", "pod.properties", "xml.properties").forEach { name ->
            val associationFile = File(inputDir, name)
            val content = associationFile.readText(Charsets.UTF_8).replace("@ICON_PATH@", iconPath)
            associationFile.writeText(content, Charsets.UTF_8)
        }
    }
}

tasks.register<Exec>("packageWindowsAppImage") {
    group = "distribution"
    description = "Builds the Windows app-image for the current Gradle version."
    dependsOn("prepareWindowsReleaseInput")
    onlyIf { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }

    doFirst {
        delete(windowsAppImageDir)
        windowsAppImageDir.get().asFile.mkdirs()
        val inputDir = windowsJpackageInput.get().asFile
        val javaHome = jpackageJavaHomeProvider.get()
        commandLine(
            File(javaHome, "bin/jpackage.exe").absolutePath,
            "--type", "app-image",
            "--name", "ProjectLibre",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "projectlibre_ui.jar",
            "--main-class", "com.projectlibre1.main.Main",
            "--icon", File(inputDir, "projectlibre.ico").absolutePath,
            "--license-file", File(inputDir, "license.txt").absolutePath,
            "--add-modules", windowsRuntimeModules.joinToString(","),
            "--dest", windowsAppImageDir.get().asFile.absolutePath,
            "--verbose"
        )
    }
}

tasks.register<Exec>("packageWindowsMsi") {
    group = "distribution"
    description = "Builds the Windows MSI for the current Gradle version."
    dependsOn("prepareWindowsReleaseInput")
    onlyIf { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }

    doFirst {
        delete(windowsMsiDir)
        windowsMsiDir.get().asFile.mkdirs()
        val inputDir = windowsJpackageInput.get().asFile
        val javaHome = jpackageJavaHomeProvider.get()
        val wixBin = File(System.getProperty("user.home"), "AppData/Local/Programs/WiX Toolset v7.0/bin")
        environment("PATH", wixBin.absolutePath + File.pathSeparator + System.getenv("PATH"))
        commandLine(
            File(javaHome, "bin/jpackage.exe").absolutePath,
            "--type", "msi",
            "--name", "ProjectLibre",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "projectlibre_ui.jar",
            "--main-class", "com.projectlibre1.main.Main",
            "--icon", File(inputDir, "projectlibre.ico").absolutePath,
            "--license-file", File(inputDir, "license.txt").absolutePath,
            "--add-modules", windowsRuntimeModules.joinToString(","),
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files --compress zip-9",
            "--dest", windowsMsiDir.get().asFile.absolutePath,
            "--file-associations", File(inputDir, "mpp.properties").absolutePath,
            "--file-associations", File(inputDir, "pod.properties").absolutePath,
            "--file-associations", File(inputDir, "xml.properties").absolutePath,
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser",
            "--verbose"
        )
    }
}

tasks.register<Exec>("packageWindowsExe") {
    group = "distribution"
    description = "Builds the Windows self-contained EXE for the current Gradle version."
    dependsOn("prepareWindowsReleaseInput")
    onlyIf { System.getProperty("os.name").startsWith("Windows", ignoreCase = true) }

    doFirst {
        delete(windowsExeDir)
        windowsExeDir.get().asFile.mkdirs()
        val inputDir = windowsJpackageInput.get().asFile
        val javaHome = jpackageJavaHomeProvider.get()
        val wixBin = File(System.getProperty("user.home"), "AppData/Local/Programs/WiX Toolset v7.0/bin")
        environment("PATH", wixBin.absolutePath + File.pathSeparator + System.getenv("PATH"))
        commandLine(
            File(javaHome, "bin/jpackage.exe").absolutePath,
            "--type", "exe",
            "--name", "ProjectLibre",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "projectlibre_ui.jar",
            "--main-class", "com.projectlibre1.main.Main",
            "--icon", File(inputDir, "projectlibre.ico").absolutePath,
            "--license-file", File(inputDir, "license.txt").absolutePath,
            "--add-modules", windowsRuntimeModules.joinToString(","),
            "--win-menu",
            "--win-shortcut",
            "--win-dir-chooser",
            "--verbose",
            "--dest", windowsExeDir.get().asFile.absolutePath
        )
    }
}

tasks.register<JavaExec>("verifyPackagedFileImports") {
    group = "verification"
    description = "Loads sample MPP and POD files with the same limited modules as the packaged app."
    dependsOn(":projectlibre_ui:installDist", ":projectlibre_ui:compileTestJava")

    val uiSourceSets = project(":projectlibre_ui").extensions.getByType<SourceSetContainer>()
    val uiTestOutput = uiSourceSets.named("test").map { it.output }
    val uiTestRuntimeClasspath = uiSourceSets.named("test").map { it.runtimeClasspath }

    classpath = files(uiTestOutput, uiTestRuntimeClasspath)
    mainClass.set("com.projectlibre1.integration.PackagedImportSmokeMain")
    args(
        "--windows-script",
        file("modules/projectlibre_ui/build/install/projectlibre_ui/bin/projectlibre_ui.bat").absolutePath,
        file("samples/Commercial construction project plan.mpp").absolutePath,
        file("samples/Commercial construction project plan.pod").absolutePath
    )
    jvmArgs("--limit-modules", windowsRuntimeModules.joinToString(","))
}

tasks.register<Zip>("packageWindowsZip") {
    group = "distribution"
    description = "Archives the Windows app-image as a downloadable ZIP."
    dependsOn("packageWindowsAppImage")

    from(windowsAppImageDir.map { it.dir("ProjectLibre") })
    archiveFileName.set("ProjectLibre-$releaseVersion-app-image.zip")
    destinationDirectory.set(docsDownloadsDir)

    doFirst {
        delete(docsDownloadsDir.file("ProjectLibre-$releaseVersion-app-image.zip"))
    }
}

tasks.register<Copy>("publishWindowsMsiToDocs") {
    group = "distribution"
    description = "Copies the Windows MSI into docs/downloads for GitHub Pages."
    dependsOn("packageWindowsMsi")

    from(windowsMsiDir.map { it.file("ProjectLibre-$releaseVersion.msi") })
    into(docsDownloadsDir)
}

tasks.register("publishSplitExeToDocs") {
    group = "distribution"
    description = "Splits the self-contained Windows EXE into GitHub-safe download parts and publishes them into docs/downloads."
    dependsOn("packageWindowsExe")

    doLast {
        val exeFile = windowsExeDir.get().file("ProjectLibre-$releaseVersion.exe").asFile
        val downloadsDir = docsDownloadsDir.asFile
        val partSize = 95L * 1024L * 1024L
        val baseName = "ProjectLibre-$releaseVersion.exe"
        val partPrefix = File(downloadsDir, baseName)

        downloadsDir.mkdirs()
        downloadsDir.listFiles()
            ?.filter { it.name.startsWith("$baseName.part") || it.name == "rebuild-$baseName.bat" }
            ?.forEach { it.delete() }

        exeFile.inputStream().buffered().use { input ->
            var partIndex = 1
            while (true) {
                val partFile = File(downloadsDir, "$baseName.part$partIndex")
                partFile.outputStream().buffered().use { output ->
                    var written = 0L
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (written < partSize) {
                        val maxRead = minOf(buffer.size.toLong(), partSize - written).toInt()
                        val read = input.read(buffer, 0, maxRead)
                        if (read <= 0) {
                            break
                        }
                        output.write(buffer, 0, read)
                        written += read
                    }
                    if (written == 0L) {
                        partFile.delete()
                        return@use
                    }
                }
                if (!partFile.exists()) {
                    break
                }
                partIndex++
            }
        }

        val rebuildScript = File(downloadsDir, "rebuild-$baseName.bat")
        rebuildScript.writeText(
            """
            @echo off
            setlocal
            set "TARGET=%~dp0$baseName"
            if not exist "%~dp0$baseName.part1" (
                echo Missing $baseName.part1
                exit /b 1
            )
            if not exist "%~dp0$baseName.part2" (
                echo Missing $baseName.part2
                exit /b 1
            )
            copy /b "%~dp0$baseName.part1"+"%~dp0$baseName.part2" "%TARGET%" >nul
            if errorlevel 1 exit /b 1
            echo Created %TARGET%
            endlocal
            """.trimIndent().replace("\n", System.lineSeparator()),
            Charsets.UTF_8
        )
    }
}

tasks.register("publishReleaseToDocs") {
    group = "distribution"
    description = "Builds the Windows self-contained EXE and publishes split download parts into docs/downloads."
    dependsOn("publishSplitExeToDocs")
}
