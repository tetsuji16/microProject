rootProject.name = "micrproject"

include("micrproject_contrib")
include("micrproject_core")
include("micrproject_application")
include("micrproject_ui")
include("micrproject_exchange")
include("micrproject_reports")

project(":micrproject_contrib").projectDir = file("modules/micrproject_contrib")
project(":micrproject_core").projectDir = file("modules/micrproject_core")
project(":micrproject_application").projectDir = file("modules/micrproject_application")
project(":micrproject_ui").projectDir = file("modules/micrproject_ui")
project(":micrproject_exchange").projectDir = file("modules/micrproject_exchange")
project(":micrproject_reports").projectDir = file("modules/micrproject_reports")
