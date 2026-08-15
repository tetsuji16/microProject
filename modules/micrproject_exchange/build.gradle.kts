dependencies {
    implementation(project(":micrproject_contrib"))
    implementation(project(":micrproject_core"))
    implementation(libs.mpxj)
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    enabled = true
    systemProperty("java.awt.headless", "true")
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
