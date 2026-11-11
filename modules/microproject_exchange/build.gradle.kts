dependencies {
    implementation(project(":microproject_contrib"))
    implementation(project(":microproject_core"))
    // MPXJ 11.5.4 still uses the javax.xml.bind namespace; keep a javax-compatible
    // JAXB runtime for it and exclude the jakarta 2.x artifacts it would pull in.
    implementation(libs.mpxj) {
        exclude(group = "jakarta.xml.bind")
        exclude(group = "org.glassfish.jaxb")
    }
    implementation(libs.bundles.jaxb.legacy) {
        exclude(group = "jakarta.xml.bind")
    }
    implementation(libs.bundles.jackson)
    implementation(libs.commons.lang3)
    implementation(libs.bundles.poi)
    // Route MPXJ/POI's Log4j API calls to the existing SLF4J backend instead
    // of emitting "could not find a logging provider" on packaged imports.
    implementation(libs.log4j.to.slf4j)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.junit4)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
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
    }
}
