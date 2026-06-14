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
    mainClass.set("com.projectlibre1.main.Main")
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("projectlibre_build/license")) {
        into("license")
    }
}

tasks.register<JavaExec>("imeSandbox") {
    group = "application"
    description = "Launch the spreadsheet IME sandbox."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.projectlibre1.pm.graphic.spreadsheet.common.SpreadsheetImeSandbox")
    dependsOn(tasks.classes)
}

sourceSets {
    named("test") {
        java.setSrcDirs(listOf("src/test/java"))
        resources.setSrcDirs(listOf("src/test/resources"))
    }
}

tasks.test {
    enabled = true
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
}

