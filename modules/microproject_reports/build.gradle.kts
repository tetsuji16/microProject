dependencies {
    implementation(project(":microproject_contrib"))
    implementation(project(":microproject_core"))
    implementation(libs.commons.csv)
    implementation(libs.slf4j.api)
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
}
