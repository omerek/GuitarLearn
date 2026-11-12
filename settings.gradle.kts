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
        // usb-serial-for-android (Arduino Nano'daki CH340/FTDI/CP210x USB-seri çipleri için)
        maven("https://jitpack.io")
    }
}

rootProject.name = "GuitarLearn"
include(":app")
