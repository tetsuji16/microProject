dependencies {
    implementation(project(":projectlibre_contrib"))
    implementation(project(":projectlibre_core"))
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
