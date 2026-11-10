dependencies {
    // Deliberately JDK/Swing-only: embedders provide their own command system,
    // icon loading, localization, and look and feel.
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
}
