pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://repo.fancyinnovations.com/releases/")
    }
}

rootProject.name = "SMP-Creative"

include("plugins:MACore")
include("plugins:MALang")
include("plugins:MADialogs")
include("plugins:MAAura")
include("plugins:MALobbyDesign")
include("plugins:MAVeloCore")
