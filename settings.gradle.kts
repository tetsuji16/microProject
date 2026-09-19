rootProject.name = "microproject"

include("microproject_contrib")
include("microproject_core")
include("microproject_application")
include("microproject_ui")
include("microproject_exchange")
include("microproject_reports")
include("microproject_bootstrap")
include("microproject_ribbon")

project(":microproject_contrib").projectDir = file("modules/microproject_contrib")
project(":microproject_core").projectDir = file("modules/microproject_core")
project(":microproject_application").projectDir = file("modules/microproject_application")
project(":microproject_ui").projectDir = file("modules/microproject_ui")
project(":microproject_exchange").projectDir = file("modules/microproject_exchange")
project(":microproject_reports").projectDir = file("modules/microproject_reports")
project(":microproject_bootstrap").projectDir = file("modules/microproject_bootstrap")
project(":microproject_ribbon").projectDir = file("modules/microproject_ribbon")
