package com.censorchi.views.activities

import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.censorchi.databinding.ActivityMainBinding
import com.censorchi.utils.Constants
import com.censorchi.utils.viewVisible
import com.sherazkhilji.videffects.filter.NoEffectFilter
import java.io.File
import kotlin.math.roundToInt

class MainActivity : BaseActivity() {
    /*Binding*/
    private lateinit var binding: ActivityMainBinding
    private var video: String? = null
    private lateinit var fileUri: Uri
    private var originalPath: String? = null
    private var mediaPlayerTop: MediaPlayer? = null

    var scaledWidth = 0f
    var scaledHeight = 0f

    private var mwidth: Int = 0
    private var mheight: Int = 0
    private var dpWidthView: Int = 0
    private var dpHeightView: Int = 0

    private var videoWidth: Int = 0
    private var videoHeight: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        video = intent.getStringExtra(Constants.VIDEO_PATH)
        fileUri = Uri.parse(video)
        originalPath = this.getRealPathFromUri(applicationContext, Uri.parse(video))
        setPlayer(originalPath!!)
        getHeight(originalPath!!)
    }

    private fun pxToDp(px: Int): Int {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        return (px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt().toInt()
    }

    private fun setPlayer(path: String) {
        binding.videoView1.apply {
            setVideoPath(path)

            setOnPreparedListener {
                it.setVolume(0F, 0F)

                mwidth = it.videoWidth
                mheight = it.videoHeight

                dpWidthView = width
                dpHeightView = height

                val xScale = dpWidthView.toFloat() / mwidth
                val yScale = dpHeightView.toFloat() / mheight
                val scale = xScale.coerceAtMost(yScale)
                scaledWidth = scale * videoWidth
                scaledHeight = scale * videoHeight

                Log.d("djdjdjdjddsd", scaledWidth.toString())
                Log.d("djdjdjdjddsd", scaledHeight.toString())
                val layoutParams: ViewGroup.LayoutParams = getLayoutParams()
                layoutParams.width = scaledWidth.toInt()
                layoutParams.height = scaledHeight.toInt()
                setLayoutParams(layoutParams)
            }
            start()
            setOnCompletionListener {

            }

        }

    }

    fun handleAspectRatio() {
        val surfaceView_Width: Int = binding.videoView2.getWidth()
        val surfaceView_Height: Int = binding.videoView2.getHeight()
        val video_Width: Float = mwidth.toFloat()
        val video_Height: Float = mheight.toFloat()
        val ratio_width = surfaceView_Width / video_Width
        val ratio_height = surfaceView_Height / video_Height
        val aspectratio = video_Width / video_Height
        val layoutParams: ViewGroup.LayoutParams = binding.videoView2.getLayoutParams()
        if (ratio_width > ratio_height) {
            layoutParams.width = (surfaceView_Height * aspectratio).toInt()
            layoutParams.height = surfaceView_Height
        } else {
            layoutParams.width = surfaceView_Width
            layoutParams.height = (surfaceView_Width / aspectratio).toInt()
        }
        binding.videoView2.layoutParams = layoutParams
    }

    fun bottomBlur(view: View) {
        binding.relLayout1.viewVisible()
        val layoutParams: ViewGroup.LayoutParams = binding.relLayout1.getLayoutParams()
        layoutParams.width = scaledWidth.toInt()
        layoutParams.height = scaledHeight.toInt()
        binding.relLayout1.setLayoutParams(layoutParams)
        mediaPlayerTop = MediaPlayer()
        mediaPlayerTop?.setOnPreparedListener {
        }

        binding.videoView2.init(
            mediaPlayerTop, NoEffectFilter()
        )
        try {
            mediaPlayerTop?.setDataSource(originalPath!!)
        } catch (e: Exception) {
            e.printStackTrace()
        }


    }


    private fun setDimension() {
        // Adjust the size of the video
        // so it fits on the screen
        val videoProportion = getVideoProportion()
        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels
        val screenProportion = screenHeight.toFloat() / screenWidth.toFloat()
        val lp: ViewGroup.LayoutParams = binding.videoView1.getLayoutParams()
        if (videoProportion < screenProportion) {
            lp.height = screenHeight
            lp.width = (screenHeight.toFloat() / videoProportion).toInt()
        } else {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() * videoProportion).toInt()
        }
        binding.videoView1.layoutParams = lp
    }

    private fun getVideoProportion(): Float {
        return 1.5f
    }

    private fun getHeight(uri: String) {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(File(uri).absolutePath)
        videoWidth = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
        )?.let {
            Integer.valueOf(
                it
            )
        }!!
        videoHeight = retriever.extractMetadata(
            MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
        )?.let {
            Integer.valueOf(it)
        }!!
        Log.d("dddddddd", "width$videoWidth")
        Log.d("dddddddd", "height$videoHeight")
    }


}