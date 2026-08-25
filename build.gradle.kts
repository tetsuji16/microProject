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
    "jcl-over-slf4j",
    "commons-pool",
    "forms",
    "flatlaf",
    "flatlaf-extras",
    "groovy",
    "openpdf",
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
    "slf4j-simple",
)

plugins {
    base
}

group = "com.microproject"
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
		// Exclude log4j2 core and logback: commons-logging 1.3.x auto-discovers them and
		// recurses via StackWalker on modern JDKs (StackOverflowError in
		// Configuration.getInstance / Digester config parse), which breaks .pod loading.
		// log4j-api is kept (POI needs it); jcl-over-slf4j replaces commons-logging and
		// routes to slf4j-simple (issue #154).
		exclude(group = "org.apache.logging.log4j", module = "log4j-core")
		exclude(group = "ch.qos.logback", module = "logback-classic")
		exclude(group = "commons-logging", module = "commons-logging")
		// Also drop commons-logging by module name alone so transitive suppliers
		// (e.g. jasperreports) cannot reintroduce it; jcl-over-slf4j provides the
		// org.apache.commons.logging API instead (issue #154).
		exclude(module = "commons-logging")
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

    if (name != "micrproject_contrib" && name != "micrproject_bootstrap") {
        projectLibreMavenDependencyAliases.forEach { alias ->
            dependencies.add("implementation", versionCatalog.findLibrary(alias).get())
        }
    }
}

tasks.register("stageAppDist") {
    group = "distribution"
    description = "Builds the installable application layout for microProject."
    dependsOn(":micrproject_ui:installDist")
}

tasks.register("verifyIndependentBoundaries") {
    group = "verification"
    description = "Verifies that reports and exchange remain independent of UI and application layers."

    doLast {
        val boundaryRules = mapOf(
            "micrproject_reports" to listOf("com.microproject.exchange", "com.microproject.application", "com.projectlibre.ui"),
            "micrproject_exchange" to listOf("com.microproject.reports", "com.microproject.application", "com.projectlibre.ui")
        )
        boundaryRules.forEach { (module, forbiddenPackages) ->
            val sourceRoot = project(":$module").projectDir.resolve("src/main")
            fileTree(sourceRoot).matching { include("**/*.java", "**/*.kt") }.forEach { sourceFile ->
                sourceFile.useLines { lines ->
                    lines.forEach { line ->
                        if (line.trimStart().startsWith("import ") && forbiddenPackages.any { line.contains(it) }) {
                            throw GradleException("Independent boundary violation in ${sourceFile}: $line")
                        }
                    }
                }
            }
        }
    }
}

val verifyTaskViewArchitecture = tasks.register("verifyTaskViewArchitecture") {
    group = "verification"
    description = "Rejects reintroduction of shared task-view state and direct Gantt mutation paths."
    doLast {
        val ui = project(":micrproject_ui").projectDir.resolve("src/main/java/com/microproject/pm/graphic")
        val graphicNode = ui.resolve("model/cache/GraphicNode.java").readText()
        listOf("ganttShapeOffset", "ganttShapeHeight", "tmpChildren", "tmpFiltered", "pertShape", "xbsShape", "pertLevel")
            .forEach { forbidden ->
                if (graphicNode.contains(forbidden))
                    throw GradleException("Shared GraphicNode view state reintroduced: $forbidden")
            }
        val interactor = ui.resolve("gantt/GanttInteractor.java").readText()
        listOf("DependencyService.getInstance().newDependency(", "ScheduleService.getInstance().setInterval(",
            "ScheduleService.getInstance().setCompleted(", "ScheduleService.getInstance().split(")
            .forEach { forbidden ->
                if (interactor.contains(forbidden))
                    throw GradleException("Direct Gantt mutation path reintroduced: $forbidden")
            }
        val ganttUi = ui.resolve("gantt/GanttUI.java").readText()
        listOf("node.contains(", "node.getStart(", "node.getEnd(", "node.getCompleted(")
            .forEach { forbidden ->
                if (ganttUi.contains(forbidden))
                    throw GradleException("Mutable-node Gantt hit testing reintroduced: $forbidden")
            }
        val selectionGeometry = ui.resolve("gantt/GanttSelectionGeometrySupport.java").readText()
        listOf("import com.microproject.pm.graphic.model.cache.GraphicNode", "import com.microproject.pm.task.Task")
            .forEach { forbidden ->
                if (selectionGeometry.contains(forbidden))
                    throw GradleException("Selection geometry must use immutable projection rows: $forbidden")
            }
        val viewCache = ui.resolve("model/cache/ViewNodeModelCache.java").readText()
        listOf("reference.createDependency(", "reference.createHierarchyDependency(")
            .forEach { forbidden ->
                if (viewCache.contains(forbidden))
                    throw GradleException("Task command gateway bypass reintroduced: $forbidden")
            }
        val spreadsheetModel = ui.resolve("spreadsheet/SpreadSheetModel.java").readText()
        if (spreadsheetModel.contains("DependencyService.getInstance().setFields("))
            throw GradleException("Spreadsheet dependency updates must use TaskCommandGateway")
		val ganttRenderer = ui.resolve("gantt/GanttRenderer.java").readText()
		listOf("GraphicNode", "GraphicDependency", "getVisibleDependencies()", ".getNode()")
			.forEach { forbidden ->
				if (ganttRenderer.contains(forbidden))
					throw GradleException("Gantt paint must use value snapshots: $forbidden")
			}
        val session = ui.resolve("views/TaskViewSession.java").readText()
        if (Regex("(?m)^\\s*(public|protected|private)?\\s*static\\s+(?!final)").containsMatchIn(session))
            throw GradleException("TaskViewSession must not contain a mutable static registry")
        val workspaceFiles = listOf(
            ui.resolve("views/GanttView.java"),
            ui.resolve("spreadsheet/common/CommonSpreadSheet.java")
        )
        workspaceFiles.forEach { source ->
            val text = source.readText()
            listOf("domainRevision", "topologyRevision", "GraphicNode").forEach { forbidden ->
                if (text.contains("class Workspace") && text.substring(text.indexOf("class Workspace")).contains(forbidden))
                    throw GradleException("Runtime projection state must not be persisted by Workspace: ${source.name}: $forbidden")
            }
        }
    }
}

tasks.named("check") {
    dependsOn(verifyTaskViewArchitecture)
}

tasks.register<Delete>("cleanLegacyPackagingArtifacts") {
    group = "build"
    description = "Removes generated legacy packaging artifacts that are not part of the Gradle source of truth."
    delete(layout.projectDirectory.dir("isolated-build"))
}

val releaseVersion = project.version.toString()
val releaseLabel = "v$releaseVersion"
val applicationVendor = "microProject contributors"
val applicationDescription = "microProject desktop project management software"
val applicationCopyright = "Copyright © 2026 microProject contributors"
val windowsReleaseRoot = layout.buildDirectory.dir("releases/$releaseLabel")
val windowsJpackageInput = windowsReleaseRoot.map { it.dir("jpackage-input") }
val windowsAppImageDir = windowsReleaseRoot.map { it.dir("app-image") }
val windowsMsiDir = windowsReleaseRoot.map { it.dir("msi") }
val windowsExeDir = windowsReleaseRoot.map { it.dir("exe") }
val docsDownloadsDir = layout.projectDirectory.dir("docs/downloads")
val windowsFileAssociationsDir = layout.projectDirectory.dir("packaging/windows/file-associations")
val windowsInstallerResourcesDir = layout.projectDirectory.dir("packaging/windows/installer-resources")
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
    dependsOn(":micrproject_ui:installDist")

    val installLibDir = project(":micrproject_ui").layout.buildDirectory.dir("install/micrproject_ui/lib")
    val iconFile = layout.projectDirectory.file("packaging/windows/icons/microproject.ico")
    val licenseFile = layout.projectDirectory.file("packaging/licenses/license.txt")

    from(installLibDir)
    from(iconFile) {
        rename { "microproject.ico" }
    }
    from(licenseFile) {
        rename { "license.txt" }
    }
    from(windowsFileAssociationsDir)
    into(windowsJpackageInput)

    doLast {
        val inputDir = windowsJpackageInput.get().asFile
        val iconPath = File(inputDir, "microproject.ico").absolutePath.replace('\\', '/')
        listOf("mpp.properties", "mpo.properties", "pod.properties", "xml.properties").forEach { name ->
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
            "--name", "microProject",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "micrproject_ui.jar",
            "--main-class", "com.microproject.main.Main",
            "--icon", File(inputDir, "microproject.ico").absolutePath,
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
            "--name", "microProject",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "micrproject_ui.jar",
            "--main-class", "com.microproject.main.Main",
            "--icon", File(inputDir, "microproject.ico").absolutePath,
            "--license-file", File(inputDir, "license.txt").absolutePath,
            "--resource-dir", windowsInstallerResourcesDir.asFile.absolutePath,
            "--add-modules", windowsRuntimeModules.joinToString(","),
            "--jlink-options", "--strip-native-commands --strip-debug --no-man-pages --no-header-files --compress zip-9",
            "--dest", windowsMsiDir.get().asFile.absolutePath,
            "--file-associations", File(inputDir, "mpp.properties").absolutePath,
            "--file-associations", File(inputDir, "mpo.properties").absolutePath,
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
            "--name", "microProject",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "micrproject_ui.jar",
            "--main-class", "com.microproject.main.Main",
            "--icon", File(inputDir, "microproject.ico").absolutePath,
            "--license-file", File(inputDir, "license.txt").absolutePath,
            "--resource-dir", windowsInstallerResourcesDir.asFile.absolutePath,
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
    dependsOn(":micrproject_ui:installDist", ":micrproject_ui:compileTestJava")

    val uiSourceSets = project(":micrproject_ui").extensions.getByType<SourceSetContainer>()
    val uiTestOutput = uiSourceSets.named("test").map { it.output }
    val uiTestRuntimeClasspath = uiSourceSets.named("test").map { it.runtimeClasspath }

    classpath = files(uiTestOutput, uiTestRuntimeClasspath)
    mainClass.set("com.microproject.integration.PackagedImportSmokeMain")
    args(
        "--windows-script",
        file("modules/micrproject_ui/build/install/micrproject_ui/bin/micrproject_ui.bat").absolutePath,
        file("samples/Commercial construction project plan.mpp").absolutePath,
        file("samples/Commercial construction project plan.pod").absolutePath
    )
    jvmArgs("--limit-modules", windowsRuntimeModules.joinToString(","))
}

tasks.register<Zip>("packageWindowsZip") {
    group = "distribution"
    description = "Archives the Windows app-image as a downloadable ZIP."
    dependsOn("packageWindowsAppImage")

    from(windowsAppImageDir.map { it.dir("microProject") })
    archiveFileName.set("microProject-$releaseVersion-app-image.zip")
    destinationDirectory.set(docsDownloadsDir)

    doFirst {
        delete(docsDownloadsDir.file("microProject-$releaseVersion-app-image.zip"))
    }
}

tasks.register<Copy>("publishWindowsMsiToDocs") {
    group = "distribution"
    description = "Copies the Windows MSI into docs/downloads for GitHub Pages."
    dependsOn("packageWindowsMsi")

    from(windowsMsiDir.map { it.file("microProject-$releaseVersion.msi") })
    into(docsDownloadsDir)
}

tasks.register("publishSplitExeToDocs") {
    group = "distribution"
    description = "Splits the self-contained Windows EXE into GitHub-safe download parts and publishes them into docs/downloads."
    dependsOn("packageWindowsExe")

    doLast {
        val exeFile = windowsExeDir.get().file("microProject-$releaseVersion.exe").asFile
        val downloadsDir = docsDownloadsDir.asFile
        val partSize = 95L * 1024L * 1024L
        val baseName = "microProject-$releaseVersion.exe"
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
