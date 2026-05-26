plugins {
    application
}

dependencies {
    implementation(project(":projectlibre_contrib"))
    implementation(project(":projectlibre_core"))
    implementation(project(":projectlibre_exchange"))
    implementation(project(":projectlibre_reports"))
}

application {
    mainClass.set("com.projectlibre1.main.Main")
}
