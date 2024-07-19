package com.censorchi.views.activities
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.censorchi.utils.sharedPreference.AppStorage
import com.censorchi.BuildConfig
import com.censorchi.utils.Constants.PERMISSION_REQUEST_CODE
import java.io.*
import java.util.concurrent.TimeUnit

open class BaseActivity : AppCompatActivity() {
    private lateinit var afterPermissionFunc: (Map<String, Int>) -> Unit

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        AppStorage.init(this)
    }

    fun fullScreenWithStatusBarWhiteIcon() {
        setStatusBarLightText(window, true)
    }

    private fun setStatusBarLightText(window: Window, isLight: Boolean) {
        setStatusBarLightTextOldApi(window, isLight)
        setStatusBarLightTextNewApi(window, isLight)
    }

    private fun setStatusBarLightTextOldApi(window: Window, isLight: Boolean) {
        val decorView = window.decorView
        decorView.systemUiVisibility =
            if (isLight) {
                decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() or View.SYSTEM_UI_FLAG_IMMERSIVE
            } else {
                decorView.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_IMMERSIVE
            }
    }

    private fun setStatusBarLightTextNewApi(window: Window, isLightText: Boolean) {
        ViewCompat.getWindowInsetsController(window.decorView)?.apply {
            // Light text == dark status bar
            isAppearanceLightStatusBars = !isLightText
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }

    fun setStatusBar() {
        if (Build.VERSION.SDK_INT in 19..20) {
            setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, true)
        }
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        setWindowFlag(this, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
        window.statusBarColor = Color.TRANSPARENT
    }


    fun checkAndRequestPermission(permissions: Array<String>, param: (Map<String, Int>) -> Unit) {
        requestPermissions(permissions, PERMISSION_REQUEST_CODE)
        afterPermissionFunc = param
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionResults = mutableMapOf<String, Int>()
        permissions.forEachIndexed { i, permission ->
            permissionResults[permission] = grantResults[i]
        }
        afterPermissionFunc(permissionResults)
    }

    private fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
        val win = activity.window
        val winParams = win.attributes
        if (on) {
            winParams.flags = winParams.flags or bits
        } else {
            winParams.flags = winParams.flags and bits.inv()
        }
        win.attributes = winParams
    }

    fun shareImageAndText(bitmap: Bitmap) {
        val uri = getImageToShare(bitmap)
        val intent = Intent(Intent.ACTION_SEND)

        // putting uri of image to be shared
        intent.putExtra(Intent.EXTRA_STREAM, uri)

        // adding text to share
        intent.putExtra(Intent.EXTRA_TEXT, "Sharing Image")

        // Add subject Here
        intent.putExtra(Intent.EXTRA_SUBJECT, "Subject Here")

        // setting type to image
        intent.type = "image/png"
        startActivity(Intent.createChooser(intent, "Share Via"))
    }

    private fun getImageToShare(bitmap: Bitmap): Uri? {
        val imageFolder = File(cacheDir, "images")
        var uri: Uri? = null
        try {
            imageFolder.mkdirs()
            val file = File(imageFolder, "shared_image.png")
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID + ".provider", file)
        } catch (e: Exception) {
            Toast.makeText(this, "" + e.message, Toast.LENGTH_LONG).show()
        }
        return uri
    }


    fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


    fun generateBlurName(): String {
        return "VIDEO_BLUR_" + System.currentTimeMillis()
    }

    fun getRealPathFromUri(context: Context, contentUri: Uri): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri, proj, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.moveToFirst()
            cursor.getString(columnIndex)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        } finally {
            cursor?.close()
        }
    }

    fun getNewPath(input: String): String {
        val oldPath = File(input)
        val newPath = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                .toString() + "/." + System.currentTimeMillis() + ".mp4"
        )
        try {
            return if (input.contains(" ") || input.contains("-")) {
                copyFile(
                    oldPath,
                    newPath,
                    this
                )
                newPath.absolutePath
            } else {
                oldPath.absolutePath
            }
        } catch (_: java.lang.Exception) {
        }
        return input
    }

    @Throws(IOException::class)
    fun copyFile(file: File, file2: File, context: Context?) {
        if (!file2.parentFile.exists()) {
            file2.parentFile.mkdirs()
        }
        if (!file2.exists()) {
            file2.createNewFile()
        }
        val channel = FileInputStream(file).channel
        val channel2 = FileOutputStream(file2).channel

        try {
            channel2.transferFrom(channel, 0, channel.size())
            val str = "pathhh"
            if (!file.path.endsWith(".jpg")) {
                if (!file.path.endsWith(".png") && !file.path.endsWith(".jpeg")) {
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(file2.path),
                        arrayOf("video/mp4"),
                        null
                    )
                    Log.v(str, file2.path)
                    channel?.close()
                    if (channel2 == null) {
                        channel2?.close()
                    }
                    return
                }
            }
        } catch (th: Throwable) {
            channel?.close()
            channel2?.close()
            throw th
        }
    }

    fun moveFile(inputPath: String, outputPath: String) {
        var `in`: InputStream? = null
        var out: OutputStream? = null
        try {
            `in` = FileInputStream(inputPath)
            out = FileOutputStream(outputPath)
            val buffer = ByteArray(1024)
            var read: Int
            while (`in`.read(buffer).also { read = it } != -1) {
                out.write(buffer, 0, read)
            }
            `in`.close()
            `in` = null
            out.flush()
            out.close()
            out = null
            scanFile(outputPath)
        } catch (fnfe1: FileNotFoundException) {
            fnfe1.printStackTrace()
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }

    private fun scanFile(path: String) {
        MediaScannerConnection.scanFile(
            this, arrayOf(path), null
        ) { path1: String, _: Uri? ->
            Log.i(
                "TAG",
                "Finished scanning $path1"
            )
        }
    }

      fun isVideoHaveAudioTrack(path: String): Boolean {
        var audioTrack = false
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(path)
        val hasAudioStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
        audioTrack = if (hasAudioStr != null && hasAudioStr == "yes") {
            true
        } else {
            false
        }
        return audioTrack
    }

    /* This function is call for calculate remaining time */
    fun calculateTimeLeft(timeLeft: Long): String {
        return String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(timeLeft) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(timeLeft) % TimeUnit.MINUTES.toSeconds(1)
        )
    }

    fun setShare(path: String) {
        val uri = FileProvider.getUriForFile(
            this@BaseActivity, "$packageName.provider", File(path)
        )
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.apply {
            type = "video/*"
            putExtra(
                Intent.EXTRA_TEXT,
                "Create By : Censor X \n\n https://play.google.com/store/apps/details?id=$packageName"
            )
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        startActivity(Intent.createChooser(sharingIntent, "Share Video"))

    }

}