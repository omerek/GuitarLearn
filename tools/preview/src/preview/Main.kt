package preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import com.omerek.guitarlearn.data.Progress
import com.omerek.guitarlearn.ui.theme.GuitarTheme
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

val out = File(System.getProperty("out", "build/shots")).apply { mkdirs() }

fun shot(name: String, w: Int = 412, h: Int = 892, dark: Boolean = false, content: @Composable () -> Unit) {
    val d = 2.5f
    val scene = ImageComposeScene((w * d).toInt(), (h * d).toInt(), Density(d)) {
        GuitarTheme(darkTheme = dark) {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) { content() }
        }
    }
    // birkaç kare ilerlet (animasyonlar/LaunchedEffect için)
    var img = scene.render(0)
    for (i in 1..20) img = scene.render(i * 50_000_000L)
    File(out, "$name.png").writeBytes(img.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    scene.close()
    println("wrote $name")
}

fun main(args: Array<String>) {
    val only = args.toSet()
    Screens.all.forEach { (name, c) -> if (only.isEmpty() || name in only) shot(name, content = c) }
    System.exit(0)
}
