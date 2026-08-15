dependencies {
    implementation(project(":micrproject_contrib"))
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
    filesMatching("com/projectlibre1/version/version.properties") {
        expand("version" to rootProject.version.toString())
    }
}
