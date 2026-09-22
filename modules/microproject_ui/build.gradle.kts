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
	// UI owns these legacy Swing libraries explicitly; they are no longer
	// injected into every subproject by the root build.
	implementation(libs.commons.beanutils)
	implementation(libs.commons.collections)
	implementation(libs.commons.collections4)
	implementation(libs.commons.digester)
	implementation(libs.commons.lang)
	implementation(libs.commons.lang3)
	implementation(libs.jcl.over.slf4j)
	implementation(libs.commons.pool)
	implementation(libs.forms)
	implementation(libs.flatlaf)
	implementation(libs.flatlaf.extras)
	implementation(libs.groovy)
	implementation(libs.openpdf)
	implementation(libs.jfreechart)
	implementation(libs.org.netbeans.swing.outline)
	implementation(libs.radiance.neon)
	implementation(libs.radiance.trident)
	implementation(libs.flamingo)
	implementation(libs.javax.activation.api)
	implementation(libs.javax.jaxb.api)
	implementation(libs.jaxb.runtime)
	implementation(libs.jackson.annotations)
	implementation(libs.jackson.core)
	implementation(libs.jackson.databind)
	implementation(libs.jasperreports)
	implementation(libs.pdfbox)
	implementation(libs.poi)
	implementation(libs.poi.ooxml)
	implementation(libs.slf4j.api)
	implementation(libs.slf4j.simple)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.microproject.main.Main")
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
	val guiSmokeTestPatterns = listOf(
		"com.microproject.pm.graphic.spreadsheet.common.U26SpreadsheetInputTransactionGuiAcceptanceTest.robotTypesDurationAndPercentAsOneInputTransaction",
		"com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalTaskTableDurationEditDoesNotPanGanttViewport",
		"com.microproject.pm.graphic.views.TaskTableGanttGridGuiAcceptanceTest.physicalTaskTableDateEditRepositionsBarWithoutPanningGanttViewport",
		"com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotClickOnTaskPropertiesInformationOpensTaskInformation",
		"com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotOpensIssue590DialogsWithoutClippedText",
		"com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotCalendarOptionsRibbonRouteOpensUsableDialog",
		"com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotCalendarCommandOpensUsableCalendarDialog",
		"com.microproject.dialog.calendar.ChangeWorkingTimeDialogGuiAcceptanceTest.robotOpensWorkingTimeDialogAndCancelsWithoutCommit",
		"com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.robotNetworkAndWbsRibbonRoutesRenderTheirDedicatedViews",
		"com.microproject.pm.graphic.frames.TaskInformationRibbonGuiAcceptanceTest.indentAndOutdentSelectedTaskThroughRibbonRoundTripsHierarchy",
		"com.microproject.pm.graphic.frames.RibbonExternalCommandGuiAcceptanceTest.robotInvokesRealFileRibbonCommandsAndOpensTheirDialogs",
		"com.microproject.dialog.ProjectInformationDialogGuiAcceptanceTest.projectInformationShowsAllTabsAndButtonsAfterResize",
		"com.microproject.dialog.assignment.AssignmentDialogGuiAcceptanceTest.robotReplaceWithActualWorkPreservesActualsAndSupportsUndoRedo"
	)
	inputs.property("guiTestSuite", guiTestSuite)
	if (guiTestSuite == "smoke") {
		filter {
			guiSmokeTestPatterns.forEach(::includeTestsMatching)
		}
	} else require(guiTestSuite == "full") {
		"guiTestSuite must be 'full' or 'smoke'"
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
    inputs.property("guiTestLocale", guiTestLocale)
    inputs.property("guiTestUiScale", guiTestUiScale ?: "default")
    systemProperty("user.language", guiTestLocale)
    systemProperty("user.country", if (guiTestLocale == "ja") "JP" else "US")
    if (guiTestUiScale != null)
        systemProperty("sun.java2d.uiScale", guiTestUiScale)
    systemProperty("microproject.gui.artifacts.dir", layout.buildDirectory.dir("reports/guiTest-artifacts").get().asFile.absolutePath)
	// GUI tests run from the generated install layout, not the repository root.
	// Expose the fixture root explicitly so sample-file acceptance routes test
	// the same release classpath without relying on a process working directory.
	systemProperty("microproject.project.dir", rootProject.projectDir.absolutePath)
    mustRunAfter(tasks.test)
}
