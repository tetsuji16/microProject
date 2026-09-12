import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.bundling.Zip
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import java.io.File
import java.util.zip.ZipFile

fun architectureSourceBoundaryViolations(sourceFile: File, forbiddenPackages: List<String>): List<String> {
    return sourceFile.readLines().mapIndexedNotNull { index, line ->
        val code = line.substringBefore("//")
        forbiddenPackages.firstOrNull { forbidden ->
            Regex("(?<![A-Za-z0-9_$])${Regex.escape(forbidden)}(?:[A-Za-z0-9_$.]*)(?![A-Za-z0-9_$])")
                .containsMatchIn(code)
        }?.let { forbidden ->
            "${sourceFile}:${index + 1}: forbidden package '$forbidden': $line"
        }
    }
}

fun requireArchitectureSet(label: String, module: String, expected: Set<String>, actual: Set<String>) {
    require(actual == expected) {
        "Unexpected $label for $module: expected=${expected.sorted()}, actual=${actual.sorted()}"
    }
}

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

}

tasks.register("stageAppDist") {
    group = "distribution"
    description = "Builds the installable application layout for microProject."
    dependsOn(":micrproject_ui:installDist")
}

tasks.register("verifyArchitectureBoundaries") {
    group = "verification"
    description = "Verifies module dependencies, API exposure, and namespace boundaries."

    doLast {
        val expectedProjectDependencies = mapOf<String, Set<String>>(
            "micrproject_contrib" to emptySet(),
            "micrproject_core" to setOf("micrproject_contrib"),
            "micrproject_application" to setOf("micrproject_core"),
            "micrproject_exchange" to setOf("micrproject_contrib", "micrproject_core"),
            "micrproject_reports" to setOf("micrproject_contrib", "micrproject_core"),
            "micrproject_bootstrap" to emptySet(),
            "micrproject_ribbon" to emptySet(),
            "micrproject_ui" to setOf(
                "micrproject_application", "micrproject_contrib", "micrproject_core",
                "micrproject_exchange", "micrproject_reports", "micrproject_ribbon"
            )
        )
        val configuredModules = rootProject.subprojects.map { it.name }.toSet()
        require(configuredModules == expectedProjectDependencies.keys) {
            "Architecture allowlist must cover exactly the configured micrproject modules. " +
                "configured=${configuredModules.sorted()}, " +
                "allowlisted=${expectedProjectDependencies.keys.sorted()}"
        }
        expectedProjectDependencies.forEach { (module, expected) ->
            val actual = project(":$module").configurations
                .filter { it.name in setOf("api", "implementation", "compileOnly", "runtimeOnly") }
                .flatMap { configuration -> configuration.dependencies }
                .filter { dependency -> dependency is org.gradle.api.artifacts.ProjectDependency }
                .map { dependency -> dependency as org.gradle.api.artifacts.ProjectDependency }
                .map { dependency -> dependency.path.removePrefix(":") }
                .toSet()
            requireArchitectureSet("project dependency graph", module, expected, actual)
        }

        val allowedApiDependencies = mapOf<String, Set<String>>(
            // contrib exposes only com.microproject.contrib/org.jdesktop APIs;
            // third-party implementation details must not leak to consumers.
            "micrproject_contrib" to emptySet(),
            "micrproject_core" to emptySet(),
            "micrproject_application" to emptySet(),
            "micrproject_exchange" to emptySet(),
            "micrproject_reports" to emptySet(),
            "micrproject_bootstrap" to emptySet(),
            "micrproject_ribbon" to emptySet(),
            "micrproject_ui" to emptySet()
        )
        allowedApiDependencies.forEach { (module, allowed) ->
            val actual = project(":$module").configurations
                .findByName("api")
                ?.dependencies
                ?.map { dependency ->
                    if (dependency is org.gradle.api.artifacts.ProjectDependency) {
                        dependency.path
                    } else {
                        "${dependency.group}:${dependency.name}"
                    }
                }
                ?.toSet()
                ?: emptySet()
            requireArchitectureSet("public API dependencies", module, allowed, actual)
        }

        val boundaryRules = mapOf(
            "micrproject_core" to listOf("com.microproject.application", "com.microproject.reports", "com.microproject.ui"),
            "micrproject_application" to listOf("com.microproject.exchange", "com.microproject.reports", "com.microproject.ui"),
            "micrproject_reports" to listOf("com.microproject.application", "com.microproject.exchange", "com.microproject.ui"),
            "micrproject_exchange" to listOf("com.microproject.application", "com.microproject.reports", "com.microproject.ui"),
            "micrproject_bootstrap" to listOf("com.microproject.application", "com.microproject.core", "com.microproject.exchange", "com.microproject.reports", "com.microproject.ui"),
            "micrproject_ribbon" to listOf("com.microproject.application", "com.microproject.core", "com.microproject.exchange", "com.microproject.menu", "com.microproject.pm", "com.microproject.reports", "com.microproject.ui", "com.microproject.util")
        )

        boundaryRules.forEach { (module, forbiddenPackages) ->
            val sourceRoot = project(":$module").projectDir.resolve("src/main")
            fileTree(sourceRoot).matching { include("**/*.java", "**/*.kt") }.forEach { sourceFile ->
                architectureSourceBoundaryViolations(sourceFile, forbiddenPackages).forEach { violation ->
                    throw GradleException("Independent boundary violation in $violation")
                }
            }
        }

        // A legacy class-name key is persisted in old POD options.  Keep this
        // compatibility adapter narrow and explicit; all other legacy FQNs are
        // still rejected, including new reflection/package leaks.
        val legacyNamespaceAllowlist = mapOf(
            "modules/micrproject_core/src/main/java/com/microproject/util/SafeObjectInput.java" to setOf("com.projectlibre1"),
            "modules/micrproject_exchange/src/main/java/com/microproject/exchange/DefaultFileImporterProvider.java" to setOf(
                "com.projectlibre1.exchange.LocalFileImporter", "com.projectlibre.exchange.LocalFileImporter"
            ),
            "modules/micrproject_core/src/main/java/com/microproject/exchange/ImporterRegistry.java" to setOf(
                "com.projectlibre1.exchange.LocalFileImporter", "com.projectlibre.exchange.LocalFileImporter"
            )
        )
        val legacyFqnPattern = Regex("(?<![A-Za-z0-9_$])com\\.projectlibre(?:1)?(?:\\.[A-Za-z0-9_$]+)*(?![A-Za-z0-9_$])")
        val legacyNamespaceReferences = fileTree(layout.projectDirectory.dir("modules")) {
            include("**/src/main/**/*.java", "**/src/main/**/*.kt")
            exclude("**/build/**")
        }.flatMap { sourceFile ->
            val relative = sourceFile.relativeTo(layout.projectDirectory.asFile).invariantSeparatorsPath
            val allowed = legacyNamespaceAllowlist[relative].orEmpty()
            sourceFile.readLines().flatMapIndexed { index, line ->
                legacyFqnPattern.findAll(line.substringBefore("//")).mapNotNull { match ->
                    val token = match.value
                    if (allowed.any { token == it || (it == "com.projectlibre1" && token.startsWith("$it.")) }) {
                        null
                    } else {
                        "${sourceFile}:${index + 1}: $line"
                    }
                }.toList()
            }
        }
        require(legacyNamespaceReferences.isEmpty()) {
            "Legacy com.projectlibre namespace leaked outside the explicit compatibility allowlist:\n" + legacyNamespaceReferences.joinToString("\n")
        }
    }
}

tasks.register("verifyDependencyAllowlist") {
    group = "verification"
    description = "Checks the approved external dependency and single logging-backend policy."
    doLast {
        // This is intentionally a group-level allowlist: versions are managed
        // centrally in libs.versions.toml, while this gate prevents a module
        // from quietly introducing an unrelated public/runtime dependency.
        val allowedGroups = setOf(
            "com.fasterxml.jackson.core", "com.fasterxml.jackson.dataformat",
            "com.formdev", "com.jgoodies", "com.lowagie", "com.thoughtworks.xstream",
            "commons-beanutils", "commons-codec", "commons-collections",
            "commons-io", "commons-lang", "commons-logging", "commons-pool", "commons-digester",
            "io.reactivex.rxjava3", "javax.activation", "javax.xml.bind",
            "net.sf.mpxj", "net.sf.jasperreports", "org.apache.commons",
            "org.apache.logging.log4j", "org.apache.pdfbox", "org.apache.poi",
            "org.codehaus.groovy", "org.glassfish.jaxb", "org.imgscalr", "org.jfree", "org.slf4j", "org.pushingpixels", "org.pushing-pixels",
            "org.update4j", "com.github.librepdf",
            "org.netbeans.api", "org.junit", "junit", "com.github"
        )
        subprojects.forEach { module ->
            module.configurations.findByName("implementation")?.dependencies?.forEach { dependency ->
                if (dependency is org.gradle.api.artifacts.ExternalModuleDependency) {
                    require(dependency.group in allowedGroups) {
                        "Dependency ${dependency.group}:${dependency.name} in ${module.name} is not in the external allowlist"
                    }
                }
            }
        }

        val forbiddenBackends = setOf("ch.qos.logback:logback-classic", "org.apache.logging.log4j:log4j-core")
        subprojects.forEach { module ->
            val runtime = module.configurations.findByName("runtimeClasspath") ?: return@forEach
            val resolved = runtime.resolvedConfiguration.resolvedArtifacts.map {
                "${it.moduleVersion.id.group}:${it.name}"
            }.toSet()
            require(resolved.intersect(forbiddenBackends).isEmpty()) {
                "Forbidden logging backend resolved for ${module.name}: ${resolved.intersect(forbiddenBackends)}"
            }
            val simpleBackends = resolved.filter { it == "org.slf4j:slf4j-simple" }
            require(simpleBackends.size <= 1) { "Duplicate slf4j-simple backend in ${module.name}" }
        }
    }
}

tasks.register("verifyArchitectureBoundaryFixtures") {
    group = "verification"
    description = "Self-tests the source-level architecture boundary detector with violating fixtures."

    doLast {
        val fixtureRoot = layout.buildDirectory.dir("tmp/architecture-boundary-fixtures").get().asFile
        fixtureRoot.deleteRecursively()
        fixtureRoot.mkdirs()
        val fixture = fixtureRoot.resolve("ForbiddenImport.java")
        try {
            fixture.writeText(
                "package com.microproject.core;\n" +
                    "import com.microproject.exchange.MpoFileImporter;\n" +
                    "class ForbiddenImport { Object load() { return Class.forName(\"com.microproject.exchange.MpoFileImporter\"); } }\n"
            )
            val detected = architectureSourceBoundaryViolations(fixture, listOf("com.microproject.exchange"))
            require(detected.size == 2) {
                "Architecture boundary fixture must reject both direct and reflective FQNs; detected=$detected"
            }

            var projectDependencyRejected = false
            try {
                requireArchitectureSet("project dependency graph", "fixture", emptySet(), setOf("micrproject_exchange"))
            } catch (_: IllegalArgumentException) {
                projectDependencyRejected = true
            }
            require(projectDependencyRejected) { "Architecture fixture must reject an undeclared project dependency." }

            var apiDependencyRejected = false
            try {
                requireArchitectureSet("public API dependencies", "fixture", emptySet(), setOf("org.bad:leak"))
            } catch (_: IllegalArgumentException) {
                apiDependencyRejected = true
            }
            require(apiDependencyRejected) { "Architecture fixture must reject an API dependency leak." }
        } finally {
            fixtureRoot.deleteRecursively()
        }
    }
}

tasks.register("verifyIndependentBoundaries") {
    group = "verification"
    description = "Compatibility alias for verifyArchitectureBoundaries."
    dependsOn("verifyArchitectureBoundaries")
}

tasks.register("verifyNamingConventions") {
    group = "verification"
    description = "Verifies the micrproject build identity, microProject product branding, and source namespace policy."

    doLast {
        // The Gradle identity is deliberately lower-case and keeps the historical
        // micrproject spelling.  It is not the user-visible product name.
        val expectedModules = setOf(
            "micrproject_contrib",
            "micrproject_core",
            "micrproject_application",
            "micrproject_ui",
            "micrproject_exchange",
            "micrproject_reports",
            "micrproject_bootstrap",
            "micrproject_ribbon"
        )
        val configuredModules = rootProject.subprojects.map { it.name }.toSet()
        require(configuredModules == expectedModules) {
            "Naming policy requires exactly the eight micrproject_* modules. " +
                "configured=${configuredModules.sorted()}, expected=${expectedModules.sorted()}"
        }
        require(rootProject.name == "micrproject") {
            "The Gradle root identity must remain 'micrproject'; use 'microProject' only for product branding."
        }
        // Issue #529: the logical project name and the on-disk project directory
        // are both part of the build contract. This catches a partially renamed
        // module while allowing unreferenced legacy folders to remain for audit.
        val modulesRoot = layout.projectDirectory.dir("modules").asFile.canonicalFile
        val expectedProjectDirs = expectedModules.associateWith { module ->
            modulesRoot.resolve(module).canonicalFile
        }
        val actualProjectDirs = expectedModules.associateWith { module ->
            project(":$module").projectDir.canonicalFile
        }
        require(actualProjectDirs == expectedProjectDirs) {
            "Each active module must live at modules/<micrproject_* name>; " +
                "actual=${actualProjectDirs.mapValues { it.value.path }}, " +
                "expected=${expectedProjectDirs.mapValues { it.value.path }}"
        }
        expectedProjectDirs.forEach { (module, directory) ->
            require(directory.isDirectory) {
                "Active module directory is missing for $module: $directory"
            }
        }
        val legacyProjectDirs = modulesRoot.listFiles()
            ?.filter { it.isDirectory && it.name.startsWith("projectlibre_") }
            .orEmpty()
            .map { it.canonicalFile }
            .toSet()
        require(legacyProjectDirs.intersect(actualProjectDirs.values.toSet()).isEmpty()) {
            "Legacy projectlibre_* directories must never be active Gradle projects: " +
                legacyProjectDirs.intersect(actualProjectDirs.values.toSet())
        }
        expectedModules.forEach { module ->
            val artifactName = project(":$module").tasks.named<Jar>("jar").get().archiveBaseName.get()
            require(artifactName == module) {
                "The $module JAR must retain its stable micrproject_* artifact name; actual=$artifactName"
            }
        }

        val settingsText = layout.projectDirectory.file("settings.gradle.kts").asFile.readText()
        require(!Regex("include\\(\\\"projectlibre_").containsMatchIn(settingsText)) {
            "Legacy projectlibre_* modules must not be reintroduced into settings.gradle.kts."
        }

        // Package declarations are the source-namespace boundary.  Both legacy
        // ProjectLibre and casing-variant microProject/micrproject declarations
        // are rejected. References to old names in compatibility strings, file
        // formats, or SafeObjectInput are intentionally not treated as a violation.
        val forbiddenDeclarations = mutableListOf<String>()
        expectedModules.forEach { module ->
            listOf("src/main/java", "src/test/java").forEach { sourcePath ->
                val sourceRoot = layout.projectDirectory.dir("modules/$module/$sourcePath").asFile
                if (!sourceRoot.isDirectory) return@forEach
                fileTree(sourceRoot).matching { include("**/*.java", "**/*.kt") }.files.forEach { sourceFile ->
                    sourceFile.readLines().forEachIndexed { index, line ->
                        val declaration = line.trim()
                        if (declaration.startsWith("package com.projectlibre") ||
                            declaration.startsWith("package org.projectlibre") ||
                            declaration.startsWith("package com.microProject") ||
                            declaration.startsWith("package com.micrproject")) {
                            forbiddenDeclarations += "${sourceFile}:${index + 1}: $declaration"
                        }
                    }
                }
            }
        }
        require(forbiddenDeclarations.isEmpty()) {
            "Legacy ProjectLibre package declarations are not allowed in active sources:\n" +
                forbiddenDeclarations.joinToString("\n")
        }

        require(minimumJavaRelease >= 25 && activeToolchainVersion >= 25) {
            "Active Java/toolchain baseline must remain Java 25 or newer."
        }
        val packagingBuild = layout.projectDirectory.file("build.gradle.kts").asFile.readText()
        require(packagingBuild.contains("--name\", \"microProject\"")) {
            "Windows packaging must use the user-visible microProject application name."
        }
        require(layout.projectDirectory.file("packaging/windows/launchers/microProject.cmd").asFile.isFile) {
            "The canonical microProject Windows launcher is missing."
        }
        require(layout.projectDirectory.file("packaging/windows/icons/microproject.ico").asFile.isFile) {
            "The canonical microproject Windows icon is missing."
        }
    }
}

tasks.named("check") {
    dependsOn("verifyArchitectureBoundaries", "verifyArchitectureBoundaryFixtures", "verifyNamingConventions")
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
val windowsPortableLauncher = layout.projectDirectory.file("packaging/windows/launchers/microProject.cmd")
val windowsFileAssociationsDir = layout.projectDirectory.dir("packaging/windows/file-associations")
val windowsInstallerResourcesDir = layout.projectDirectory.dir("packaging/windows/installer-resources")
val update4jPrivateKeyFile = providers.environmentVariable("UPDATE4J_PRIVATE_KEY_FILE")
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
    dependsOn(":micrproject_ui:installDist", ":micrproject_bootstrap:jar")

    val installLibDir = project(":micrproject_ui").layout.buildDirectory.dir("install/micrproject_ui/lib")
    val iconFile = layout.projectDirectory.file("packaging/windows/icons/microproject.ico")
    val licenseFile = layout.projectDirectory.file("packaging/licenses/license.txt")

    from(installLibDir)
    from(project(":micrproject_bootstrap").tasks.named<Jar>("jar"))
    from(project(":micrproject_bootstrap").configurations.named("runtimeClasspath"))
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
            "--main-jar", "micrproject_bootstrap.jar",
            "--main-class", "com.microproject.bootstrap.MicroProjectUpdater",
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
        commandLine(
            File(javaHome, "bin/jpackage.exe").absolutePath,
            "--type", "msi",
            "--name", "microProject",
            "--app-version", releaseVersion,
            "--vendor", applicationVendor,
            "--description", applicationDescription,
            "--copyright", applicationCopyright,
            "--input", inputDir.absolutePath,
            "--main-jar", "micrproject_bootstrap.jar",
            "--main-class", "com.microproject.bootstrap.MicroProjectUpdater",
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
    from(windowsPortableLauncher)
    archiveFileName.set("microProject-$releaseVersion-app-image.zip")
    destinationDirectory.set(docsDownloadsDir)

    doFirst {
        delete(docsDownloadsDir.file("microProject-$releaseVersion-app-image.zip"))
    }

    doLast {
        val archive = archiveFile.get().asFile
        ZipFile(archive).use { zip ->
            require(zip.getEntry("microProject.exe") != null) {
                "Portable ZIP is missing microProject.exe: $archive"
            }
            require(zip.getEntry("microProject.cmd") != null) {
                "Portable ZIP is missing microProject.cmd: $archive"
            }
        }
    }
}

tasks.register<JavaExec>("generateUpdateConfiguration") {
    group = "distribution"
    description = "Generates a signed update4j configuration for the staged Windows app image."
    dependsOn("packageWindowsAppImage", ":micrproject_bootstrap:classes")

    val bootstrapSourceSet = project(":micrproject_bootstrap")
        .extensions.getByType<SourceSetContainer>().named("main")
    classpath = bootstrapSourceSet.get().runtimeClasspath
    mainClass.set("com.microproject.bootstrap.ConfigurationGenerator")
    onlyIf {
        val keyFile = update4jPrivateKeyFile.orNull
        keyFile != null && File(keyFile).isFile
    }
    doFirst {
        val keyFile = update4jPrivateKeyFile.orNull
            ?: throw GradleException("UPDATE4J_PRIVATE_KEY_FILE is required to sign the update manifest")
        args(
            "--app-dir", windowsAppImageDir.get().dir("microProject/app").asFile.absolutePath,
            "--output", windowsReleaseRoot.get().file("release-assets/configuration.xml").asFile.absolutePath,
            "--private-key", keyFile,
            "--version", releaseVersion,
            "--asset-base", "https://github.com/tetsuji16/ProjectLibre/releases/download/$releaseLabel/"
        )
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
