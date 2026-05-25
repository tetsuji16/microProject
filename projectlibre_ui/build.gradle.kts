plugins {
    application
}

dependencies {
    implementation(project(":projectlibre_contrib"))
    implementation(project(":projectlibre_core"))
    implementation(project(":projectlibre_exchange"))
    implementation(project(":projectlibre_reports"))
    implementation(
        fileTree(rootProject.file("projectlibre_contrib/lib")) {
            include("**/*.jar")
            exclude("flamingo-6.2.jar", "trident-6.2.jar")
        }
    )
}

application {
    mainClass.set("com.projectlibre1.main.Main")
}
