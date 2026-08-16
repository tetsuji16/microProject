dependencies {
    implementation(project(":micrproject_contrib"))
    // slf4j-simple is the logging backend. commons-logging 1.3.x routes through slf4j;
    // log4j2/logback are excluded project-wide (see root build.gradle.kts) because they
    // recurse via StackWalker on modern JDKs and break Configuration.getInstance()/.pod load.
    implementation(libs.slf4j.simple)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
    systemProperty("projectlibre.test.releaseVersion", rootProject.version.toString())
}

tasks.processResources {
    inputs.property("releaseVersion", rootProject.version.toString())
    filesMatching("com/microproject/version/version.properties") {
        expand("version" to rootProject.version.toString())
    }
}
