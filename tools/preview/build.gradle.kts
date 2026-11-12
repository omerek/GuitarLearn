plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.compose") version "2.0.21"
    application
}
val app = rootDir.resolve("../../app/src/main/java").canonicalPath
sourceSets {
    main {
        kotlin.srcDirs(app, "stubs", "src")
        kotlin.exclude(
            "**/MainActivity.kt", "**/audio/TunerEngine.kt", "**/audio/SoundPlayer.kt", "**/audio/Metronome.kt",
            "**/data/Progress.kt", "**/ui/platform/Platform.kt", "**/ui/screens/TunerScreen.kt",
            "**/led/LedController.kt", "**/audio/ChordListener.kt", "**/data/UserSongStore.kt", "**/ui/platform/Backup.kt",
        )
    }
}
val cmp = "1.7.3"
dependencies {
    implementation("org.jetbrains.compose.desktop:desktop-jvm-linux-x64:$cmp")
    implementation("org.jetbrains.compose.material3:material3-desktop:$cmp")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:$cmp")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
}
kotlin { jvmToolchain(17) }
application { mainClass.set("preview.MainKt") }

tasks.named<JavaExec>("run") {
    systemProperty("out", layout.buildDirectory.dir("shots").get().asFile.path)
}
