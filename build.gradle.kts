buildscript {
    repositories {
        // CRÍTICO: Repositorio Huawei DEBE ir primero
        maven {
            url = uri("https://developer.huawei.com/repo/")
            isAllowInsecureProtocol = true
        }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.20")
    }
}

allprojects {
    repositories {
        // CRÍTICO: Repositorio Huawei DEBE ir primero
        maven {
            url = uri("https://developer.huawei.com/repo/")
            isAllowInsecureProtocol = true
        }
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}