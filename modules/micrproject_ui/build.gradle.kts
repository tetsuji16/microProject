import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.api.tasks.testing.Test

plugins {
    application
}

dependencies {
    implementation(project(":micrproject_contrib"))
    implementation(project(":micrproject_core"))
    implementation(project(":micrproject_application"))
    implementation(project(":micrproject_exchange"))
    implementation(project(":micrproject_reports"))
    implementation(libs.commons.csv)
    implementation(libs.imgscalr.lib)
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
            // micrproject_ui.jar contains the compatibility DefaultFormBuilder.
            // It must precede the bundled JGoodies jars, which expose an older
            // binary-incompatible implementation of the same class.
            val separator = if (isWindows) ";" else ":"
            val classpathValue = "${prefix}micrproject_ui.jar${separator}${prefix}jgoodies-forms-1.9.0.jar${separator}${prefix}*"
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
	// The installed desktop launcher intentionally puts micrproject_ui.jar before
	// jgoodies-forms so the bundled DefaultFormBuilder compatibility shim wins.
	// Keep the GUI acceptance runtime in that same order; otherwise dialog tests
	// exercise the incompatible library class rather than the shipped application.
	classpath = files(tasks.jar).plus(guiTestSourceSet.runtimeClasspath)
	useJUnitPlatform()
	systemProperty("java.awt.headless", "false")
    systemProperty("micrproject.gui.artifacts.dir", layout.buildDirectory.dir("reports/guiTest-artifacts").get().asFile.absolutePath)
    mustRunAfter(tasks.test)
}
