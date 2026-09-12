dependencies {
    implementation(project(":micrproject_contrib"))
    implementation(project(":micrproject_core"))
    implementation(libs.jasperreports)
    implementation(libs.commons.collections)
    implementation(libs.slf4j.api)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
}
