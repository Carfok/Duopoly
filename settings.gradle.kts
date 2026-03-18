pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    val props = java.util.Properties()
    val localPropertiesFile = rootProject.projectDir.resolve("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { props.load(it) }
    }

    repositories {
        google()
        mavenCentral()
        maven { 
            url = uri("https://maven.pkg.github.com/godotengine/godot")
            credentials {
                username = props.getProperty("gpr.user") ?: System.getenv("GPR_USER")
                password = props.getProperty("gpr.key") ?: System.getenv("GPR_KEY")
            }
        }
    }
}

rootProject.name = "Duopoly"
include(":app")
include(":core")
include(":ai")
include(":data")
include(":godot-bridge")
