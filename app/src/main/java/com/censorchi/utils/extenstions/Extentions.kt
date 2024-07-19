package com.censorchi.utils
import android.app.Activity
import android.content.res.Resources
import android.net.Uri
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.FileProvider
import com.astritveliu.boom.Boom
import com.censorchi.BuildConfig
import com.censorchi.di.MyApplication.Companion.appContext
import java.io.File

val uri = getTmpFileUri()
fun View.applyBoomEffect(){
    Boom(this)
}

private fun getTmpFileUri(): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", appContext?.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(appContext!!, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
}

fun View.resizeView(w: Double, h: Double) {

    val activity = this.context as Activity
    val maxWidth = activity.screenWidth()
    val maxHeight = activity.screenHeight() - (2 * 56.toPx())

    val scale = kotlin.math.min(
        maxWidth / w,
        maxHeight / h)

    layoutParams.width = (w * scale).toInt()
    layoutParams.height = (h * scale).toInt()
    requestLayout()
}

/**
 * Convert dp to px
 */
fun Int.toPx() = (this * Resources.getSystem().displayMetrics.density).toInt()


/**
 * Get screen width in pixels
 */
fun Activity.screenWidth(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.widthPixels
}

/**
 * Get screen height in pixels
 */
fun Activity.screenHeight(): Int {
    val displayMetrics = DisplayMetrics()
    windowManager.defaultDisplay.getMetrics(displayMetrics)
    return displayMetrics.heightPixels
}



