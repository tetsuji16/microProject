import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.api.tasks.testing.Test

plugins {
    application
}

dependencies {
    implementation(project(":microproject_ribbon"))
    implementation(project(":microproject_contrib"))
    implementation(project(":microproject_core"))
    implementation(project(":microproject_application"))
    implementation(project(":microproject_exchange"))
    implementation(project(":microproject_reports"))
    implementation(libs.commons.csv)
    implementation(libs.imgscalr.lib)
	implementation(libs.commons.collections4)
	// UI uses these string libraries directly; other Commons dependencies come
	// through the core module.
	implementation(libs.commons.lang3)
	implementation(libs.commons.text)
	implementation(libs.forms)
	implementation(libs.flatlaf)
	implementation(libs.flatlaf.extras)
	implementation(libs.openpdf)
	implementation(libs.jfreechart)
	implementation(libs.org.netbeans.swing.outline)
	implementation(libs.radiance.neon)
	implementation(libs.radiance.trident)
	implementation(libs.flamingo)
	implementation(libs.bundles.jaxb)
	implementation(libs.jackson.annotations)
	implementation(libs.jackson.core)
	implementation(libs.jackson.databind)
	implementation(libs.pdfbox)
	implementation(libs.poi)
	implementation(libs.poi.ooxml)
	implementation(libs.slf4j.api)
	implementation(libs.slf4j.simple)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.microproject.main.Main")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        val scriptDir = requireNotNull(outputDir) { "startScripts outputDir must be configured" }
        val scriptName = requireNotNull(applicationName) { "startScripts applicationName must be configured" }
        val candidates = listOf(scriptName, "$scriptName.bat")
        for (candidate in candidates) {
            val scriptFile = scriptDir.resolve(candidate)
            if (!scriptFile.isFile) continue
            val original = scriptFile.readText(Charsets.UTF_8)
            val isWindows = candidate.endsWith(".bat")
            val prefix = if (isWindows) "%APP_HOME%\\lib\\" else "\$APP_HOME/lib/"
            // microproject_ui.jar contains the compatibility DefaultFormBuilder.
            // It must precede the bundled JGoodies jars, which expose an older
            // binary-incompatible implementation of the same class.
            val separator = if (isWindows) ";" else ":"
            val classpathValue = "${prefix}microproject_ui.jar${separator}${prefix}jgoodies-forms-1.9.0.jar${separator}${prefix}*"
            val updated = original.replace(Regex("""(?m)^set CLASSPATH=.*$""")) {
                "set CLASSPATH=$classpathValue"
            }.replace(Regex("""(?m)^CLASSPATH=.*$""")) {
                "CLASSPATH=$classpathValue"
            }
            if (updated != original) {
                scriptFile.writeText(updated, Charsets.UTF_8)
            }
        }
    }
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("packaging/licenses")) {
        into("license")
    }
}

tasks.register<JavaExec>("imeSandbox") {
    group = "application"
    description = "Launch the spreadsheet IME sandbox."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.microproject.pm.graphic.spreadsheet.common.SpreadsheetImeSandbox")
    dependsOn(tasks.classes)
}

tasks.test {
    enabled = true
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

val guiTestSourceSet = sourceSets.create("guiTest") {
    java.srcDir("src/guiTest/java")
    resources.srcDir("src/guiTest/resources")
    compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}

configurations.named(guiTestSourceSet.implementationConfigurationName) {
    extendsFrom(configurations.testImplementation.get())
}
configurations.named(guiTestSourceSet.runtimeOnlyConfigurationName) {
    extendsFrom(configurations.testRuntimeOnly.get())
}

tasks.register<Test>("guiTest") {
    group = "verification"
    description = "Runs non-headless Swing acceptance tests after regenerating installDist."
    enabled = true
    dependsOn(tasks.installDist)
    testClassesDirs = guiTestSourceSet.output.classesDirs
	// The installed desktop launcher intentionally puts microproject_ui.jar before
	// jgoodies-forms so the bundled DefaultFormBuilder compatibility shim wins.
	// Keep the GUI acceptance runtime in that same order; otherwise dialog tests
	// exercise the incompatible library class rather than the shipped application.
	classpath = files(tasks.jar).plus(guiTestSourceSet.runtimeClasspath)
	useJUnitPlatform()
	val guiTestSuite = providers.gradleProperty("guiTestSuite").orElse("full").get()
	inputs.property("guiTestSuite", guiTestSuite)
	val guiTestJavaDir = file("src/guiTest/java")
	val journeyAnnotation = Regex("""@GuiJourney\(InputMechanism\.(\w+)\)""")
	val journeyJavaFiles = fileTree(guiTestJavaDir) { include("**/*.java") }.files
	val journeySource = file("src/guiTest/java/com/microproject/testsupport/GuiJourney.java").readText()
	val mechanismEnumBody = Regex("""enum InputMechanism\s*\{([^}]*)\}""")
		.find(journeySource)?.groupValues?.get(1)
		?: error("Could not find GuiJourney.InputMechanism in GuiJourney.java")
	val declaredMechanisms = mechanismEnumBody.lineSequence()
		.mapNotNull { Regex("""^\s*([A-Z][A-Z0-9_]*)\s*[,;]?\s*$""").find(it)?.groupValues?.get(1) }
		.toList()
	val annotatedMechanisms = journeyJavaFiles.flatMap { source ->
		journeyAnnotation.findAll(source.readText()).map { it.groupValues[1] }.toList()
	}
	val mechanismCounts = annotatedMechanisms.groupingBy { it }.eachCount()
	val missingMechanisms = declaredMechanisms.filter { mechanismCounts[it] == null }
	val duplicateMechanisms = mechanismCounts.filterValues { it > 1 }.keys.sorted()
	val unknownMechanisms = mechanismCounts.keys - declaredMechanisms.toSet()
	require(missingMechanisms.isEmpty() && duplicateMechanisms.isEmpty() && unknownMechanisms.isEmpty()) {
		"Each GuiJourney.InputMechanism must annotate exactly one test. " +
			"Missing: ${missingMechanisms.sorted()}; duplicate: $duplicateMechanisms; " +
			"unknown: ${unknownMechanisms.sorted()}"
	}
	if (guiTestSuite == "smoke" || guiTestSuite == "journeys") {
		// smoke remains the workflow-compatible name for the per-input journey suite.
		val journeyClasses = journeyJavaFiles
			.filter { it.readText().contains("@GuiJourney(") }
			.map { it.relativeTo(guiTestJavaDir).invariantSeparatorsPath.removeSuffix(".java") + ".class" }
		require(journeyClasses.isNotEmpty()) { "No @GuiJourney tests found under $guiTestJavaDir" }
		include(journeyClasses)
		useJUnitPlatform { includeTags("gui-journey") }
	} else require(guiTestSuite == "full") {
		"guiTestSuite must be 'full', 'smoke' or 'journeys'"
	}
	// This two-invocation acceptance case uses a shared temp workspace and a fresh
	// Gradle test worker per phase. Keep it out of ordinary full-suite discovery;
	// the dedicated driver supplies the workspace and requires phase markers.
	if (providers.environmentVariable("MICROPROJECT_ISSUE967_WORKSPACE").orNull.isNullOrBlank()) {
		exclude("com/microproject/pm/graphic/frames/FreshProcessMpoRestartGuiAcceptanceTest.class")
	}
	// Swing singletons (locale, menu factories, windows and focus state) are
	// process-wide. One acceptance class may not leave that state behind for
	// another class: doing so turns a real physical-route failure into a
	// test-order accident. Keep each GUI command family in its own JVM.
	forkEvery = 1
	systemProperty("java.awt.headless", "false")
	systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
	// A modal dialog, stale native window, or non-daemon AWT helper must fail
	// the owning acceptance method instead of hanging the Gradle worker.
	systemProperty("junit.jupiter.execution.timeout.mode", "enabled")
	systemProperty("junit.jupiter.execution.timeout.default", "120s")
	testLogging {
		events("started", "passed", "failed", "skipped")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
		showExceptions = true
		showCauses = true
		showStackTraces = true
	}
    val guiTestLocale = providers.gradleProperty("guiTestLocale").orElse("ja").get()
    val guiTestUiScale = providers.gradleProperty("guiTestUiScale").orNull
    val guiTestArtifactsDir = providers.gradleProperty("guiTestArtifactsDir")
        .orElse(layout.buildDirectory.dir("reports/guiTest-artifacts").map { it.asFile.absolutePath }).get()
    val guiTestInvocationId = "${System.currentTimeMillis()}-${ProcessHandle.current().pid()}"
    val guiTestContentionMarker = providers.gradleProperty("guiTestContentionMarker")
        .orElse(layout.buildDirectory.file("reports/guiTest-artifacts/gui-test-contended-$guiTestInvocationId.txt")
            .map { it.asFile.absolutePath }).get()
    inputs.property("guiTestLocale", guiTestLocale)
    inputs.property("guiTestUiScale", guiTestUiScale ?: "default")
    inputs.property("guiTestArtifactsDir", guiTestArtifactsDir)
    inputs.property("guiTestContentionMarker", guiTestContentionMarker)
    systemProperty("user.language", guiTestLocale)
    systemProperty("user.country", if (guiTestLocale == "ja") "JP" else "US")
    if (guiTestUiScale != null)
        systemProperty("sun.java2d.uiScale", guiTestUiScale)
    systemProperty("microproject.gui.artifacts.dir", guiTestArtifactsDir)
	systemProperty("microproject.gui.contentionMarker", guiTestContentionMarker)
	// GUI tests run from the generated install layout, not the repository root.
	// Expose the fixture root explicitly so sample-file acceptance routes test
	// the same release classpath without relying on a process working directory.
	systemProperty("microproject.project.dir", rootProject.projectDir.absolutePath)
	// AutoRecoveryStore resolves %LOCALAPPDATA%. A snapshot left in the real
	// profile opens a modal recovery prompt in every later GUI run and in the
	// user's own application, so each Gradle invocation starts from an empty one.
	val guiTestLocalAppData = layout.buildDirectory.dir("tmp/guiTest/localAppData").get().asFile
	environment("LOCALAPPDATA", guiTestLocalAppData.absolutePath)
	doFirst {
		guiTestLocalAppData.deleteRecursively()
		guiTestLocalAppData.mkdirs()
	}
    mustRunAfter(tasks.test)
}
