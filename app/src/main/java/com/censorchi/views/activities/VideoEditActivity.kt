package com.censorchi.views.activities

import android.app.Dialog
import android.content.ContentValues
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.AndroidRuntimeException
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.Statistics
import com.censorchi.R
import com.censorchi.databinding.ActivityEditVideoBinding
import com.censorchi.utils.*
import com.censorchi.utils.Constants.CENSORX_BOTTOM_BLUR
import com.censorchi.utils.Constants.CENSORX_FULL_BLUR
import com.censorchi.utils.Constants.CENSORX_TOP_BLUR
import com.censorchi.utils.Constants.CROP_BOTTOM_BLUR
import com.censorchi.utils.Constants.CROP_BOTTOM_BLUR_TWO
import com.censorchi.utils.Constants.Full_VIDEO_PATH
import com.censorchi.utils.Constants.VIDEO_PATH
import com.censorchi.utils.sharedPreference.AppStorage
import com.censorchi.views.popUp.DialogueForShowVideoProgress
import com.censorchi.views.popUp.ExitDialogue
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.upstream.*
import com.iamkdblue.videocompressor.VideoCompress
import kotlinx.coroutines.launch
import net.surina.soundtouch.SoundTouch
import java.io.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt

class VideoEditActivity : BaseActivity(), Player.Listener, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener, MediaPlayer.OnCompletionListener {
    /*Binding*/
    private lateinit var binding: ActivityEditVideoBinding

    /* Views & Objects */
    private lateinit var fileUri: Uri
    private lateinit var audioChangeLayout: RelativeLayout
    private lateinit var audioChangeLayoutForLandScape: RelativeLayout
    private lateinit var audioDistortion: RelativeLayout
    private lateinit var audioDistortionForLandScape: RelativeLayout
    private lateinit var audioRemove: RelativeLayout
    private lateinit var audioRemoveForLandScape: RelativeLayout
    private lateinit var duration: TextView
    private lateinit var durationLandScape: TextView
    private lateinit var totalTime: TextView
    private lateinit var totalTimeForLandScape: TextView
    private lateinit var dialog: Dialog
    private lateinit var popUpForCompressedVideo: DialogueForShowVideoProgress
    private lateinit var exitPopup: ExitDialogue
    private var handler: Handler? = null
    private var progressbar: ProgressBar? = null
    private var textView: TextView? = null
    private var mediaPlayerTop: MediaPlayer? = null
    private var mediaPlayerBottom: MediaPlayer? = null
    private var mediaPlayerFull: MediaPlayer? = null
    private var listForLastFifty = ArrayList<String>()
    private var listForFirstFifty = ArrayList<String>()

    /* Files */
    private var file1: File? = null
    private var file2: File? = null
    private var compressedFile: File? = null
    private var file3: File? = null
    private var folder: File? = null
    private var fileForMute: File? = null
    private var extractAudioFile: File? = null
    private var fullVideoFile: File? = null
    private var topBlurFinalOutputFile: File? = null
    private var bottomBlurOutputFile: File? = null
    private var bottomBlurOutputFileTwo: File? = null
    private var bottomBlurFinalOutputFile: File? = null
    private var fullBlurOutputFile: File? = null

    var downloadFile: File? = null
    var downloadFile2: File? = null
    var compresseddFile: File? = null
    var downloadFile3: File? = null
    var fullVideoForCropFile: File? = null
    var topBlurFinalDownload: File? = null
    var bottomBlurDownload: File? = null
    var bottomBlurDownloadTwo: File? = null
    var bottomBlurFinalDownload: File? = null
    var fullBlurDownload: File? = null

    /* Strings */
    private var video: String? = null
    private lateinit var outPutForMutedBlurredVideo: String
    private lateinit var secondOutputForBlur: String
    private lateinit var lessResolatedFile: String
    private lateinit var compressFile: String
    private lateinit var finalOutput: String
    private lateinit var fullOutPut: String
    private lateinit var topBlurFinalOutput: String
    private lateinit var bottomBlurOutput: String
    private lateinit var bottomBlurOutputTwo: String
    private lateinit var bottomBlurFinalOutput: String
    private lateinit var fullBlurOutput: String
    private var finalVideo: String = ""
    private var mutedVideo: String = ""
    private var finalMuteVideo: String = ""
    private var originalPath: String? = null
    private var distortedVideo: String = ""
    private var finalDistortedVideo: String = ""

    /*Integers*/
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var seekbarRadius = 25
    private var seekBarForSaveVideo = 50
    private var higth = 5
    private var weigth = 5
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var values = 0
    private var videoPosition = 0
    private var counter: Int = 0
    private var counter2: Int = 0
    private var scaledWidth = 0f
    private var scaledHeight = 0f

    /*Longs*/
    private var totalDuration: Long = 0
    private var lastMaxValue: Long = 0
    private var progressId: Long = 0

    /*Floats*/
    private var dur: Float = 0F
    private val uris = mutableListOf<Uri>()
    private var statistics: Statistics? = null
    private var mHandler: Handler? = null

    /*Booleans*/
    private var isBlurred = false
    private var isDistortion = false
    private var isCompression = false
    private var isMuted = false
    private var topBlur = false
    private var nonBlur = false
    private var bottomBlur = false
    private var fullBlur = false
    private var checkVisibility = false
    private var isShowBackDialogue = false
    private var isLandScapeMode = false
    private var isLargeSizeVideo = false
    private var isHundredValue = false
    private var processComplete = false
    private var isInitialBlurValue = false
    private var isInitialBlurValueDistortion = false

    private val updateProgressAction = Runnable {
        updateProgress()
    }

    private val updateProgressActionForLandScape = Runnable {
        updateProgressForLandScape()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        disableShareAndSave()
        binding.exoPause.viewVisible()
        handler = Handler(Looper.getMainLooper())
        popUpForCompressedVideo = DialogueForShowVideoProgress()
        createFilesAndDirectories()
        video = intent.getStringExtra(VIDEO_PATH)
        fileUri = Uri.parse(video)
        originalPath = this.getRealPathFromUri(applicationContext, Uri.parse(video))

        uris.add(fileUri)
        getHeight(originalPath!!)
        getTotalDuration(originalPath!!)
        checkOrientationOfVideo()

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            if (isLandScapeMode) {
                binding.relForGetValues.viewVisible()
                binding.layoutBlue.viewVisible()
                processComplete = true
                binding.apply {
                    videoView1.apply {
                        setVideoPath(originalPath!!)
                        start()
                        setOnPreparedListener {
                            it.setVolume(0F, 0F)
                            var mwidth = it.videoWidth
                            var mheight = it.videoHeight

                            var dpWidthView = width
                            var dpHeightView = height

                            val xScale = dpWidthView.toFloat() / mwidth
                            val yScale = dpHeightView.toFloat() / mheight
                            val scale = xScale.coerceAtMost(yScale)

                            scaledWidth = scale * videoWidth
                            scaledHeight = scale * videoHeight
                        }

                    }
                }
                if (videoHeight == videoWidth) {
                    processComplete = false
                    compressVideo()
                    resizeVideo("compress")
                    isLargeSizeVideo = true
                } else {
                    compressVideo()
                    resizeVideo("compress")
                    isLargeSizeVideo = true
                }

            } else {
                if (videoWidth != 1920) {
                    binding.relForGetValues.viewVisible()
                    binding.layoutBlue.viewVisible()
                    processComplete = true
                    binding.apply {
                        videoView1.apply {
                            setVideoPath(originalPath!!)
                            start()
                            setOnPreparedListener {
                                it.setVolume(0F, 0F)
                                var mwidth = it.videoWidth
                                var mheight = it.videoHeight

                                var dpWidthView = width
                                var dpHeightView = height

                                val xScale = dpWidthView.toFloat() / mwidth
                                val yScale = dpHeightView.toFloat() / mheight
                                val scale = xScale.coerceAtMost(yScale)

                                scaledWidth = scale * videoWidth
                                scaledHeight = scale * videoHeight
                            }

                        }

                    }
                }

                if (videoHeight > 1080) {
                    isLargeSizeVideo = false
                    isCompression = true
                    setProgressDialogue("process")
                    videoCutForFullVideoCommand(
                        getRealPathFromUri(applicationContext, Uri.parse(video))!!,
                        fullOutPut,
                        "noCompress"
                    )

                    videoCutForTopCommand(originalPath!!, bottomBlurOutput, "noCompress")
                    videoCutForBottomCommand(originalPath!!, bottomBlurOutputTwo, "noCompress")
                } else if (videoWidth > 1000 || videoHeight >= 900) {
                    if (!isLandScapeMode) {
                        val params =
                            binding.mVideoSurfaceViewNone.layoutParams as RelativeLayout.LayoutParams
                        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
                        params.addRule(RelativeLayout.ALIGN_PARENT_LEFT)
                        params.addRule(RelativeLayout.ALIGN_PARENT_TOP)
                        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        binding.mVideoSurfaceViewNone.layoutParams = params //causes layout update

                    }
                    compressVideo()
                    resizeVideo("compress")
                    isLargeSizeVideo = true
                } else {
                    isLargeSizeVideo = false
                    isCompression = true
                    setProgressDialogue("process")
                    videoCutForFullVideoCommand(
                        getRealPathFromUri(applicationContext, Uri.parse(video))!!,
                        fullOutPut,
                        "noCompress"
                    )
                    videoCutForTopCommand(originalPath!!, bottomBlurOutput, "noCompress")
                    videoCutForBottomCommand(originalPath!!, bottomBlurOutputTwo, "noCompress")
                }
            }
        }, 500)

        binding.homeToolbarId.toolbarNameTv.text = getString(R.string.censor_video)
        audioChangeLayout = binding.audioChangeLayout
        audioChangeLayoutForLandScape = binding.audioChangeLayoutForLanScape
        audioDistortion = binding.cvDistortion
        audioDistortionForLandScape = binding.cvDistortionForLanScape
        audioRemove = binding.cvAudioRemoved
        audioRemoveForLandScape = binding.cvAudioRemovedForLanScape
        duration = binding.duration
        durationLandScape = binding.durationForLandScape
        totalTime = binding.endTime
        totalTimeForLandScape = binding.endTimeForLandScape

        listeners()
        setBottomLayout()
        setNonBlur()
        getTotalDuration(originalPath!!)
    }

    private fun setVideoView(path: String) {
        binding.apply {
            mVideoSurfaceViewNone.apply {
                setVideoPath(path)
                start()
                setBlurs()
                setOnPreparedListener {
                    seekbar.max = mVideoSurfaceViewNone.duration
                    setPlayerSeekBar()
                    if (!isBlurred) {
                        mVideoSurfaceViewTop.viewGone()
                        mVideoSurfaceViewBottom.viewGone()
                        mVideoSurfaceViewFull.viewGone()
                    }
                }
                setOnCompletionListener {
                    exoPause.viewGone()
                    exoPlay.viewVisible()
                }
            }
        }
    }

    private fun setVideoViewLandScape(path: String, type: String) {
        val layoutParams: ViewGroup.LayoutParams = binding.relLayoutLandScape.layoutParams
        layoutParams.width = scaledWidth.toInt()
        layoutParams.height = scaledHeight.toInt()
        binding.relLayoutLandScape.layoutParams = layoutParams

        binding.apply {
            mVideoSurfaceViewNoneLandScape.apply {
                setVideoPath(path)
                val metrics = DisplayMetrics()
                windowManager.defaultDisplay.getMetrics(metrics)
                mVideoSurfaceViewNoneLandScape.layoutParams =
                    RelativeLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels)
                start()
                setBlursForLandScape()
                setOnPreparedListener {
                    seekbar.max = mVideoSurfaceViewNoneLandScape.duration
                    setPlayerSeekBarForLandScape()
                    if (!isBlurred) {
                        mVideoSurfaceViewTopForLandScape.viewGone()
                        mVideoSurfaceViewBottomForLandScape.viewGone()
                        mVideoSurfaceViewFullForLandScape.viewGone()
                    }
                }
                setOnCompletionListener {
                    videoPauseBtnLandScape.viewGone()
                    videoPlayBtnLandScape.viewVisible()
                }
            }
        }
    }


    private fun resetPlayer() {
        if (isLandScapeMode) {
            binding.apply {
                if (topBlur) {
                    setVideoWhenTopBlur()
                } else if (bottomBlur) {
                    setVideoWhenBottomBlur()
                } else if (fullBlur) {
                    setVideoWhenFullBlur()
                }
                setPlayerSeekBarForLandScape()
                videoPauseBtnLandScape.viewVisible()
                videoPlayBtnLandScape.viewGone()
            }
        } else {
            if (processComplete) {
                val layoutParams: ViewGroup.LayoutParams = binding.relBlurViews.layoutParams
                layoutParams.width = scaledWidth.toInt()
                layoutParams.height = scaledHeight.toInt()
                binding.relBlurViews.layoutParams = layoutParams

                val params = RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                if (topBlur) {
                    binding.mVideoSurfaceViewTop.layoutParams = params
                } else if (bottomBlur) {
                    binding.mVideoSurfaceViewBottom.layoutParams = params
                } else {
                    binding.mVideoSurfaceViewFull.layoutParams = params
                }
            }

            binding.apply {
                if (topBlur) {
                    setVideoWhenTopBlur()
                } else if (bottomBlur) {
                    setVideoWhenBottomBlur()
                } else if (fullBlur) {
                    setVideoWhenFullBlur()
                }
                setPlayerSeekBar()
                exoPause.viewVisible()
                exoPlay.viewGone()
            }
        }

    }


    private fun resetPlayerForMute(path: String) {
        binding.apply {
            if (isLandScapeMode) {
                lifecycleScope.launch {
                    mVideoSurfaceViewNoneLandScape.apply {
                        stopPlayback()
                        setVideoPath(path)
                        start()
                    }
                }
                videoPauseBtnLandScape.viewVisible()
                videoPlayBtnLandScape.viewGone()
                if (topBlur) {
                    setVideoWhenTopBlur()
                } else if (bottomBlur) {
                    setVideoWhenBottomBlur()
                } else if (fullBlur) {
                    setVideoWhenFullBlur()
                }
                setPlayerSeekBarForLandScape()
            } else {
                lifecycleScope.launch {
                    mVideoSurfaceViewNone.apply {
                        stopPlayback()
                        setVideoPath(path)
                        start()
                    }
                }
                exoPause.viewVisible()
                exoPlay.viewGone()
                if (topBlur) {
                    setVideoWhenTopBlur()
                } else if (bottomBlur) {
                    setVideoWhenBottomBlur()
                } else if (fullBlur) {
                    setVideoWhenFullBlur()
                }
                setPlayerSeekBar()
            }


        }
    }

    private fun setVideoWhenTopBlur() {
        binding.apply {
            if (isLandScapeMode) {
                mVideoSurfaceViewNoneLandScape.seekTo(0)
                mVideoSurfaceViewNoneLandScape.start()
                mediaPlayerTop?.seekTo(0)
                mediaPlayerTop?.start()

                mVideoWidth = videoWidth
                mVideoHeight = videoHeight
                mVideoWidth /= weigth
                mVideoHeight /= higth
                val filter = BlurEffect2(seekbarRadius, mVideoWidth, mVideoHeight)
                mVideoSurfaceViewTopForLandScape.filter = filter

                mVideoSurfaceViewBottomForLandScape.viewGone()
                mVideoSurfaceViewTopForLandScape.viewVisible()
                mVideoSurfaceViewFullForLandScape.viewGone()
            } else {
                mVideoSurfaceViewNone.seekTo(0)
                mVideoSurfaceViewNone.start()

                mediaPlayerTop?.seekTo(0)
                mediaPlayerTop?.start()
                mVideoWidth = videoWidth
                mVideoHeight = videoHeight
                mVideoWidth /= weigth
                mVideoHeight /= higth
                val filter = BlurEffect2(seekbarRadius, mVideoWidth, mVideoHeight)
                mVideoSurfaceViewTop.filter = filter
                mVideoSurfaceViewBottom.viewGone()
                mVideoSurfaceViewTop.viewVisible()
                mVideoSurfaceViewFull.viewGone()
            }


        }
    }

    private fun setVideoWhenBottomBlur() {
        if (mediaPlayerBottom != null) {
            binding.apply {
                if (isLandScapeMode) {
                    mVideoSurfaceViewNoneLandScape.seekTo(0)
                    mVideoSurfaceViewNoneLandScape.start()
                    mediaPlayerBottom?.seekTo(0)
                    mediaPlayerBottom?.start()

                    mVideoWidth = videoWidth
                    mVideoHeight = videoHeight
                    mVideoWidth /= weigth
                    mVideoHeight /= higth
                    val filter = BlurEffect2(seekbarRadius, mVideoWidth, mVideoHeight)
                    mVideoSurfaceViewBottomForLandScape.filter = filter

                    mVideoSurfaceViewTopForLandScape.viewGone()
                    mVideoSurfaceViewBottomForLandScape.viewVisible()
                    mVideoSurfaceViewFullForLandScape.viewGone()
                } else {
                    mVideoSurfaceViewNone.seekTo(0)
                    mVideoSurfaceViewNone.start()
                    mediaPlayerBottom?.seekTo(0)
                    mediaPlayerBottom?.start()
                    mVideoWidth = videoWidth
                    mVideoHeight = videoHeight
                    mVideoWidth /= weigth
                    mVideoHeight /= higth
                    val filter = BlurEffect2(seekbarRadius, mVideoWidth, mVideoHeight)
                    mVideoSurfaceViewBottom.filter = filter
                    mVideoSurfaceViewTop.viewGone()
                    mVideoSurfaceViewBottom.viewVisible()
                    mVideoSurfaceViewFull.viewGone()
                }

            }
        }
    }

    private fun setVideoWhenFullBlur() {
        binding.apply {
            if (isLandScapeMode) {
                mVideoSurfaceViewNoneLandScape.seekTo(0)
                mVideoSurfaceViewNoneLandScape.start()

                mediaPlayerFull?.seekTo(0)
                mediaPlayerFull?.start()

                mVideoWidth = videoWidth
                mVideoHeight = videoHeight
                mVideoWidth /= weigth
                mVideoHeight /= higth
                val filter = BlurEffect2(seekbarRadius, mVideoWidth, mVideoHeight)
                mVideoSurfaceViewFullForLandScape.filter = filter
                mVideoSurfaceViewFullForLandScape.viewVisible()

                layoutBlack.viewVisible()
                mVideoSurfaceViewTopForLandScape.viewGone()
                mVideoSurfaceViewBottomForLandScape.viewGone()
                mVideoSurfaceViewFullForLandScape.viewVisible()
            } else {
                mVideoSurfaceViewNone.seekTo(0)
                mVideoSurfaceViewNone.start()
                mediaPlayerFull?.seekTo(0)
                mediaPlayerFull?.start()

                mVideoWidth = videoWidth
                mVideoHeight = videoHeight
                mVideoWidth /= weigth
                mVideoHeight /= higth
                val filter = BlurEffect2(seekbarRadius, mVideoWidth, mVideoHeight)
                mVideoSurfaceViewFull.filter = filter

                layoutBlack.viewVisible()
                mVideoSurfaceViewTop.viewGone()
                mVideoSurfaceViewBottom.viewGone()
                mVideoSurfaceViewFull.viewVisible()
            }
        }
    }

    private fun setBlurs() {
        if (mediaPlayerTop == null) {
            mediaPlayerTop = MediaPlayer()
            mediaPlayerTop?.setVolume(0F, 0F)
            getHeight(originalPath!!)
            var mvideoWidth = videoWidth
            var mvideoHeight = videoHeight
            mvideoWidth /= weigth
            mvideoHeight /= higth
            binding.mVideoSurfaceViewTop.holder.setFixedSize(mvideoWidth, mvideoHeight)
            binding.mVideoSurfaceViewTop.init(
                mediaPlayerTop, BlurEffect2(10, mvideoWidth, mvideoHeight)
            )
            try {
                mediaPlayerTop?.setDataSource(bottomBlurOutput)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mediaPlayerBottom == null) {
            mediaPlayerBottom = MediaPlayer()
            mediaPlayerBottom?.setVolume(0F, 0F)
            getHeight(originalPath!!)
            var mvideoWidth = videoWidth
            var mvideoHeight = videoHeight
            mvideoWidth /= weigth
            mvideoHeight /= higth
            binding.mVideoSurfaceViewBottom.holder.setFixedSize(mvideoWidth, mvideoHeight)

            binding.mVideoSurfaceViewBottom.init(
                mediaPlayerBottom, BlurEffect2(10, mvideoWidth, mvideoHeight)
            )
            try {
                mediaPlayerBottom?.setDataSource(bottomBlurOutputTwo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mediaPlayerFull == null) {
            mediaPlayerFull = MediaPlayer()
            mediaPlayerFull?.setVolume(0F, 0F)
            getHeight(originalPath!!)
            var mvideoWidth = videoWidth
            var mvideoHeight = videoHeight
            mvideoWidth /= weigth
            mvideoHeight /= higth
            binding.mVideoSurfaceViewFull.holder.setFixedSize(mvideoWidth, mvideoHeight)

            binding.mVideoSurfaceViewFull.init(
                mediaPlayerFull, BlurEffect2(10, mvideoWidth, mvideoHeight)
            )
            try {
                mediaPlayerFull?.setDataSource(originalPath!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun setBlursForLandScape() {
        if (mediaPlayerTop == null) {
            mediaPlayerTop = MediaPlayer()
            mediaPlayerTop?.setVolume(0F, 0F)
            getHeight(originalPath!!)
            var mvideoWidth = videoWidth
            var mvideoHeight = videoHeight
            mvideoWidth /= weigth
            mvideoHeight /= higth
            binding.mVideoSurfaceViewTopForLandScape.holder.setFixedSize(mvideoWidth, mvideoHeight)
            binding.mVideoSurfaceViewTopForLandScape.init(
                mediaPlayerTop, BlurEffect2(10, mvideoWidth, mvideoHeight)
            )
            try {
                mediaPlayerTop?.setDataSource(bottomBlurOutput)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mediaPlayerBottom == null) {
            mediaPlayerBottom = MediaPlayer()

            mediaPlayerBottom?.setVolume(0F, 0F)
            getHeight(originalPath!!)
            var mvideoWidth = videoWidth
            var mvideoHeight = videoHeight
            mvideoWidth /= weigth
            mvideoHeight /= higth
            binding.mVideoSurfaceViewBottomForLandScape.holder.setFixedSize(
                mvideoWidth, mvideoHeight
            )

            binding.mVideoSurfaceViewBottomForLandScape.init(
                mediaPlayerBottom, BlurEffect2(10, mvideoWidth, mvideoHeight)
            )
            try {
                mediaPlayerBottom?.setDataSource(bottomBlurOutputTwo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (mediaPlayerFull == null) {
            mediaPlayerFull = MediaPlayer()
            mediaPlayerFull?.setVolume(0F, 0F)
            getHeight(originalPath!!)
            var mvideoWidth = videoWidth
            var mvideoHeight = videoHeight
            mvideoWidth /= weigth
            mvideoHeight /= higth
            binding.mVideoSurfaceViewFullForLandScape.holder.setFixedSize(mvideoWidth, mvideoHeight)

            binding.mVideoSurfaceViewFullForLandScape.init(
                mediaPlayerFull, BlurEffect2(10, mvideoWidth, mvideoHeight)
            )
            try {
                mediaPlayerFull?.setDataSource(originalPath!!)
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }

    }

    private fun execFfmpegBinaryForCrop(
        command: Array<String>, type: String
    ) {
        enableStatisticsCallback("bottomCrop")
//        enableStatisticsCallbacks()
        lifecycleScope.launch {
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        dialog.dismiss()
                        isCompression = false
                        binding.videoView1.stopPlayback()
                        binding.relForGetValues.viewGone()
                        binding.relParent.viewVisible()

                        if (isLandScapeMode) {
                            binding.seekbarLayout.viewGone()
                            binding.relLayoutLandScape.viewVisible()
                            binding.mVideoSurfaceViewNoneLandScape.viewVisible()

                            setVideoViewLandScape(fullOutPut, type)
                        } else {
                            binding.seekbarLayout.viewVisible()
                            binding.relLayout.viewVisible()
                            binding.mVideoSurfaceViewNone.viewVisible()
                            setVideoView(fullOutPut)
                        }

                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isBlurred = false
//                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }
            progressId = executionId
        }
    }

    private fun execFfmpegBinaryForBottomCrop(
        command: Array<String>, outPut: String, type: String
    ) {
        lifecycleScope.launch {
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {

                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isBlurred = false
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }

            progressId = executionId
        }
    }

    private fun execFfmpegBinaryForBottomCropTop(
        command: Array<String>, outPut: String, type: String
    ) {
        lifecycleScope.launch {
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {

                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isBlurred = false
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }
            progressId = executionId
        }
    }

    /* These are the clicks of blur filters */
    override fun onClick(view: View?) {
        binding.apply {
            when (view?.id) {
                R.id.cv_non_blur -> {
                    if (isLandScapeMode) {
                        layoutBlack.viewGone()
                        nonBlur = true
                        cvTopBlur.isEnabled = true
                        cvBottomBlur.isEnabled = true
                        cvFullBlur.isEnabled = true
                        finalVideo = ""
                        blurLinearlayout.viewGone()
                        defaultView()
                        setNonBlur()
                        mVideoSurfaceViewNoneLandScape.viewVisible()
                        if (isBlurred) {
                            mVideoSurfaceViewTopForLandScape.viewGone()
                            mVideoSurfaceViewBottomForLandScape.viewGone()
                            mVideoSurfaceViewFullForLandScape.viewGone()
                            videoPauseBtnLandScape.viewVisible()
                            videoPlayBtnLandScape.viewGone()

                            isBlurred = false
                            isShowBackDialogue = false
                            mVideoSurfaceViewNoneLandScape.stopPlayback()
                            if (isMuted) {
                                mVideoSurfaceViewNoneLandScape.setVideoPath(mutedVideo)
                            } else if (isDistortion) {
                                mVideoSurfaceViewNoneLandScape.setVideoPath(distortedVideo)
                            } else {
                                mVideoSurfaceViewNoneLandScape.setVideoPath(originalPath!!)
                                disableShareAndSave()
                            }
                            mVideoSurfaceViewNoneLandScape.start()
                        } else {
                            isBlurred = false
                            isShowBackDialogue = false
                            videoPauseBtnLandScape.viewVisible()
                            videoPlayBtnLandScape.viewGone()
                            mVideoSurfaceViewNoneLandScape.stopPlayback()
                            if (isMuted) {
                                mVideoSurfaceViewNoneLandScape.setVideoPath(mutedVideo)
                            } else if (isDistortion) {
                                mVideoSurfaceViewNoneLandScape.setVideoPath(distortedVideo)
                            } else {
                                mVideoSurfaceViewNoneLandScape.setVideoPath(originalPath!!)
                                disableShareAndSave()
                            }
                            mVideoSurfaceViewNoneLandScape.start()
                        }
                    } else {
                        nonBlur = true
                        cvTopBlur.isEnabled = true
                        cvBottomBlur.isEnabled = true
                        cvFullBlur.isEnabled = true
                        finalVideo = ""
                        blurLinearlayout.viewGone()
                        defaultView()
                        setNonBlur()
                        mVideoSurfaceViewNone.viewVisible()
                        if (isBlurred) {
                            mVideoSurfaceViewTop.viewGone()
                            mVideoSurfaceViewBottom.viewGone()
                            mVideoSurfaceViewFull.viewGone()
                            exoPause.viewVisible()
                            exoPlay.viewGone()
                            isBlurred = false
                            isShowBackDialogue = false
                            mVideoSurfaceViewNone.stopPlayback()
                            if (isMuted) {
                                mVideoSurfaceViewNone.setVideoPath(mutedVideo)
                            } else if (isDistortion) {
                                mVideoSurfaceViewNone.setVideoPath(distortedVideo)
                            } else {
                                mVideoSurfaceViewNone.setVideoPath(originalPath!!)
                                disableShareAndSave()
                            }
                            mVideoSurfaceViewNone.start()
                        } else {
                            isBlurred = false
                            isShowBackDialogue = false
                            exoPause.viewVisible()
                            exoPlay.viewGone()
                            mVideoSurfaceViewNone.stopPlayback()
                            if (isMuted) {
                                mVideoSurfaceViewNone.setVideoPath(mutedVideo)
                            } else if (isDistortion) {
                                mVideoSurfaceViewNone.setVideoPath(distortedVideo)
                            } else {
                                mVideoSurfaceViewNone.setVideoPath(originalPath!!)
                                disableShareAndSave()
                            }
                            mVideoSurfaceViewNone.start()
                        }
                    }

                }
                R.id.cv_top_blur -> {
                    layoutBlack.viewGone()
                    defaultView()
                    setTopBlur()
                    blurVideoCommands(0, blurSeekbar.progress)
                }
                R.id.cv_bottom_blur -> {
                    layoutBlack.viewGone()
                    defaultView()
                    setBottomBlur()
                    blurVideoCommands(1, blurSeekbar.progress)
                }
                R.id.cv_full_blur -> {
                    layoutBlack.viewVisible()

                    defaultView()
                    setFullBlur()
                    blurVideoCommands(2, blurSeekbar.progress)
                }
            }
        }
    }

    private fun videoCutForFullVideoCommand(input: String, output: String, type: String) {
        getHeight(input)
        var crop = ""
        if (videoWidth == 1920) {
//            crop = String.format(
//                "crop=%d:%d:%d:%d",
//                videoHeight,
//                videoWidth,
//                0,
//                0
//            )
            crop = String.format("crop=%d:%d:%d:%d", videoWidth, videoHeight, 0, 0)
        } else {
            crop = String.format("crop=%d:%d:%d:%d", videoWidth, videoHeight, 0, 0)
        }

        val cmd = arrayOf(
            "-y", "-i", input, "-filter_complex", crop, "-b:v", "8000k", output
        )
        execFfmpegBinaryForCrop(cmd, type)
    }

    private fun videoCutForTopCommand(input: String, output: String, type: String) {
        getHeight(input)
        var crop = ""
        if (videoWidth == 1920) {
            if (isLandScapeMode) {
                crop = String.format(
                    "crop=%d:%d:%d:%d",
                    videoWidth,
                    videoHeight / 2,
                    0,
                    0,
                )
            } else {
                crop = String.format(
                    "crop=%d:%d:%d:%d",
                    videoHeight,
                    videoWidth / 2,
                    0,
                    0,
                )
            }

        } else {
            crop = String.format(
                "crop=%d:%d:%d:%d",
                videoWidth,
                videoHeight / 2,
                0,
                0,
            )
        }

        val cmd = arrayOf(
            "-y", "-i", input, "-filter_complex", crop, "-b:v", "8000k", output
        )
        execFfmpegBinaryForBottomCropTop(cmd, output, type)
    }

    private fun videoCutForBottomCommand(input: String, output: String, type: String) {
        getHeight(input)
        var crop = ""
        if (videoWidth == 1920) {
            if (isLandScapeMode) {
                crop = String.format(
                    "crop=%d:%d:%d:%d",
                    videoWidth,
                    videoHeight / 2,
                    0,
                    videoHeight / 2,
                )
            } else {
                crop = String.format(
                    "crop=%d:%d:%d:%d",
                    videoHeight,
                    videoWidth / 2,
                    0,
                    0,
                )
            }

        } else {
            crop = String.format(
                "crop=%d:%d:%d:%d",
                videoWidth,
                videoHeight / 2,
                0,
                videoHeight / 2,
            )
        }

        val cmd = arrayOf(
            "-y", "-i", input, "-filter_complex", crop, "-b:v", "8000k", output
        )
        execFfmpegBinaryForBottomCrop(cmd, output, type)
    }


    private fun resizeVideo(type: String) {
        val cmd = arrayOf(
            "-i",
            getNewPath(originalPath!!),
            "-vf",
            "scale=480:320",
            "-b:v",
            "8000k",
            lessResolatedFile
        )
        execFfmpegForResolutionDown(cmd, lessResolatedFile, type)
    }

    private fun compressVideo() {
        VideoCompress.compressVideoLow(getRealPathFromUri(applicationContext, Uri.parse(video)),
            compressFile,
            object : VideoCompress.CompressListener {
                override fun onStart() {
                    isInitialBlurValue = true
                    isInitialBlurValueDistortion = true
                }

                override fun onSuccess(compressVideoPath: String) {
                    lifecycleScope.launch {
                        muteCommand(compressVideoPath)
                        audioExtractCommand(compressVideoPath)

                    }
                }

                override fun onFail() {

                }

                override fun onProgress(percent: Float) {
//                    if (percent <= 100) runOnUiThread {
//                        setProgressVideoCut((percent.toInt() / 2).toFloat())
//                    }
                }
            })
    }

    private fun execFfmpegForResolutionDown(
        command: Array<String>, output: String, type: String
    ) {
        isCompression = true
        setProgressDialogue("process")
        enableStatisticsCallback("resize")
        Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
        val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {
                    isCompression = false
                    processComplete = false
                    originalPath = output
                    lifecycleScope.launch {
                        videoCutForFullVideoCommand(
                            originalPath!!, fullOutPut, "compress"
                        )

                        videoCutForTopCommand(originalPath!!, bottomBlurOutput, type)
                        videoCutForBottomCommand(originalPath!!, bottomBlurOutputTwo, type)
                    }

                }
                Config.RETURN_CODE_CANCEL -> {
                    isDistortion = false
//                    Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
                    Log.e(Config.TAG, "Async command execution canceled by user")
                }
                else -> {

                }
            }
        }

        progressId = executionId
    }

    /* This function is call for update the seekbar and show time */
    private fun updateProgress() {
        var timeLeft: Long? = null
        timeLeft =
            (binding.mVideoSurfaceViewNone.duration - binding.mVideoSurfaceViewNone.currentPosition).toLong()

        val delayMs: Long = TimeUnit.SECONDS.toMillis(1)
        mHandler?.postDelayed(updateProgressAction, delayMs)

        if (calculateTimeLeft(timeLeft) == "-12:-55") {
            duration.text = Constants.formatSeconds(totalDuration)
        } else {
            "-${this.calculateTimeLeft(timeLeft)}".also {
                duration.text = it
            }
        }


    }

    private fun updateProgressForLandScape() {
        var timeLeft: Long? = null
        timeLeft =
            (binding.mVideoSurfaceViewNoneLandScape.duration - binding.mVideoSurfaceViewNoneLandScape.currentPosition).toLong()
        val delayMs: Long = TimeUnit.SECONDS.toMillis(1)
        mHandler?.postDelayed(updateProgressActionForLandScape, delayMs)

        if (calculateTimeLeft(timeLeft) == "-12:-55") {
            durationLandScape.text = Constants.formatSeconds(totalDuration)
        } else {
            "-${this.calculateTimeLeft(timeLeft)}".also {
                durationLandScape.text = it
            }
        }
    }


    //fixme
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, p2: Boolean) {
        val whatToSay: String = progress.toString()
        binding.textView.text = whatToSay
        val `val` = progress * (seekBar.width - 3 * seekBar.thumbOffset) / seekBar.max
        "$progress%".also { binding.textView.text = it }
        binding.textView.x = seekBar.x + `val` + seekBar.thumbOffset / 2
        seekBarForSaveVideo = progress

        if (isLandScapeMode) {
            lifecycleScope.launch {
                if (topBlur) {
                    if (progress <= 2) {
                        values = 7
                        weigth = 5
                        higth = 5
                        mVideoWidth /= 5
                        mVideoHeight /= 5
                        seekbarRadius = values
//                        binding.mVideoSurfaceViewTopForLandScape.viewGone()
//                        seekbarRadius = 0
                    } else if (progress % 5 == 0) {
                        var quotent = progress / 5
                        if ((quotent % 2) == 0) {
                            var minusValue = quotent - 1
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            minusValue = setBlurRangeValue(minusValue)
                            binding.mVideoSurfaceViewTopForLandScape.viewVisible()
                            binding.mVideoSurfaceViewTopForLandScape.filter =
                                BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewTopForLandScape.viewVisible()
                            seekbarRadius = quotent
                            binding.mVideoSurfaceViewTopForLandScape.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    } else {
                        var mProgress = 5 * (ceil(abs(progress / 5).toDouble()))
                        var quotent = mProgress.toInt() / 5
                        if ((quotent % 2) == 0) {
                            if (quotent != 0) {
                                var minusValue = quotent - 1
                                mVideoWidth = videoWidth
                                mVideoHeight = videoHeight
                                minusValue = setBlurRangeValue(minusValue)
                                binding.mVideoSurfaceViewTopForLandScape.viewVisible()
                                seekbarRadius = minusValue
                                binding.mVideoSurfaceViewTopForLandScape.filter =
                                    BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                            }

                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewTopForLandScape.viewVisible()
                            seekbarRadius = quotent
                            binding.mVideoSurfaceViewTopForLandScape.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    }

                } else if (bottomBlur) {
                    if (progress <= 2) {
//                        binding.mVideoSurfaceViewBottomForLandScape.viewGone()
//                        seekbarRadius = 0
                        values = 7
                        weigth = 5
                        higth = 5
                        mVideoWidth /= 5
                        mVideoHeight /= 5
                        seekbarRadius = values

                    } else if (progress % 5 == 0) {
                        var quotent = progress / 5
                        if ((quotent % 2) == 0) {
                            var minusValue = quotent - 1
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            minusValue = setBlurRangeValue(minusValue)
                            binding.mVideoSurfaceViewBottomForLandScape.viewVisible()
                            binding.mVideoSurfaceViewBottomForLandScape.filter =
                                BlurEffect2(minusValue, mVideoWidth, mVideoHeight)

                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewBottomForLandScape.viewVisible()
                            binding.mVideoSurfaceViewBottomForLandScape.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    } else {
                        val mProgress = 5 * (ceil(abs(progress / 5).toDouble()))
                        var quotent = mProgress.toInt() / 5
                        if ((quotent % 2) == 0) {
                            if (quotent != 0) {
                                var minusValue = quotent - 1
                                mVideoWidth = videoWidth
                                mVideoHeight = videoHeight
                                minusValue = setBlurRangeValue(minusValue)
                                binding.mVideoSurfaceViewBottomForLandScape.viewVisible()
                                binding.mVideoSurfaceViewBottomForLandScape.filter =
                                    BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                            }
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewBottomForLandScape.viewVisible()
                            binding.mVideoSurfaceViewBottomForLandScape.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    }

                } else if (fullBlur) {
                    if (progress <= 2) {
//                        binding.mVideoSurfaceViewTopForLandScape.viewGone()
//                        binding.mVideoSurfaceViewBottomForLandScape.viewGone()
//                        seekbarRadius = 0
                        values = 7
                        weigth = 5
                        higth = 5
                        mVideoWidth /= 5
                        mVideoHeight /= 5
                        seekbarRadius = values
                    } else if (progress % 5 == 0) {
                        var quotent = progress / 5
                        if ((quotent % 2) == 0) {
                            var minusValue = quotent - 1
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            minusValue = setBlurRangeValue(minusValue)
                            binding.layoutBlack.viewVisible()
                            binding.mVideoSurfaceViewFullForLandScape.viewVisible()
                            binding.mVideoSurfaceViewFullForLandScape.filter =
                                BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.layoutBlack.viewVisible()
                            binding.mVideoSurfaceViewFullForLandScape.viewVisible()
                            binding.mVideoSurfaceViewFullForLandScape.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    } else {
                        val mProgress = 5 * (ceil(abs(progress / 5).toDouble()))
                        var quotent = mProgress.toInt() / 5
                        if ((quotent % 2) == 0) {
                            if (quotent != 0) {
                                var minusValue = quotent - 1
                                mVideoWidth = videoWidth
                                mVideoHeight = videoHeight
                                minusValue = setBlurRangeValue(minusValue)
                                binding.layoutBlack.viewVisible()
                                binding.mVideoSurfaceViewFullForLandScape.viewVisible()
                                binding.mVideoSurfaceViewFullForLandScape.filter =
                                    BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                            }
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.layoutBlack.viewVisible()
                            binding.mVideoSurfaceViewFullForLandScape.viewVisible()
                            binding.mVideoSurfaceViewFullForLandScape.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    }
                }
            }
        } else {
            lifecycleScope.launch {
                if (topBlur) {
                    if (progress <= 2) {
//                        binding.mVideoSurfaceViewTop.viewGone()
//                        seekbarRadius = 0
                        values = 7
                        weigth = 5
                        higth = 5
                        mVideoWidth /= 5
                        mVideoHeight /= 5
                        seekbarRadius = values
                    } else if (progress % 5 == 0) {
                        var quotent = progress / 5
                        if ((quotent % 2) == 0) {
                            var minusValue = quotent - 1
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            minusValue = setBlurRangeValue(minusValue)
                            binding.mVideoSurfaceViewTop.viewVisible()
                            binding.mVideoSurfaceViewTop.filter =
                                BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewTop.viewVisible()
                            seekbarRadius = quotent
                            binding.mVideoSurfaceViewTop.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    } else {
                        var mProgress = 5 * (ceil(abs(progress / 5).toDouble()))
                        var quotent = mProgress.toInt() / 5
                        if ((quotent % 2) == 0) {
                            if (quotent != 0) {
                                var minusValue = quotent - 1
                                mVideoWidth = videoWidth
                                mVideoHeight = videoHeight
                                minusValue = setBlurRangeValue(minusValue)
                                binding.mVideoSurfaceViewTop.viewVisible()
                                seekbarRadius = minusValue
                                binding.mVideoSurfaceViewTop.filter =
                                    BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                            }

                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewTop.viewVisible()
                            seekbarRadius = quotent
                            binding.mVideoSurfaceViewTop.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    }

                } else if (bottomBlur) {
                    if (progress <= 2) {
//                        binding.mVideoSurfaceViewBottom.viewGone()
//                        seekbarRadius = 0
                        values = 7
                        weigth = 5
                        higth = 5
                        mVideoWidth /= 5
                        mVideoHeight /= 5
                        seekbarRadius = values

                    } else if (progress % 5 == 0) {
                        var quotent = progress / 5
                        if ((quotent % 2) == 0) {
                            var minusValue = quotent - 1
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            minusValue = setBlurRangeValue(minusValue)
                            binding.mVideoSurfaceViewBottom.viewVisible()
                            binding.mVideoSurfaceViewBottom.filter =
                                BlurEffect2(minusValue, mVideoWidth, mVideoHeight)

                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewBottom.viewVisible()
                            binding.mVideoSurfaceViewBottom.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    } else {
                        val mProgress = 5 * (ceil(abs(progress / 5).toDouble()))
                        var quotent = mProgress.toInt() / 5
                        if ((quotent % 2) == 0) {
                            if (quotent != 0) {
                                var minusValue = quotent - 1
                                mVideoWidth = videoWidth
                                mVideoHeight = videoHeight
                                minusValue = setBlurRangeValue(minusValue)
                                binding.mVideoSurfaceViewBottom.viewVisible()
                                binding.mVideoSurfaceViewBottom.filter =
                                    BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                            }
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewBottom.viewVisible()
                            binding.mVideoSurfaceViewBottom.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    }

                } else if (fullBlur) {
                    if (progress <= 2) {
                        values = 7
                        weigth = 5
                        higth = 5
                        mVideoWidth /= 5
                        mVideoHeight /= 5
                        seekbarRadius = values

                    } else if (progress % 5 == 0) {
                        var quotent = progress / 5
                        if ((quotent % 2) == 0) {
                            var minusValue = quotent - 1
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            minusValue = setBlurRangeValue(minusValue)
                            binding.mVideoSurfaceViewFull.viewVisible()
                            binding.mVideoSurfaceViewFull.filter =
                                BlurEffect2(minusValue, mVideoWidth, mVideoHeight)

                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewFull.viewVisible()
                            binding.mVideoSurfaceViewFull.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    } else {
                        val mProgress = 5 * (ceil(abs(progress / 5).toDouble()))
                        var quotent = mProgress.toInt() / 5
                        if ((quotent % 2) == 0) {
                            if (quotent != 0) {
                                var minusValue = quotent - 1
                                mVideoWidth = videoWidth
                                mVideoHeight = videoHeight
                                minusValue = setBlurRangeValue(minusValue)
                                binding.mVideoSurfaceViewFull.viewVisible()
                                binding.mVideoSurfaceViewFull.filter =
                                    BlurEffect2(minusValue, mVideoWidth, mVideoHeight)
                            }
                        } else {
                            mVideoWidth = videoWidth
                            mVideoHeight = videoHeight
                            quotent = setBlurRangeValue(quotent)
                            binding.mVideoSurfaceViewFull.viewVisible()
                            binding.mVideoSurfaceViewFull.filter =
                                BlurEffect2(quotent, mVideoWidth, mVideoHeight)
                        }
                    }

                }
            }
        }

    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    override fun onStopTrackingTouch(seeBar: SeekBar?) {

    }

    private fun setBlurRangeValue(value: Int): Int {
        when (value) {
            //5-15
            1 -> {
                values = 7
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //15-25
            3 -> {
                values = 9
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //25-35
            5 -> {
                values = 11
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //35-45
            7 -> {
                values = 15
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //45-55
            9 -> {
                values = 17
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //55-65
            11 -> {
                values = 19
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //65-75
            13 -> {
                values = 21
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            //75-85
            13 -> {
                values = 23
                weigth = 5
                higth = 5
                mVideoWidth /= 5
                mVideoHeight /= 5
                seekbarRadius = values
            }
            else -> {
                values = 25
                weigth = 8
                higth = 8
                mVideoWidth /= 8
                mVideoHeight /= 8
                seekbarRadius = values
            }
        }

        return values
    }

/* In this function we execute all blur video commands video different checks
*  https://github.com/tanersener/mobile-ffmpeg
*  */

    private fun blurVideoCommands(type: Int, radius: Int) {
        when (type) {
            0 -> {
                if (isDistortion) setTopBlurView(distortedVideo)
                else if (isMuted) setTopBlurView(mutedVideo)
                else setTopBlurView(fullOutPut)
            }
            1 -> {
                if (isDistortion) setBottomBlurView(distortedVideo)
                else if (isMuted) setBottomBlurView(mutedVideo)
                else setBottomBlurView(fullOutPut)
            }
            else -> {
                if (isDistortion) setFullBlurView(distortedVideo)
                else if (isMuted) setFullBlurView(mutedVideo)
                else setFullBlurView(fullOutPut)
            }
        }
    }

    private fun setBottomBlurView(path: String) {
        topBlur = false
        bottomBlur = true
        fullBlur = false
        isBlurred = true
        nonBlur = false
        isShowBackDialogue = true
        enableShareAndSave()
        binding.apply {
            cvTopBlur.isEnabled = true
            cvBottomBlur.isEnabled = false
            cvFullBlur.isEnabled = true
            blurLinearlayout.viewVisible()
        }
        resetPlayer()
    }

    private fun setTopBlurView(path: String) {
        topBlur = true
        bottomBlur = false
        fullBlur = false
        isBlurred = true
        nonBlur = false
        isShowBackDialogue = true
        enableShareAndSave()
        binding.apply {
            cvTopBlur.isEnabled = false
            cvBottomBlur.isEnabled = true
            cvFullBlur.isEnabled = true
            blurLinearlayout.viewVisible()
        }
        resetPlayer()
    }

    private fun setFullBlurView(path: String) {
        isBlurred = true
        nonBlur = false
        topBlur = false
        bottomBlur = false
        fullBlur = true
        isShowBackDialogue = true
        enableShareAndSave()
        binding.apply {
            cvTopBlur.isEnabled = true
            cvBottomBlur.isEnabled = true
            cvFullBlur.isEnabled = false
            blurLinearlayout.viewVisible()
        }
        resetPlayer()
    }

    private fun setPlayerSeekBar() {
        mHandler = Handler(Looper.getMainLooper())
        mHandler?.post(updateProgressAction)

        val updateSeekBar: Runnable = object : Runnable {
            override fun run() {
                var mCurrentPosition: Int? = null
                if (isLandScapeMode) {
                    mCurrentPosition = binding.mVideoSurfaceViewNoneLandScape.currentPosition
                    binding.seekbar.max = binding.mVideoSurfaceViewNoneLandScape.duration / 1000
                } else {
                    mCurrentPosition = binding.mVideoSurfaceViewNone.currentPosition
                    binding.seekbar.max = binding.mVideoSurfaceViewNone.duration / 1000
                }
                binding.seekbar.progress = mCurrentPosition / 1000
                binding.seekbar.postDelayed(this, 500)

            }
        }
        runOnUiThread(updateSeekBar)
        binding.seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (isLandScapeMode) {
                    binding.mVideoSurfaceViewNoneLandScape.seekTo(seekBar.progress * 1000)
                    binding.mVideoSurfaceViewNoneLandScape.start()
                    binding.videoPlayBtnLandScape.viewGone()
                    binding.videoPauseBtnLandScape.viewVisible()

                } else {
                    binding.mVideoSurfaceViewNone.seekTo(seekBar.progress * 1000)
                    binding.mVideoSurfaceViewNone.start()
                    binding.exoPlay.viewGone()
                    binding.exoPause.viewVisible()
                }

                if (topBlur) {
                    if (mediaPlayerTop != null) {
                        mediaPlayerTop?.seekTo(seekBar.progress * 1000)
                        mediaPlayerTop?.start()
                    }
                } else if (bottomBlur) {
                    if (mediaPlayerBottom != null) {
                        mediaPlayerBottom?.seekTo(seekBar.progress * 1000)
                        mediaPlayerBottom?.start()
                    }

                } else if (fullBlur) {
                    if (mediaPlayerFull != null) {
                        mediaPlayerFull?.seekTo(seekBar.progress * 1000)
                        mediaPlayerFull?.start()
                    }
                }
                runOnUiThread(updateSeekBar)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                handler?.removeCallbacks(updateProgressAction)
                if (isLandScapeMode) {
                    binding.mVideoSurfaceViewNoneLandScape.pause()
                } else {
                    binding.mVideoSurfaceViewNone.pause()
                }
                if (topBlur) {
                    mediaPlayerTop?.pause()
                } else if (bottomBlur) {
                    mediaPlayerBottom?.pause()

                } else if (fullBlur) {
                    mediaPlayerFull?.pause()
                }
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    if (isLandScapeMode) {
                        binding.mVideoSurfaceViewNoneLandScape.seekTo(progress * 1000)
                    } else {
                        binding.mVideoSurfaceViewNone.seekTo(progress * 1000)
                    }

                }

                if (topBlur) {
                    if (mediaPlayerTop != null && fromUser) {
                        mediaPlayerTop?.seekTo(progress * 1000)
                    }
                } else if (bottomBlur) {
                    if (mediaPlayerBottom != null && fromUser) {
                        mediaPlayerBottom?.seekTo(progress * 1000)
                    }

                } else if (fullBlur) {
                    if (mediaPlayerFull != null && fromUser) {
                        mediaPlayerFull?.seekTo(progress * 1000)

                    }
                }
            }
        })
    }

    private fun setPlayerSeekBarForLandScape() {
        mHandler = Handler(Looper.getMainLooper())
        mHandler?.post(updateProgressActionForLandScape)

        val updateSeekBar: Runnable = object : Runnable {
            override fun run() {
                var mCurrentPosition: Int? = null
                mCurrentPosition = binding.mVideoSurfaceViewNoneLandScape.currentPosition
                binding.seekbarForLandScape.max =
                    binding.mVideoSurfaceViewNoneLandScape.duration / 1000
                binding.seekbarForLandScape.progress = mCurrentPosition / 1000
                binding.seekbarForLandScape.postDelayed(this, 500)

            }
        }
        runOnUiThread(updateSeekBar)
        binding.seekbarForLandScape.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {
                binding.mVideoSurfaceViewNoneLandScape.seekTo(seekBar.progress * 1000)
                binding.mVideoSurfaceViewNoneLandScape.start()
                binding.videoPlayBtnLandScape.viewGone()
                binding.videoPauseBtnLandScape.viewVisible()

                if (topBlur) {
                    if (mediaPlayerTop != null) {
                        mediaPlayerTop?.seekTo(seekBar.progress * 1000)
                        mediaPlayerTop?.start()
                    }
                } else if (bottomBlur) {
                    if (mediaPlayerBottom != null) {
                        mediaPlayerBottom?.seekTo(seekBar.progress * 1000)
                        mediaPlayerBottom?.start()
                    }

                } else if (fullBlur) {
                    if (mediaPlayerFull != null) {
                        mediaPlayerFull?.seekTo(seekBar.progress * 1000)
                        mediaPlayerFull?.start()
                    }
                }
                runOnUiThread(updateSeekBar)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                handler?.removeCallbacks(updateProgressActionForLandScape)
                binding.mVideoSurfaceViewNoneLandScape.pause()

                if (topBlur) {
                    mediaPlayerTop?.pause()
                } else if (bottomBlur) {
                    mediaPlayerBottom?.pause()

                } else if (fullBlur) {
                    mediaPlayerFull?.pause()
                }
            }

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.mVideoSurfaceViewNoneLandScape.seekTo(progress * 1000)
                }

                if (topBlur) {
                    if (mediaPlayerTop != null && fromUser) {
                        mediaPlayerTop?.seekTo(progress * 1000)
                    }
                } else if (bottomBlur) {
                    if (mediaPlayerBottom != null && fromUser) {
                        mediaPlayerBottom?.seekTo(progress * 1000)
                    }

                } else if (fullBlur) {
                    if (mediaPlayerFull != null && fromUser) {
                        mediaPlayerFull?.seekTo(progress * 1000)

                    }
                }
            }
        })
    }

/* These function is command of topBlur
*  https://github.com/tanersener/mobile-ffmpeg
*  */

    private fun topBlurCommand(
        input: String, output: String, radius: Int, blurType: String, buttonType: String
    ) {
        getHeight(input)
        val cmd = arrayOf(
            "-y",
            "-i",
            input,
            "-filter_complex",
            "[0:v]crop=" + videoWidth + ":" + videoHeight / 2 + ":" + videoHeight % 2 + ":" + 0 + ",gblur=$radius[blurred];[0:v][blurred]overlay=" + videoHeight % 2 + ":" + 0 + "[v]",
            "-map",
            "[v]",
            "-map",
            "0:a?",
            "-c:a",
            "copy",
            "-crf",
            "25",
            "-b:v",
            "8000k",
            "-preset",
            "ultrafast",
            output,
            "-hide_banner"
        )
        execFfmpegBinaryForBlur(cmd, output, blurType, buttonType)
    }

    /* These function is command of bottomBlur
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */
    private fun bottomBlurCommand(
        input: String, output: String, radius: Int, blurType: String, buttonType: String
    ) {
        getHeight(input)
        val cmd = arrayOf(
            "-y",
            "-i",
            input,
            "-filter_complex",
            "[0:v]crop=" + videoWidth + ":" + videoHeight / 2 + ":" + 0 + ":" + videoHeight / 2 + ",gblur=$radius[blurred];[0:v][blurred]overlay=" + 0 + ":" + videoHeight / 2 + "[v]",
            "-map",
            "[v]",
            "-map",
            "0:a?",
            "-c:a",
            "copy",
            "-crf",
            "25",
            "-b:v",
            "8000k",
            "-preset",
            "ultrafast",
            output,
            "-hide_banner"
        )
        execFfmpegBinaryForBlur(cmd, output, blurType, buttonType)

    }

    /* These function is command of fullBlur
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */
    private fun fullBlurCommand(
        input: String, output: String, radius: Int, blurType: String, buttonType: String
    ) {
        getHeight(input)
        val cmd = arrayOf(
            "-y",
            "-i",
            input,
            "-filter_complex",
            "[0:v]crop=$videoWidth:$videoHeight:0:0,gblur=$radius[blurred];[0:v][blurred]overlay=0:0[v]",
            "-map",
            "[v]",
            "-map",
            "0:a?",
            "-c:a",
            "copy",
            "-crf",
            "25",
            "-b:v",
            "8000k",
            "-preset",
            "ultrafast",
            output,
            "-hide_banner"
        )
        execFfmpegBinaryForBlur(cmd, output, blurType, buttonType)

    }

    /* This function is actually execution of blur commands */
    private fun execFfmpegBinaryForBlur(
        command: Array<String>, outPut: String, blurType: String, buttonType: String
    ) {

        when (blurType) {
            "topBlur" -> {
                binding.apply {


                    if (isLandScapeMode) {
                        videoPauseBtnLandScape.viewGone()
                        videoPlayBtnLandScape.viewVisible()
                        mVideoSurfaceViewNoneLandScape.pause()
                    } else {
                        exoPause.viewGone()
                        exoPlay.viewVisible()
                        mVideoSurfaceViewNone.pause()
                    }

                    mediaPlayerTop?.pause()
                }
            }
            "bottomBlur" -> {
                binding.apply {
                    if (isLandScapeMode) {
                        videoPauseBtnLandScape.viewGone()
                        videoPlayBtnLandScape.viewVisible()
                        mVideoSurfaceViewNoneLandScape.pause()
                    } else {
                        exoPause.viewGone()
                        exoPlay.viewVisible()
                        mVideoSurfaceViewNone.pause()
                    }
                    mediaPlayerBottom?.pause()
                }
            }
            "fullBlur" -> {
                binding.apply {

                    if (isLandScapeMode) {
                        videoPauseBtnLandScape.viewGone()
                        videoPlayBtnLandScape.viewVisible()
                        mVideoSurfaceViewNoneLandScape.pause()
                    } else {
                        exoPause.viewGone()
                        exoPlay.viewVisible()
                        mVideoSurfaceViewNone.pause()
                    }
                    mediaPlayerFull?.pause()
                }
            }
        }
        setProgressDialogue("blur")
        enableStatisticsCallback("")
        lifecycleScope.launch {
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        dialog.dismiss()
                        if (buttonType == "save") {
                            setSave(outPut, "blur")
                        } else if (buttonType == "share") {
                            setShare(outPut)
                        }

                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isBlurred = false
//                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }

            progressId = executionId
        }
    }


    /* This function is mute the video sound */
    private fun muteVideoCommands() {
        if (isBlurred) {
//            if (mutedVideo != "") {
//                resetPlayerForMute(mutedVideo)
//            } else {
//
//            }
            muteCommand(fullOutPut)
        } else if (isDistortion) {
            muteCommand(distortedVideo)
        } else {
            muteCommand(fullOutPut)
        }
    }

    private fun muteCommand(input: String) {
        fileForMute =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        if (!fileForMute!!.exists()) {
            fileForMute!!.mkdirs()
        }
        val downloadFile = File(fileForMute, generateBlurName() + ".mp4")
        val outPutForMute = downloadFile.absolutePath
        val cmd = arrayOf("-i", input, "-c", "copy", "-an", outPutForMute)
        execFfmpegBinaryForMuteAudio(cmd, outPutForMute)
    }

    /* This function is execute the mute video commands */
    private fun execFfmpegBinaryForMuteAudio(
        command: Array<String>, outPut: String
    ) {
        lifecycleScope.launch {
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        if (isInitialBlurValue) {
                            finalMuteVideo = outPut
                            isInitialBlurValue = false
                        } else {
                            isShowBackDialogue = true
                            finalVideo = ""
                            enableShareAndSave()
                            mutedVideo = outPut
                            isMuted = true
                            isDistortion = false
                            binding.apply {
                                buttonLayouts.apply {
                                    buttonRemoveAudio.isEnabled = false
                                    buttonRemoveDistortion.isEnabled = true
                                }
                                ivNoEffect.apply {
                                    setImageDrawable(
                                        ContextCompat.getDrawable(
                                            this@VideoEditActivity, R.drawable.ic_no_effect
                                        )
                                    )
                                    setColorFilter(
                                        ContextCompat.getColor(
                                            context, R.color.white
                                        )
                                    )
                                }

                                tvNoEffect.setTextColor(
                                    ContextCompat.getColor(
                                        this@VideoEditActivity, R.color.white
                                    )
                                )
                                buttonNoEffect.apply {
                                    background = ContextCompat.getDrawable(
                                        this@VideoEditActivity,
                                        R.drawable.ss_corner_round_white_blue_corener
                                    )
                                    isEnabled = true
                                }
                            }
                            resetPlayerForMute(mutedVideo)
                        }
                    }
                    Config.RETURN_CODE_CANCEL -> {
//                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }
            progressId = executionId
        }
    }


    /* This function is extract the audio or wav file from video or mp4. */
    private fun audioExtractCommand(path: String) {
        folder = File(Environment.getExternalStorageDirectory().toString() + "/extractAudio")
        if (!folder!!.exists()) {
            folder!!.mkdir()
        }
        val outputForExtractAudio: String =
            getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + "/extract_audio_output.mp3"

        val cmd = arrayOf(
            "-y", "-i", // input path
            path, "-f",  // output format
            "wav", "-ab",  // encode speed
            "64k", "-vn",  // dont want video
            outputForExtractAudio // output path
        )
        execFfmpegForExtractVoiceFromVideo(
            cmd, outputForExtractAudio, path
        )
    }

    /* This function is execute mp4 to wav command */
    private fun execFfmpegForExtractVoiceFromVideo(
        command: Array<String>, strAudioPath: String, inputForAttachVoice: String
    ) {
        lifecycleScope.launch {
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        if (isLandScapeMode) {
                            audioChangeLayoutForLandScape.viewVisible()
                        } else {
                            audioChangeLayout.viewVisible()
                        }

                        doSoundTouchProcessing(strAudioPath, inputForAttachVoice)
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isDistortion = false
//                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }

                }
            }
            progressId = executionId
        }
    }

    /*  Function that does the SoundTouch processing */
    private fun doSoundTouchProcessing(input: String, inputForAttachVoice: String): Long {
        val st = SoundTouch()
        st.apply {
            setTempo(1f)
            setPitchSemiTones(6f)
        }
        val extortedAudioOutput: String =
            getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + "/pitch_change_output.mp3"
        val res = st.processFile(input, extortedAudioOutput)
        val folder = File(Environment.getExternalStorageDirectory().toString() + "/ExtractedVideos")
        if (!folder.exists()) {
            folder.mkdir()
        }
        extractAudioFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        if (!extractAudioFile!!.exists()) {
            extractAudioFile!!.mkdirs()
        }
        val downloadFile = File(extractAudioFile, generateBlurName() + ".mp4")
        finalOutput = downloadFile.absolutePath

        val cmd = arrayOf(
            "-i",
            inputForAttachVoice,
            "-i",
            extortedAudioOutput,
            "-c:v",
            "copy",
            "-c:a",
            "aac",
            "-map",
            "0:v:0",
            "-map",
            "1:a:0",
            "-shortest",
            finalOutput
        )

        execFfmpegForAttachAudioWithVideo(cmd, finalOutput)
        if (res != 0) {
//            Toast.makeText(this, "Failure: $err", Toast.LENGTH_SHORT).show()
            return -1L
        }
        return 0L
    }


    /*  Function that attach the distorted voice to video again */
    private fun execFfmpegForAttachAudioWithVideo(
        command: Array<String>, distortedFilePath: String
    ) {

        val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {

                    if (isInitialBlurValueDistortion) {
                        isInitialBlurValueDistortion = false
                        finalDistortedVideo = distortedFilePath
                    } else {
                        isShowBackDialogue = true
                        finalVideo = ""
                        isDistortion = true
                        isMuted = false
                        if (isLandScapeMode) {
                            audioDistortionForLandScape.viewVisible()
                            audioRemoveForLandScape.viewGone()
                        } else {
                            audioDistortion.viewVisible()
                            audioRemove.viewGone()
                        }

                        enableShareAndSave()
                        binding.apply {
                            buttonNoEffect.apply {
                                background = ContextCompat.getDrawable(
                                    this@VideoEditActivity,
                                    R.drawable.ss_corner_round_white_blue_corener
                                )
                                isEnabled = true
                            }
                            buttonLayouts.apply {
                                buttonRemoveAudio.isEnabled = true
                                buttonRemoveDistortion.isEnabled = false
                            }
                            ivNoEffect.apply {
                                setImageDrawable(
                                    ContextCompat.getDrawable(
                                        this@VideoEditActivity, R.drawable.ic_no_effect
                                    )
                                )
                                setColorFilter(
                                    ContextCompat.getColor(
                                        context, R.color.white
                                    )
                                )
                            }

                            tvNoEffect.setTextColor(
                                ContextCompat.getColor(
                                    this@VideoEditActivity, R.color.white
                                )
                            )

                            resetAudioButtonItems()
                            buttonLayouts.buttonRemoveDistortion.background =
                                ContextCompat.getDrawable(
                                    this@VideoEditActivity, R.drawable.ss_corner_round_light_blue
                                )

                            buttonLayouts.ivAddDistortion.apply {
                                setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context, R.drawable.ic_disortion
                                    )
                                )
                                setColorFilter(
                                    ContextCompat.getColor(
                                        context, R.color.white
                                    )
                                )
                            }

                            buttonLayouts.tvAddDistortion.setTextColor(
                                ContextCompat.getColor(
                                    this@VideoEditActivity, R.color.white
                                )
                            )


                        }
                        distortedVideo = distortedFilePath
                        resetPlayerForMute(distortedFilePath)
                    }


//                    resetPlayer(distortedFilePath)
                }
                Config.RETURN_CODE_CANCEL -> {
                    isDistortion = false
//                    Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                    Log.e(Config.TAG, "Async command execution canceled by user")
                }
            }
        }
        progressId = executionId
    }

    /* Save video to gallery by creating folder*/
    private fun saveVideo() {
        if (isBlurred) {
            if (topBlur) {
                if (isLargeSizeVideo) {
                    if (isMuted) {
                        topBlurCommand(
                            finalMuteVideo,
                            topBlurFinalOutput,
                            seekBarForSaveVideo,
                            "topBlur",
                            "save"
                        )
                    } else if (isDistortion) {
                        topBlurCommand(
                            finalDistortedVideo,
                            topBlurFinalOutput,
                            seekBarForSaveVideo,
                            "topBlur",
                            "save"
                        )
                    } else {
                        topBlurCommand(
                            compressFile,
                            topBlurFinalOutput,
                            seekBarForSaveVideo, "topBlur", "save"
                        )

                    }
                } else {
                    if (isMuted) {
                        topBlurCommand(
                            mutedVideo, topBlurFinalOutput, seekBarForSaveVideo, "topBlur", "save"
                        )
                    } else if (isDistortion) {
                        topBlurCommand(
                            distortedVideo,
                            topBlurFinalOutput,
                            seekBarForSaveVideo,
                            "topBlur",
                            "save"
                        )
                    } else {
                        topBlurCommand(
                            originalPath!!,
                            topBlurFinalOutput,
                            seekBarForSaveVideo,
                            "topBlur",
                            "save"
                        )
                    }
                }
            } else if (bottomBlur) {
                if (isLargeSizeVideo) {
                    if (isMuted) {
                        bottomBlurCommand(
                            finalMuteVideo,
                            bottomBlurFinalOutput,
                            seekBarForSaveVideo,
                            "bottomBlur",
                            "save"
                        )
                    } else if (isDistortion) {
                        bottomBlurCommand(
                            finalDistortedVideo,
                            bottomBlurFinalOutput,
                            seekBarForSaveVideo,
                            "bottomBlur",
                            "save"
                        )
                    } else {
                        bottomBlurCommand(
                            compressFile,
                            bottomBlurFinalOutput,
                            seekBarForSaveVideo,
                            "bottomBlur",
                            "save"
                        )

                    }
                } else {
                    if (isMuted) {
                        bottomBlurCommand(
                            mutedVideo,
                            bottomBlurFinalOutput,
                            seekBarForSaveVideo,
                            "bottomBlur",
                            "save"
                        )
                    } else if (isDistortion) {
                        bottomBlurCommand(
                            distortedVideo,
                            bottomBlurFinalOutput,
                            seekBarForSaveVideo,
                            "bottomBlur",
                            "save"
                        )
                    } else {
                        bottomBlurCommand(
                            originalPath!!,
                            bottomBlurFinalOutput,
                            seekBarForSaveVideo,
                            "bottomBlur",
                            "save"
                        )
                    }
                }


            } else if (fullBlur) {
                if (isLargeSizeVideo) {
                    if (isMuted) {
                        fullBlurCommand(
                            finalMuteVideo, fullBlurOutput, seekBarForSaveVideo, "fullBlur", "save"
                        )
                    } else if (isDistortion) {
                        fullBlurCommand(
                            finalDistortedVideo,
                            fullBlurOutput,
                            seekBarForSaveVideo,
                            "fullBlur",
                            "save"
                        )
                    } else {
                        fullBlurCommand(
                            compressFile, fullBlurOutput, seekBarForSaveVideo, "fullBlur", "save"
                        )
                    }
                } else {
                    if (isMuted) {
                        fullBlurCommand(
                            mutedVideo, fullBlurOutput, seekBarForSaveVideo, "fullBlur", "save"
                        )
                    } else if (isDistortion) {
                        fullBlurCommand(
                            distortedVideo, fullBlurOutput, seekBarForSaveVideo, "fullBlur", "save"
                        )
                    } else {
                        fullBlurCommand(
                            originalPath!!, fullBlurOutput, seekBarForSaveVideo, "fullBlur", "save"
                        )
                    }
                }

            }

        } else {
            if (isLargeSizeVideo) {
                if (isMuted) {
                    binding.apply {
                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewGone()
                            videoPlayBtnLandScape.viewVisible()
                            mVideoSurfaceViewNoneLandScape.pause()
                        } else {
                            exoPause.viewGone()
                            exoPlay.viewVisible()
                            mVideoSurfaceViewNone.pause()
                        }
                    }
                    setSave(finalMuteVideo, "mute")
                } else if (isDistortion) {
                    binding.apply {

                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewGone()
                            videoPlayBtnLandScape.viewVisible()
                            mVideoSurfaceViewNoneLandScape.pause()
                        } else {
                            exoPause.viewGone()
                            exoPlay.viewVisible()
                            mVideoSurfaceViewNone.pause()
                        }
                    }
                    setSave(finalDistortedVideo, "distortion")
                }
            } else {
                if (isMuted) {
                    binding.apply {
                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewGone()
                            videoPlayBtnLandScape.viewVisible()
                            mVideoSurfaceViewNoneLandScape.pause()
                        } else {
                            exoPause.viewGone()
                            exoPlay.viewVisible()
                            mVideoSurfaceViewNone.pause()
                        }
                    }
                    setSave(mutedVideo, "mute")
                } else if (isDistortion) {
                    binding.apply {
                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewGone()
                            videoPlayBtnLandScape.viewVisible()
                            mVideoSurfaceViewNoneLandScape.pause()
                        } else {
                            exoPause.viewGone()
                            exoPlay.viewVisible()
                            mVideoSurfaceViewNone.pause()
                        }
                    }
                    setSave(distortedVideo, "distortion")
                }
            }
        }
    }

    private fun setSave(finalVideo: String, type: String) {
        isShowBackDialogue = false
        val pathOf = finalVideo
        val rootPath = File(
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                resources.getString(R.string.app_name) + " Videos"
            ).toString()
        )
        if (!rootPath.exists()) {
            rootPath.mkdirs()
        }
        val input = File(pathOf)
        val outPut1 = File(rootPath, input.name)
        moveFile(pathOf, outPut1.absolutePath)
        setSaveDialogue(type)
    }


    /* Set the save dialogue */
    private fun setSaveDialogue(type: String) {
        val dialog = Dialog(this@VideoEditActivity)

        dialog.apply {
            setContentView(R.layout.custom_progressbar)
            window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val progress = findViewById<ProgressBar>(R.id.progressBar)
            val textView = findViewById<TextView>(R.id.tv)
            val ivDone = findViewById<ImageView>(R.id.iv_done)
            val mHandler = Handler(Looper.getMainLooper())

            if (type == "blur") {
                if (topBlur) {
                    binding.apply {

                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewVisible()
                            videoPlayBtnLandScape.viewGone()
                            if (mVideoSurfaceViewNoneLandScape.isPlaying) {
                                mVideoSurfaceViewNoneLandScape.resume()
                            } else {
                                mVideoSurfaceViewNoneLandScape.start()
                            }
                        } else {
                            exoPause.viewVisible()
                            exoPlay.viewGone()
                            if (mVideoSurfaceViewNone.isPlaying) {
                                mVideoSurfaceViewNone.resume()
                            } else {
                                mVideoSurfaceViewNone.start()
                            }
                        }

                        mediaPlayerTop?.start()
                    }
                } else if (bottomBlur) {
                    binding.apply {

                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewVisible()
                            videoPlayBtnLandScape.viewGone()
                            if (mVideoSurfaceViewNoneLandScape.isPlaying) {
                                mVideoSurfaceViewNoneLandScape.resume()
                            } else {
                                mVideoSurfaceViewNoneLandScape.start()
                            }
                        } else {
                            exoPause.viewVisible()
                            exoPlay.viewGone()
                            if (mVideoSurfaceViewNone.isPlaying) {
                                mVideoSurfaceViewNone.resume()
                            } else {
                                mVideoSurfaceViewNone.start()
                            }
                        }

                        mediaPlayerBottom?.start()
                    }
                } else {
                    binding.apply {

                        if (isLandScapeMode) {
                            videoPauseBtnLandScape.viewVisible()
                            videoPlayBtnLandScape.viewGone()
                            if (mVideoSurfaceViewNoneLandScape.isPlaying) {
                                mVideoSurfaceViewNoneLandScape.resume()
                            } else {
                                mVideoSurfaceViewNoneLandScape.start()
                            }
                        } else {
                            exoPause.viewVisible()
                            exoPlay.viewGone()
                            if (mVideoSurfaceViewNone.isPlaying) {
                                mVideoSurfaceViewNone.resume()
                            } else {
                                mVideoSurfaceViewNone.start()
                            }
                        }

                        mediaPlayerFull?.start()
                    }
                }
                ivDone.viewVisible()
                progress.viewGone()
                window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCancelable(true)
                show()
                textView.text = getString(R.string.video_saved)
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    cancel()
                }, 4000)
            } else {
                var progressBarStatus = 0
                Thread {
                    while (progressBarStatus < 100) {
                        // sleeping for 20milliseconds
                        try {
                            Thread.sleep(20)
                            progressBarStatus++
                        } catch (e: InterruptedException) {
                            e.printStackTrace()
                        }

                        mHandler.post {
                            if (progressBarStatus == 99) {
                                ivDone.viewVisible()
                                progress.viewGone()
                                binding.apply {

                                    if (isLandScapeMode) {
                                        videoPauseBtnLandScape.viewVisible()
                                        videoPlayBtnLandScape.viewGone()
                                        if (mVideoSurfaceViewNoneLandScape.isPlaying) {
                                            mVideoSurfaceViewNoneLandScape.resume()
                                        } else {
                                            mVideoSurfaceViewNoneLandScape.start()
                                        }
                                    } else {
                                        exoPause.viewVisible()
                                        exoPlay.viewGone()
                                        if (mVideoSurfaceViewNone.isPlaying) {
                                            mVideoSurfaceViewNone.resume()
                                        } else {
                                            mVideoSurfaceViewNone.start()
                                        }
                                    }

                                }
                            } else {
                                progress.progress = progressBarStatus
                                if (progressBarStatus != 100) {
                                    textView.text = "Exporting Video ${progressBarStatus}%"
                                } else {
                                    textView.text = "Video Saved"

                                }
//                                progress.progress = progressBarStatus
//                                "Exporting Video ${progressBarStatus}%".also { textView.text = it }
                            }
                        }
                    }
                }.start()
                window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setCancelable(true)
                show()
                val handler = Handler(Looper.getMainLooper())
                handler.postDelayed({
                    cancel()
                }, 4000)
            }
        }
    }

    private fun setProgressDialogue(type: String) {
        dialog = Dialog(this@VideoEditActivity)
        dialog.apply {
            setContentView(R.layout.custom_progressbar)
            progressbar = findViewById(R.id.progressBar)
            textView = findViewById(R.id.tv)
            if (type == "process") {
                textView?.text = "Video Processing 0%"
            } else {
                textView?.text = "Exporting Video 0%"
            }

            window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            show()

        }
    }

    private fun showProgressForCrop(progressBarStatus: Float) {
        if (progressBarStatus <= 100.0) {
            if (!listForLastFifty.contains(progressBarStatus.toString())) {
                listForLastFifty.add(progressBarStatus.toString())
                var value = counter2++
                val progress = "Video Processing ${value + 50}%"
                progressbar?.progress = value + 50
                textView?.text = progress
            }
        }
    }

    private fun showProgressForResize(progressBarStatus: Float) {
        if (progressBarStatus <= 50.0) {
            if (!listForFirstFifty.contains(progressBarStatus.toString())) {
                listForFirstFifty.add(progressBarStatus.toString())
                var value = counter++
                val progress = "Video Processing ${value}%"
                progressbar?.progress = value
                textView?.text = progress
                isHundredValue = true

            }
//            if (value <= 50) {
//                val progress = "Video Processing ${value}%"
//                progressbar?.progress = value
//                textView?.text = progress
//                isHundredValue = true
//                Log.d("djdjdjjdjddj", "" + progressBarStatus)
//            }
        } else {
            if (!isHundredValue) {
                progressbar?.progress = 0
                textView?.text = "0%"
            }
        }

    }


    private fun showProgressForBlur(progressBarStatus: Float, type: String) {
        if (progressBarStatus <= 100.0) {
            val progress: String
            progress = if (type == "bottomCrop") {
                "Video Processing ${progressBarStatus.toLong()}%"
            } else {
                "Exporting Video ${progressBarStatus.toLong()}%"
            }
            progressbar?.progress = progressBarStatus.toInt()
            textView?.text = progress
        }
    }


    /* Enable callBack that show progress of video that is converted */
    private fun enableStatisticsCallback(type: String) {
        Config.enableStatisticsCallback { newStatistics: Statistics ->
            statistics = newStatistics
            val timeInMilliseconds: Int = statistics!!.time
            if (timeInMilliseconds > 0) {
                val totalVideoDuration: Int = dur.toInt()
                val completePercentage =
                    BigDecimal(timeInMilliseconds).multiply(BigDecimal(100)).divide(
                        BigDecimal(totalVideoDuration), 0, BigDecimal.ROUND_HALF_UP
                    ).toString()

                lifecycleScope.launch {
                    when (type) {
                        "bottomCrop" -> {
                            val value = String.format("%s", completePercentage).toInt() / 2
                            val sum = value + 50
                            showProgressForCrop(sum.toFloat())
                        }
                        "resize" -> {
                            val value = String.format("%s", completePercentage).toInt() / 2
                            showProgressForResize(
                                value.toFloat()
                            )
                        }
                        else -> {
                            showProgressForBlur(
                                String.format("%s", completePercentage).toFloat(), type
                            )
                        }
                    }
                }

            }
            throw AndroidRuntimeException("I am test exception thrown by test application")
        }
    }


    /* This function is get the height and width of video */
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
        Log.d("ddddddddddddd", "width$videoWidth")
        Log.d("ddddddddddddd", "height$videoHeight")
    }

    /* This function is get total duration of video */
    private fun getTotalDuration(uri: String) {
        totalDuration = Constants.getDuration(this@VideoEditActivity, Uri.parse(uri))
        lastMaxValue = totalDuration
        val range: Int = (lastMaxValue - 0).toInt()
        val rangeVal = range.toString()
        dur = rangeVal.toFloat() * 1000
    }


    override fun onBackPressed() {
        if (isShowBackDialogue) {
            exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                override fun onGoToHomeOk() {
                    onBackPressedDispatcher.onBackPressed() //with this line
                    exitPopup.dismiss()
                    deleteFiles()
                    if (progressId != 0L) {
                        FFmpeg.cancel(progressId)
                        FFmpeg.cancel()
                    }
                }

                override fun onGoToHomeCancel() {
                    exitPopup.dismiss()

                }

            }, "back")
            exitPopup.apply {
                show(supportFragmentManager, "")
                isCancelable = true
            }
        } else {
            onBackPressedDispatcher.onBackPressed() //with this line
            deleteFiles()
            if (progressId != 0L) {
                FFmpeg.cancel(progressId)
                FFmpeg.cancel()
            }
        }

    }

    private fun deleteFiles() {
        if (file3 != null) file3!!.delete()
        if (file1 != null) file1!!.delete()
        if (file2 != null) file2!!.delete()
        if (compressedFile != null) compressedFile!!.delete()
        if (fileForMute != null) fileForMute!!.delete()
        if (extractAudioFile != null) extractAudioFile!!.delete()
        if (folder != null) folder!!.delete()
        if (fullVideoFile != null) fullVideoFile!!.delete()
        if (topBlurFinalOutputFile != null) topBlurFinalOutputFile!!.delete()
        if (bottomBlurOutputFile != null) bottomBlurOutputFile!!.delete()
        if (bottomBlurOutputFileTwo != null) bottomBlurOutputFileTwo!!.delete()
        if (bottomBlurFinalOutputFile != null) bottomBlurFinalOutputFile!!.delete()
        if (fullBlurOutputFile != null) fullBlurOutputFile!!.delete()


    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listeners() {
        binding.apply {
            blurSeekbar.apply {
                setOnSeekBarChangeListener(this@VideoEditActivity)
                progress = 50
                max = 100
            }
            homeToolbarId.apply {
                ivBack.apply {
                    applyBoomEffect()
                    setOnClickListener {
                        if (isShowBackDialogue) {
                            exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                                override fun onGoToHomeOk() {
                                    finish()
                                    exitPopup.dismiss()
                                    if (progressId != 0L) {
                                        FFmpeg.cancel(progressId)
                                        FFmpeg.cancel()
                                    }
                                    deleteFiles()
                                }

                                override fun onGoToHomeCancel() {
                                    exitPopup.dismiss()
                                }

                            }, "back")
                            exitPopup.apply {
                                show(supportFragmentManager, "")
                                isCancelable = true
                            }
                        } else {
                            finish()
                            if (progressId != 0L) {
                                FFmpeg.cancel(progressId)
                                FFmpeg.cancel()
                            }
                            deleteFiles()
                        }
                    }
                }
                ivShare.apply {
                    applyBoomEffect()
                    setOnClickListener {
                        if (isBlurred) {
                            if (topBlur) {
                                if (isLargeSizeVideo) {
                                    if (isMuted)
                                        topBlurCommand(
                                            finalMuteVideo,
                                            topBlurFinalOutput,
                                            seekbarRadius,
                                            "topBlur",
                                            "share"
                                        )
                                    else if (isDistortion)
                                        topBlurCommand(
                                            finalDistortedVideo,
                                            topBlurFinalOutput,
                                            seekbarRadius,
                                            "topBlur",
                                            "share"
                                        )
                                    else topBlurCommand(
                                        compressFile,
                                        topBlurFinalOutput,
                                        seekbarRadius,
                                        "topBlur",
                                        "share"
                                    )
                                } else {
                                    if (isMuted) topBlurCommand(
                                        mutedVideo,
                                        topBlurFinalOutput,
                                        seekbarRadius,
                                        "topBlur",
                                        "share"
                                    )
                                    else if (isDistortion) topBlurCommand(
                                        distortedVideo,
                                        topBlurFinalOutput,
                                        seekbarRadius,
                                        "topBlur",
                                        "share"
                                    )
                                    else topBlurCommand(
                                        originalPath!!,
                                        topBlurFinalOutput,
                                        seekbarRadius,
                                        "topBlur",
                                        "share"
                                    )
                                }
                            } else if (bottomBlur) {
                                if (isLargeSizeVideo) {
                                    if (isMuted) bottomBlurCommand(
                                        finalMuteVideo,
                                        bottomBlurFinalOutput,
                                        seekbarRadius,
                                        "bottomBlur",
                                        "share"
                                    )
                                    else if (isDistortion) bottomBlurCommand(
                                        finalDistortedVideo,
                                        bottomBlurFinalOutput,
                                        seekbarRadius,
                                        "bottomBlur",
                                        "share"
                                    )
                                    else bottomBlurCommand(
                                        compressFile,
                                        bottomBlurFinalOutput,
                                        seekbarRadius,
                                        "bottomBlur",
                                        "share"
                                    )
                                } else {
                                    if (isMuted) bottomBlurCommand(
                                        mutedVideo,
                                        bottomBlurFinalOutput,
                                        seekbarRadius,
                                        "bottomBlur",
                                        "share"
                                    )
                                    else if (isDistortion) bottomBlurCommand(
                                        distortedVideo,
                                        bottomBlurFinalOutput,
                                        seekbarRadius,
                                        "bottomBlur",
                                        "share"
                                    )
                                    else bottomBlurCommand(
                                        originalPath!!,
                                        bottomBlurFinalOutput,
                                        seekbarRadius,
                                        "bottomBlur",
                                        "share"
                                    )
                                }

                            } else if (fullBlur) {
                                if (isLargeSizeVideo) {
                                    if (isMuted) fullBlurCommand(
                                        finalMuteVideo,
                                        fullBlurOutput,
                                        seekbarRadius,
                                        "fullBlur",
                                        "share"
                                    )
                                    else if (isDistortion) fullBlurCommand(
                                        finalDistortedVideo,
                                        fullBlurOutput,
                                        seekbarRadius,
                                        "fullBlur",
                                        "share"
                                    )
                                    else fullBlurCommand(
                                        compressFile,
                                        fullBlurOutput,
                                        seekbarRadius,
                                        "fullBlur",
                                        "share"
                                    )
                                } else {
                                    if (isMuted) fullBlurCommand(
                                        mutedVideo,
                                        fullBlurOutput,
                                        seekbarRadius,
                                        "fullBlur",
                                        "share"
                                    )
                                    else if (isDistortion) fullBlurCommand(
                                        distortedVideo,
                                        fullBlurOutput,
                                        seekbarRadius,
                                        "fullBlur",
                                        "share"
                                    )
                                    else fullBlurCommand(
                                        originalPath!!,
                                        fullBlurOutput,
                                        seekbarRadius,
                                        "fullBlur",
                                        "share"
                                    )
                                }

                            }
                        } else {
                            if (isMuted) {
                                setShare(mutedVideo)
                            } else if (isDistortion) {
                                setShare(distortedVideo)
                            }
                        }
                    }

                }
                ivDownload.apply {
                    applyBoomEffect()
                    setOnClickListener {
                        saveVideo()
                    }
                }
            }
            buttonLayouts.apply {
                buttonRemoveAudio.apply {
                    setOnClickListener {
                        if (isVideoHaveAudioTrack(originalPath!!)) {
                            applyBoomEffect()
                            resetAudioButtonItems()

                            if (isLandScapeMode) {
                                audioDistortionForLandScape.viewGone()
                                audioRemoveForLandScape.viewVisible()
                            } else {
                                audioDistortion.viewGone()
                                audioRemove.viewVisible()
                            }

                            buttonLayouts.buttonRemoveAudio.background = ContextCompat.getDrawable(
                                this@VideoEditActivity, R.drawable.ss_corner_round_light_blue
                            )
                            buttonLayouts.ivRemoveAudio.apply {
                                setImageDrawable(
                                    ContextCompat.getDrawable(
                                        context, R.drawable.ic_remove
                                    )
                                )
                                setColorFilter(
                                    ContextCompat.getColor(
                                        context, R.color.white
                                    )
                                )
                            }
                            buttonLayouts.tvRemoveAudio.setTextColor(
                                ContextCompat.getColor(
                                    this@VideoEditActivity, R.color.white
                                )
                            )
                            muteVideoCommands()
                        } else {
                            exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                                override fun onGoToHomeOk() {
                                    exitPopup.dismiss()
                                }

                                override fun onGoToHomeCancel() {
                                    exitPopup.dismiss()
                                }
                            }, "noSound")
                            exitPopup.show(supportFragmentManager, "")
                            exitPopup.isCancelable = true

                        }
                    }
                }
                buttonRemoveDistortion.apply {
                    setOnClickListener {
                        if (isVideoHaveAudioTrack(originalPath!!)) {
                            applyBoomEffect()
                            if (distortedVideo != "") {
                                resetPlayerForMute(distortedVideo)
                                isShowBackDialogue = true
                                finalVideo = ""
                                isDistortion = true
                                isMuted = false

                                if (isLandScapeMode) {
                                    audioDistortionForLandScape.viewVisible()
                                    audioRemoveForLandScape.viewGone()
                                } else {
                                    audioDistortion.viewVisible()
                                    audioRemove.viewGone()
                                }

                                enableShareAndSave()
                                binding.apply {
                                    buttonNoEffect.apply {
                                        background = ContextCompat.getDrawable(
                                            this@VideoEditActivity,
                                            R.drawable.ss_corner_round_white_blue_corener
                                        )
                                        isEnabled = true
                                    }
                                    buttonLayouts.apply {
                                        buttonRemoveAudio.isEnabled = true
                                        buttonRemoveDistortion.isEnabled = false
                                    }
                                    ivNoEffect.apply {
                                        setImageDrawable(
                                            ContextCompat.getDrawable(
                                                this@VideoEditActivity, R.drawable.ic_no_effect
                                            )
                                        )
                                        setColorFilter(
                                            ContextCompat.getColor(
                                                context, R.color.white
                                            )
                                        )
                                    }

                                    tvNoEffect.setTextColor(
                                        ContextCompat.getColor(
                                            this@VideoEditActivity, R.color.white
                                        )
                                    )

                                    resetAudioButtonItems()
                                    buttonLayouts.buttonRemoveDistortion.background =
                                        ContextCompat.getDrawable(
                                            this@VideoEditActivity,
                                            R.drawable.ss_corner_round_light_blue
                                        )

                                    buttonLayouts.ivAddDistortion.apply {
                                        setImageDrawable(
                                            ContextCompat.getDrawable(
                                                context, R.drawable.ic_disortion
                                            )
                                        )
                                        setColorFilter(
                                            ContextCompat.getColor(
                                                context, R.color.white
                                            )
                                        )
                                    }

                                    buttonLayouts.tvAddDistortion.setTextColor(
                                        ContextCompat.getColor(
                                            this@VideoEditActivity, R.color.white
                                        )
                                    )


                                }
                            } else {
                                audioExtractCommand(fullOutPut)
                            }

                        } else {
                            exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                                override fun onGoToHomeOk() {
                                    exitPopup.dismiss()
                                }

                                override fun onGoToHomeCancel() {
                                    exitPopup.dismiss()
                                }
                            }, "noSound")
                            exitPopup.show(supportFragmentManager, "")
                            exitPopup.isCancelable = true
                        }
                    }
                }
            }

            buttonNoEffect.apply {
                setOnClickListener {
                    if (isVideoHaveAudioTrack(originalPath!!)) {
                        applyBoomEffect()
                        buttonNoEffect.isEnabled = false
                        if (isLandScapeMode) {
                            audioDistortionForLandScape.viewGone()
                            audioRemoveForLandScape.viewGone()
                        } else {
                            audioDistortion.viewGone()
                            audioRemove.viewGone()
                        }

                        resetAudioButtonItems()
                        buttonNoEffect.background = ContextCompat.getDrawable(
                            this@VideoEditActivity, R.drawable.ss_corner_round_white
                        )
                        ivNoEffect.apply {
                            setImageDrawable(
                                ContextCompat.getDrawable(
                                    this@VideoEditActivity, R.drawable.ic_no_effect_unselect
                                )
                            )
                            setColorFilter(
                                ContextCompat.getColor(
                                    context, R.color.grey_color
                                )
                            )
                        }
                        tvNoEffect.setTextColor(
                            ContextCompat.getColor(
                                this@VideoEditActivity, R.color.grey_color
                            )
                        )

                        if (isMuted) {
                            buttonLayouts.buttonRemoveAudio.isEnabled = true
                            resetPlayerForMute(originalPath!!)
                            isMuted = false
                            disableShareAndSave()

                        } else if (isDistortion) {
                            buttonLayouts.buttonRemoveDistortion.isEnabled = true
                            resetPlayerForMute(originalPath!!)
                            isDistortion = false
                            disableShareAndSave()
                        }
                    }
                }
            }
            cvNonBlur.setOnClickListener(this@VideoEditActivity)
            cvTopBlur.setOnClickListener(this@VideoEditActivity)
            cvBottomBlur.setOnClickListener(this@VideoEditActivity)
            cvFullBlur.setOnClickListener(this@VideoEditActivity)

            relParent.setOnClickListener {
                if (isLandScapeMode) {
                    if (!checkVisibility) {
                        checkVisibility = true
                        playPauseLayoutForLandScape.viewGone()
                        seekbarLayoutForLandScape.viewGone()
                    } else {
                        checkVisibility = false
                        playPauseLayoutForLandScape.viewVisible()
                        seekbarLayoutForLandScape.viewVisible()
                    }
                } else {
                    if (!checkVisibility) {
                        checkVisibility = true
                        playPauseLayout.viewGone()
                        seekbarLayout.viewGone()
                    } else {
                        checkVisibility = false
                        playPauseLayout.viewVisible()
                        seekbarLayout.viewVisible()
                    }
                }
            }

            videoPauseBtnLandScape.setOnClickListener {
                if (topBlur) {
                    mediaPlayerTop?.pause()
                } else if (bottomBlur) {
                    mediaPlayerBottom?.pause()
                } else {
                    mediaPlayerFull?.pause()
                }
                mVideoSurfaceViewNoneLandScape.pause()

                videoPauseBtnLandScape.viewGone()
                videoPlayBtnLandScape.viewVisible()
            }
            videoPlayBtnLandScape.setOnClickListener {
                if (topBlur) {
                    mediaPlayerTop?.start()
                } else if (bottomBlur) {
                    mediaPlayerBottom?.start()
                } else {
                    mediaPlayerFull?.start()
                }
                if (mVideoSurfaceViewNoneLandScape.isPlaying) {
                    mVideoSurfaceViewNoneLandScape.resume()
                } else {
                    mVideoSurfaceViewNoneLandScape.start()
                }
                setPlayerSeekBarForLandScape()
                videoPauseBtnLandScape.viewVisible()
                videoPlayBtnLandScape.viewGone()

            }

            exoPause.setOnClickListener {
                if (topBlur) {
                    mediaPlayerTop?.pause()
                } else if (bottomBlur) {
                    mediaPlayerBottom?.pause()
                } else {
                    mediaPlayerFull?.pause()
                }
                mVideoSurfaceViewNone.pause()

                exoPause.viewGone()
                exoPlay.viewVisible()
            }

            exoPlay.setOnClickListener {
                if (topBlur) {
                    mediaPlayerTop?.start()
                } else if (bottomBlur) {
                    mediaPlayerBottom?.start()
                } else {
                    mediaPlayerFull?.start()
                }
                if (mVideoSurfaceViewNone.isPlaying) {
                    mVideoSurfaceViewNone.resume()
                } else {
                    mVideoSurfaceViewNone.start()
                }
                setPlayerSeekBar()
                exoPause.viewVisible()
                exoPlay.viewGone()

            }
        }
    }

    //Custom Bottom navigation View
    private fun setBottomLayout() {
        binding.apply {
            ivCensor.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_censor_selected
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.white
                    )
                )
            }
            tvCenser.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            viewCensor.setBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            viewAudio.setBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
            censorLayout.setOnClickListener {
                censorTab()
                setCensorVisibility()
                if (isLandScapeMode) {
                    audioChangeLayoutForLanScape.viewGone()
                } else {
                    audioChangeLayout.viewGone()
                }


            }
            audioLayout.setOnClickListener {
                audioTab()
                setAudioVisibility()
                if (isLandScapeMode) {
                    audioChangeLayoutForLanScape.viewVisible()
                } else {
                    audioChangeLayout.viewVisible()
                }

            }
        }
    }

    private fun setCensorVisibility() {
        binding.run {
            blurLayouts.viewVisible()
            layoutAudioChangeButton.viewGone()
            buttonNoEffect.viewGone()
        }

        if (topBlur || bottomBlur || fullBlur) binding.blurLinearlayout.viewVisible()
        else binding.blurLinearlayout.viewGone()

    }

    private fun setAudioVisibility() {
        binding.run {
            blurLinearlayout.viewGone()
            blurLayouts.viewGone()
            layoutAudioChangeButton.viewVisible()
            buttonNoEffect.viewVisible()
        }
    }

    private fun censorTab() {
        resetItems()
        binding.apply {
            ivCensor.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_censor_selected
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.white
                    )
                )
            }
            tvCenser.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            viewCensor.setBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
        }
    }

    private fun audioTab() {
        resetItems()
        binding.apply {
            ivSpeaker.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_speaker_selected
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.white
                    )
                )
            }
            tvAudio.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            viewAudio.setBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
        }
    }

    private fun resetItems() {
        binding.apply {
            ivCensor.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_censor_unfilled
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.grey_color
                    )
                )
            }
            tvCenser.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
            viewCensor.setBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )

            ivSpeaker.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_speaker_unfilled
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.grey_color
                    )
                )
            }
            tvAudio.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )

            viewAudio.setBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
        }
    }

    private fun resetAudioButtonItems() {
        binding.buttonLayouts.apply {
            buttonRemoveAudio.background = ContextCompat.getDrawable(
                this@VideoEditActivity, R.drawable.ss_corner_round_dark_blue
            )
            buttonRemoveDistortion.background = ContextCompat.getDrawable(
                this@VideoEditActivity, R.drawable.ss_corner_round_dark_blue
            )

            ivRemoveAudio.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_remove_unfilled
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.grey_color
                    )
                )
            }

            tvRemoveAudio.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )

            ivAddDistortion.apply {
                setImageDrawable(
                    ContextCompat.getDrawable(
                        context, R.drawable.ic_disortion_unfilled
                    )
                )
                setColorFilter(
                    ContextCompat.getColor(
                        context, R.color.grey_color
                    )
                )
            }

            tvAddDistortion.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
        }
    }

    /* This function is set default view of filter layout */
    private fun defaultView() {
        binding.apply {
            cvNonBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.un_selected_button
                )
            )
            tvNoneBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
            ivNoneBlur.setColorFilter(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                ), PorterDuff.Mode.SRC_IN
            )
            cvTopBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.un_selected_button
                )
            )
            tvTopBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
            ivTopBlur.setColorFilter(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                ), PorterDuff.Mode.SRC_IN
            )
            cvBottomBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.un_selected_button
                )
            )
            tvBottomBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )

            ivBottomBlur.setColorFilter(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                ), PorterDuff.Mode.SRC_IN
            )
            cvFullBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.un_selected_button
                )
            )

            tvFullBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.grey_color
                )
            )
            ivFullBlur.setImageResource(R.drawable.ic_full_blur)
        }

    }

    private fun setNonBlur() {
        binding.apply {
            cvNonBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.selected_btn_color
                )
            )
            tvNoneBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            ivNoneBlur.setColorFilter(
                ContextCompat.getColor(this@VideoEditActivity, R.color.white),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun setTopBlur() {
        binding.apply {
            cvTopBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.selected_btn_color
                )
            )
            tvTopBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            ivTopBlur.setColorFilter(
                ContextCompat.getColor(this@VideoEditActivity, R.color.white),
                PorterDuff.Mode.SRC_IN
            )
        }

    }

    private fun setBottomBlur() {
        binding.apply {
            cvBottomBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.selected_btn_color
                )
            )
            tvBottomBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            ivBottomBlur.setColorFilter(
                ContextCompat.getColor(this@VideoEditActivity, R.color.white),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun setFullBlur() {
        binding.apply {
            cvFullBlur.setCardBackgroundColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.selected_btn_color
                )
            )
            tvFullBlur.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            ivFullBlur.setImageResource(R.drawable.ic_full_blur_white)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isLandScapeMode) {
            mHandler?.removeCallbacks(updateProgressActionForLandScape)
        } else {
            mHandler?.removeCallbacks(updateProgressAction)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressId != 0L) {
            FFmpeg.cancel(progressId)
            FFmpeg.cancel()
        }
        if (isLandScapeMode) {
            mHandler?.removeCallbacks(updateProgressActionForLandScape)
        } else {
            mHandler?.removeCallbacks(updateProgressAction)
        }

        topBlur = false
        nonBlur = false
        bottomBlur = false
        fullBlur = false
        deleteFiles()
        AppStorage.apply {
            clearSessionTopBlur()
            clearSessionBottomBlur()
            clearSessionFullBlur()
        }
    }

    /* That function is creating files and folders */
    private fun createFilesAndDirectories() {
        file1 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        file2 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        compressedFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        file3 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        fullVideoFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        topBlurFinalOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        bottomBlurOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        bottomBlurOutputFileTwo =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        bottomBlurFinalOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        fullBlurOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        if (!file1!!.exists()) {
            file1!!.mkdirs()
        }

        if (!file2!!.exists()) {
            file2!!.mkdirs()
        }
        if (!compressedFile!!.exists()) {
            compressedFile!!.mkdirs()
        }
        if (!file3!!.exists()) {
            file3!!.mkdirs()
        }

        if (!fullVideoFile!!.exists()) {
            fullVideoFile!!.mkdirs()
        }

        if (!topBlurFinalOutputFile!!.exists()) {
            topBlurFinalOutputFile!!.mkdirs()
        }

        if (!bottomBlurOutputFile!!.exists()) {
            bottomBlurOutputFile!!.mkdirs()
        }

        if (!bottomBlurOutputFileTwo!!.exists()) {
            bottomBlurOutputFileTwo!!.mkdirs()
        }
        if (!bottomBlurFinalOutputFile!!.exists()) {
            bottomBlurFinalOutputFile!!.mkdirs()
        }

        if (!fullBlurOutputFile!!.exists()) {
            fullBlurOutputFile!!.mkdirs()
        }

        downloadFile = File(file1, generateBlurName() + ".mp4")
        downloadFile2 = File(file2, "VIDEO_" + System.currentTimeMillis() + ".mp4")
        compresseddFile =
            File(compressedFile, "COMPRESSED" + System.currentTimeMillis() + ".mp4")
        downloadFile3 = File(file3, "VIDEO2_" + System.currentTimeMillis() + ".mp4")

        fullVideoForCropFile =
            File(fullVideoFile, Full_VIDEO_PATH + System.currentTimeMillis() + ".mp4")
        topBlurFinalDownload =
            File(topBlurFinalOutputFile, CENSORX_TOP_BLUR + System.currentTimeMillis() + ".mp4")

        bottomBlurDownload =
            File(bottomBlurOutputFile, CROP_BOTTOM_BLUR + System.currentTimeMillis() + ".mp4")

        bottomBlurDownloadTwo = File(
            bottomBlurOutputFileTwo, CROP_BOTTOM_BLUR_TWO + System.currentTimeMillis() + ".mp4"
        )

        bottomBlurFinalDownload = File(
            bottomBlurFinalOutputFile, CENSORX_BOTTOM_BLUR + System.currentTimeMillis() + ".mp4"
        )
        fullBlurDownload =
            File(fullBlurOutputFile, CENSORX_FULL_BLUR + System.currentTimeMillis() + ".mp4")

        fullOutPut = fullVideoForCropFile!!.absolutePath
        topBlurFinalOutput = topBlurFinalDownload!!.absolutePath
        bottomBlurFinalOutput = bottomBlurFinalDownload!!.absolutePath
        bottomBlurOutput = bottomBlurDownload!!.absolutePath
        bottomBlurOutputTwo = bottomBlurDownloadTwo!!.absolutePath
        fullBlurOutput = fullBlurDownload!!.absolutePath
        outPutForMutedBlurredVideo = downloadFile!!.absolutePath
        secondOutputForBlur = downloadFile3!!.absolutePath
        lessResolatedFile = downloadFile2!!.absolutePath
        compressFile = compresseddFile!!.absolutePath

    }

    override fun onPause() {
        super.onPause()
        if (isLandScapeMode) {
            binding.mVideoSurfaceViewNoneLandScape.pause()
            mHandler?.removeCallbacks(updateProgressActionForLandScape)
            videoPosition = binding.mVideoSurfaceViewNoneLandScape.currentPosition
        } else {
            binding.mVideoSurfaceViewNone.pause()
            mHandler?.removeCallbacks(updateProgressAction)
            videoPosition = binding.mVideoSurfaceViewNone.currentPosition
        }


    }

    override fun onResume() {
        super.onResume()
        if (isCompression) {
            if (isLandScapeMode) {
                binding.relLayoutLandScape.viewGone()
            } else {
                binding.relLayout.viewGone()
            }

        } else {
            if (isLandScapeMode) {
                mHandler = Handler(Looper.getMainLooper())
                mHandler?.post(updateProgressActionForLandScape)
                updateProgressForLandScape()
                binding.videoPlayBtnLandScape.viewGone()
                binding.videoPauseBtnLandScape.viewVisible()
                if (isBlurred) {
                    if (topBlur) {
                        if (videoPosition != 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaPlayerTop?.seekTo(videoPosition)
                                mediaPlayerTop?.start()
                            }, 200)
                            binding.mVideoSurfaceViewNoneLandScape.seekTo(videoPosition)
                            binding.mVideoSurfaceViewNoneLandScape.start()
                        } else {
                            mediaPlayerTop?.seekTo(videoPosition)
                            mediaPlayerTop?.start()
                            binding.mVideoSurfaceViewNoneLandScape.start()
                        }

                    } else if (bottomBlur) {
                        if (videoPosition != 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaPlayerBottom?.seekTo(videoPosition)
                                mediaPlayerBottom?.start()
                            }, 200)
                            binding.mVideoSurfaceViewNoneLandScape.seekTo(videoPosition)
                            binding.mVideoSurfaceViewNoneLandScape.start()
                        } else {
                            mediaPlayerBottom?.seekTo(0)
                            mediaPlayerBottom?.start()
                            binding.mVideoSurfaceViewNoneLandScape.start()
                        }

                    } else if (fullBlur) {
                        if (videoPosition != 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaPlayerFull?.seekTo(videoPosition)
                                mediaPlayerFull?.start()
                            }, 200)
                            binding.mVideoSurfaceViewNoneLandScape.seekTo(videoPosition)
                            binding.mVideoSurfaceViewNoneLandScape.start()
                        } else {
                            mediaPlayerFull?.seekTo(0)
                            mediaPlayerFull?.start()
                            binding.mVideoSurfaceViewNoneLandScape.start()
                        }
                    }
                } else {
                    if (videoPosition != 0) {
                        binding.mVideoSurfaceViewNoneLandScape.seekTo(videoPosition)
                        binding.mVideoSurfaceViewNoneLandScape.start()
                    } else {
                        binding.mVideoSurfaceViewNoneLandScape.start()
                    }
                }
            } else {
                mHandler = Handler(Looper.getMainLooper())
                mHandler?.post(updateProgressAction)
                updateProgress()
                binding.exoPlay.viewGone()
                binding.exoPause.viewVisible()
                if (isBlurred) {
                    if (topBlur) {
                        if (videoPosition != 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaPlayerTop?.seekTo(videoPosition)
                                mediaPlayerTop?.start()
                            }, 200)
                            binding.mVideoSurfaceViewNone.seekTo(videoPosition)
                            binding.mVideoSurfaceViewNone.start()

                        } else {
                            mediaPlayerTop?.seekTo(videoPosition)
                            mediaPlayerTop?.start()
                            binding.mVideoSurfaceViewNone.start()
                        }
                    } else if (bottomBlur) {
                        if (videoPosition != 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaPlayerBottom?.seekTo(videoPosition)
                                mediaPlayerBottom?.start()
                            }, 200)
                            binding.mVideoSurfaceViewNone.seekTo(videoPosition)
                            binding.mVideoSurfaceViewNone.start()
                        } else {
                            mediaPlayerBottom?.seekTo(0)
                            mediaPlayerBottom?.start()
                            binding.mVideoSurfaceViewNone.start()
                        }

                    } else if (fullBlur) {
                        if (videoPosition != 0) {
                            val handler = Handler(Looper.getMainLooper())
                            handler.postDelayed({
                                mediaPlayerFull?.seekTo(videoPosition)
                                mediaPlayerFull?.start()
                            }, 200)
                            binding.mVideoSurfaceViewNone.seekTo(videoPosition)
                            binding.mVideoSurfaceViewNone.start()

                        } else {
                            mediaPlayerFull?.seekTo(0)
                            mediaPlayerFull?.start()
                            binding.mVideoSurfaceViewNone.start()
                        }

                    }
                } else {
                    if (videoPosition != 0) {
                        binding.mVideoSurfaceViewNone.seekTo(videoPosition)
                        binding.mVideoSurfaceViewNone.start()
                    } else {
                        binding.mVideoSurfaceViewNone.start()
                    }
                }
            }
        }
    }

    override fun onCompletion(p0: MediaPlayer?) {
        if (isLandScapeMode) {
            binding.videoPauseBtnLandScape.viewGone()
            binding.videoPlayBtnLandScape.viewVisible()
            totalTimeForLandScape.viewVisible()
            durationLandScape.viewGone()
            totalTimeForLandScape.text = Constants.formatSeconds(totalDuration)
        } else {
            binding.exoPause.viewGone()
            binding.exoPlay.viewVisible()
            totalTime.viewVisible()
            duration.viewGone()
            totalTime.text = Constants.formatSeconds(totalDuration)
        }

    }

    private fun enableShareAndSave() {
        binding.homeToolbarId.apply {
            ivDownload.apply {
                isEnabled = true
                alpha = 1f
            }
            ivShare.apply {
                isEnabled = true
                alpha = 1f
            }
        }
    }

    private fun disableShareAndSave() {
        binding.homeToolbarId.apply {
            ivDownload.isEnabled = false
            ivDownload.alpha = 0.5f
            ivShare.isEnabled = false
            ivShare.alpha = 0.5f
        }
    }

    private fun checkOrientationOfVideo() {
        val mp = MediaPlayer()
        try {
            mp.setDataSource(originalPath)
            mp.prepare()
            mp.setOnVideoSizeChangedListener { _, width, height ->
                if (width < height) {
                    isLandScapeMode = false
                } else {
                    isLandScapeMode = true
                }
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun pxToDp(px: Int): Int {
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        return (px / (displayMetrics.xdpi / DisplayMetrics.DENSITY_DEFAULT)).roundToInt().toInt()
    }


}