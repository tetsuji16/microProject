import org.gradle.jvm.tasks.Jar

val shadedLibs = listOf(
    "lib/flatlaf-3.7.1.jar",
    "lib/flatlaf-extras-3.7.1.jar",
    "lib/commons-lang.jar",
    "lib/groovy/groovy-2.4.21.jar",
    "lib/groovy/ant-antlr-1.9.15.jar",
    "lib/commons-beanutils.jar",
    "lib/commons-digester.jar",
    "lib/commons-pool.jar",
    "lib/commons-collections.jar",
    "lib/commons-logging-api.jar",
    "lib/forms.jar",
    "lib/jlfgr.jar",
    "lib/l2fprod-common-totd.jar",
    "lib/nachocalendar.jar",
    "lib/jfreechart.jar",
    "lib/jcommon.jar",
    "lib/jdnc-0_7-all.jar",
    "lib/itext.jar",
    "lib/radiance-flamingo-1.0.2.jar",
    "lib/radiance-neon-1.0.2.jar",
    "lib/radiance-trident-1.0.2.jar",
    "lib/radiance-substance-1.0.2.jar",
    "lib/exchange/jakarta-poi.jar",
    "lib/exchange/poi-ooxml-3.17.jar",
    "lib/exchange/poi-ooxml-schemas-3.17.jar",
    "lib/exchange/xmlbeans-2.6.0.jar",
    "lib/exchange/commons-collections4.jar",
    "lib/exchange/rtfparserkit.jar",
    "lib/exchange/jaxb-api.jar",
    "lib/exchange/jaxb-core.jar",
    "lib/exchange/jaxb-impl.jar",
    "lib/exchange/javax.activation-api.jar",
    "lib/commons-lang3-3.14.0.jar",
    "lib/commons-collections4-4.4.jar",
    "lib/pdfbox-3.0.1.jar",
    "lib/fontbox-3.0.1.jar",
    "lib/pdfbox-io-3.0.1.jar",
    "lib/jackson-core-2.16.1.jar",
    "lib/jackson-databind-2.16.1.jar",
    "lib/jackson-annotations-2.16.1.jar",
)

val scriptLibs = listOf(
    "lib/groovy/groovy-2.4.21.jar",
    "lib/groovy/ant-antlr-1.9.15.jar",
    "lib/commons-lang.jar",
)

val reportsLibs = listOf(
    "lib/jasperreports/bsh.jar",
    "lib/jasperreports/jasperreports.jar",
)

dependencies {
    implementation(
        fileTree("lib") {
            include("**/*.jar")
        }
    )
}

val licenseDir = rootProject.file("projectlibre_build/license")

tasks.jar {
    archiveFileName.set("projectlibre-contrib.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    exclude("net/sf/jasperreports/compilers/**")
    from(shadedLibs.map { zipTree(layout.projectDirectory.file(it)) })
    metaInf {
        from(licenseDir)
    }
}

val reportsJar by tasks.registering(Jar::class) {
    archiveFileName.set("projectlibre-reports.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    include("net/sf/jasperreports/compilers/**")
    from(reportsLibs.map { zipTree(layout.projectDirectory.file(it)) })
    metaInf {
        from(licenseDir)
    }
}

val scriptRadianceJar by tasks.registering(Jar::class) {
    archiveFileName.set("projectlibre-script-radiance.jar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(scriptLibs.map { zipTree(layout.projectDirectory.file(it)) })
    metaInf {
        from(licenseDir)
    }
}

tasks.assemble {
    dependsOn(reportsJar, scriptRadianceJar)
}
