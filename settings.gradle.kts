pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.xenondevs.xyz/releases/")
        maven("https://repo.loohpjames.com/repository/")
    }
}

rootProject.name = "AdvancedAnnouncer"

include(
    "core-api",
    "core-domain",
    "application",
    "platform-common",
    "platform-spigot"
)
