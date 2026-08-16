dependencies {
    implementation(project(":micrproject_contrib"))
    implementation(project(":micrproject_core"))
    implementation(libs.mpxj)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.vintage:junit-vintage-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
    systemProperty("java.awt.headless", "true")
    // Force commons-logging to the JDK logger. Auto-discovery of log4j2 / logback
    // triggers a StackWalker recursion (StackOverflowError) on modern JDKs and breaks
    // Configuration.getInstance() (Digester config parse) -> .pod loading. See issue #154.
    systemProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger")
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = true
    }
}
