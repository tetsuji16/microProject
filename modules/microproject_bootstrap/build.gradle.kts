// Bootstrap auto-update launcher (#338, plan A — update4j).
// This module is intentionally lean: it only depends on update4j (and JUnit for
// tests). It is carved out of the root project's bulk dependency injection in
// build.gradle.kts so the bootstrap jar stays minimal — the bootstrap must run
// before the business application and should not pull in the UI/report stack.
dependencies {
    implementation(libs.update4j)

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    enabled = true
    useJUnitPlatform()
}
