import org.gradle.jvm.tasks.Jar

dependencies {
    implementation(libs.bundles.commons.legacy)
    implementation(libs.commons.collections4)
    implementation(libs.bundles.jackson)
    implementation(libs.jasperreports)
    implementation(libs.bundles.jaxb)
    implementation(libs.bundles.poi)
    implementation(libs.flatlaf)
    implementation(libs.flatlaf.extras)
    implementation(libs.groovy)
    implementation(libs.pdfbox)
    implementation(libs.radiance.neon)
    implementation(libs.radiance.trident)
    implementation(libs.flamingo)
    implementation(libs.slf4j.api)
    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val licenseDir = rootProject.file("packaging/licenses")

tasks.jar {
    archiveFileName.set("projectlibre-contrib.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    exclude("net/sf/jasperreports/compilers/**")
    metaInf {
        from(licenseDir)
    }
}

val reportsJar by tasks.registering(Jar::class) {
    archiveFileName.set("projectlibre-reports.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    include("net/sf/jasperreports/compilers/**")
    metaInf {
        from(licenseDir)
    }
}

val scriptRadianceJar by tasks.registering(Jar::class) {
    archiveFileName.set("projectlibre-script-radiance.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    metaInf {
        from(licenseDir)
    }
}

tasks.assemble {
    dependsOn(reportsJar, scriptRadianceJar)
}

tasks.test {
    enabled = true
    useJUnitPlatform()
}
