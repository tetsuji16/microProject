rootProject.name = "projectlibre"

include("projectlibre_contrib")
include("projectlibre_core")
include("projectlibre_application")
include("projectlibre_ui")
include("projectlibre_exchange")
include("projectlibre_reports")

project(":projectlibre_contrib").projectDir = file("modules/projectlibre_contrib")
project(":projectlibre_core").projectDir = file("modules/projectlibre_core")
project(":projectlibre_application").projectDir = file("modules/projectlibre_application")
project(":projectlibre_ui").projectDir = file("modules/projectlibre_ui")
project(":projectlibre_exchange").projectDir = file("modules/projectlibre_exchange")
project(":projectlibre_reports").projectDir = file("modules/projectlibre_reports")
