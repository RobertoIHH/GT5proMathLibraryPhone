pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        // CRÍTICO: Repositorio Huawei DEBE ir primero
        maven {
            url = uri("https://developer.huawei.com/repo/")
            isAllowInsecureProtocol = true
        }
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "GT5ProMathLibrary"
include(":app")