dependencies {
    implementation(project(":projectlibre_contrib"))
}

sourceSets {
    main {
        resources.srcDir(rootProject.file("projectlibre_build/src"))
    }
}

tasks.processResources {
    filesMatching("com/projectlibre1/version/version.properties") {
        expand("version" to rootProject.version.toString())
    }
}
