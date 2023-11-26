import java.net.URI

include(":SinchCalling")


pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url= URI("https://jitpack.io") }
        maven { url= URI("https://maven.sinch.com") }
        flatDir {
            dirs("libs")
        }
    }
}

rootProject.name = "Ron-Sinch"
include(":app")
 