package com.censorchi.utils
import android.net.Uri
import android.view.View
import androidx.core.content.FileProvider
import com.astritveliu.boom.Boom
import com.censorchi.BuildConfig
import com.censorchi.di.MyApplication.Companion.appContext
import java.io.File

val uri = getTmpFileUri()
fun View.applyBoomEffect(ripple: Boolean){
    Boom(this)
}

private fun getTmpFileUri(): Uri {
    val tmpFile = File.createTempFile("tmp_image_file", ".png", appContext?.cacheDir).apply {
        createNewFile()
        deleteOnExit()
    }
    return FileProvider.getUriForFile(appContext!!, "${BuildConfig.APPLICATION_ID}.provider", tmpFile)
}



