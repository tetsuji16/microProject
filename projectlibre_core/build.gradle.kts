dependencies {
    implementation(project(":projectlibre_contrib"))
    implementation(
        fileTree(rootProject.file("projectlibre_contrib/lib")) {
            include("**/*.jar")
            exclude("flamingo-6.2.jar", "trident-6.2.jar")
        }
    )
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
