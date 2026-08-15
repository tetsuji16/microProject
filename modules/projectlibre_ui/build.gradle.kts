import org.gradle.jvm.application.tasks.CreateStartScripts

plugins {
    application
}

dependencies {
    implementation(project(":projectlibre_contrib"))
    implementation(project(":projectlibre_core"))
    implementation(project(":projectlibre_application"))
    implementation(project(":projectlibre_exchange"))
    implementation(project(":projectlibre_reports"))
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
            // projectlibre_ui.jar contains the compatibility DefaultFormBuilder.
            // It must precede the bundled JGoodies jars, which expose an older
            // binary-incompatible implementation of the same class.
            val separator = if (isWindows) ";" else ":"
            val classpathValue = "${prefix}projectlibre_ui.jar${separator}${prefix}jgoodies-forms-1.9.0.jar${separator}${prefix}*"
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
