package com.censorchi.views.activities

import android.app.Dialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.AndroidRuntimeException
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.abedelazizshe.lightcompressorlibrary.CompressionListener
import com.abedelazizshe.lightcompressorlibrary.VideoCompressor
import com.abedelazizshe.lightcompressorlibrary.VideoQuality
import com.abedelazizshe.lightcompressorlibrary.config.AppSpecificStorageConfiguration
import com.abedelazizshe.lightcompressorlibrary.config.Configuration
import com.abedelazizshe.lightcompressorlibrary.config.SaveLocation
import com.abedelazizshe.lightcompressorlibrary.config.SharedStorageConfiguration
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.arthenica.mobileffmpeg.Statistics
import com.censorchi.R
import com.censorchi.databinding.ActivityEditVideoBinding
import com.censorchi.utils.*
import com.censorchi.utils.sharedPreference.AppStorage.clearSessionTopBlur
import com.censorchi.utils.sharedPreference.AppStorage.clearSessionBottomBlur
import com.censorchi.utils.sharedPreference.AppStorage.clearSessionFullBlur
import com.censorchi.utils.sharedPreference.AppStorage.getBottomBlur
import com.censorchi.utils.sharedPreference.AppStorage.getFullBlur
import com.censorchi.utils.sharedPreference.AppStorage.getTopBlur
import com.censorchi.utils.sharedPreference.AppStorage.setBottomBlurPref
import com.censorchi.utils.sharedPreference.AppStorage.setFullBlurPref
import com.censorchi.utils.sharedPreference.AppStorage.setTopBlur
import com.censorchi.utils.Constants.CENSORX_BOTTOM_BLUR
import com.censorchi.utils.Constants.CENSORX_FULL_BLUR
import com.censorchi.utils.Constants.CENSORX_TOP_BLUR
import com.censorchi.utils.Constants.MAX_BUFFER_DURATION
import com.censorchi.utils.Constants.MIN_BUFFER_DURATION
import com.censorchi.utils.Constants.MIN_PLAYBACK_RESUME_BUFFER
import com.censorchi.utils.Constants.MIN_PLAYBACK_START_BUFFER
import com.censorchi.utils.Constants.VIDEO_PATH
import com.censorchi.views.popUp.DialogueForShowVideoProgress
import com.censorchi.views.popUp.ExitDialogue
import com.censorchi.views.popUp.MaterialDialogHelper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.upstream.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.surina.soundtouch.SoundTouch
import java.io.*
import java.lang.System.err
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit

class VideoEditActivity : BaseActivity(), Player.Listener, SeekBar.OnSeekBarChangeListener,
    View.OnClickListener {
    /*Binding*/
    private lateinit var binding: ActivityEditVideoBinding

    /*Views*/
    private lateinit var fileUri: Uri
    private lateinit var audioChangeLayout: RelativeLayout
    private lateinit var audioDistortion: RelativeLayout
    private lateinit var audioRemove: RelativeLayout
    private lateinit var duration: TextView
    private lateinit var totalTime: TextView
    private lateinit var popUpForCompressedVideo: DialogueForShowVideoProgress
    private var exoPlayer: ExoPlayer? = null
    lateinit var exitPopup: ExitDialogue

    /* Files */
    private var file1: File? = null
    private var file2: File? = null
    private var file3: File? = null
    private var folder: File? = null
    private var fileForMute: File? = null
    private var extractAudioFile: File? = null
    private var topBlurOutputFile: File? = null
    private var bottomBlurOutputFile: File? = null
    private var fullBlurOutputFile: File? = null

    /*Strings*/
    private var video: String? = null
    private lateinit var outputForBlurredVideo: String
    private lateinit var outPutForMutedBlurredVideo: String
    private lateinit var secondOutputForBlur: String
    private lateinit var newVideo: String
    private lateinit var lessResolatedFile: String
    private lateinit var finalOutput: String
    private lateinit var topBlurOutput: String
    private lateinit var bottomBlurOutput: String
    private lateinit var fullBlurOutput: String
    private var finalVideo: String = ""
    private var mutedVideo: String = ""
    private var blurAndMuteVideo: String = ""
    private var originalPath: String? = null
    private var finalDistortedVideo: String = ""

    /*Integers*/
    private var videoWidth: Int = 0
    private var videoHeight: Int = 0
    private var mradius: Int = 0
    var topSeekBar = 0
    var bottomSeekBar = 0
    var fullSeekBar = 0

    /*Longs*/
    private var totalDuration: Long = 0
    private var lastMaxValue: Long = 0
    private var progressId: Long = 0
    private var playbackPosition = 0L

    /*Floats*/
    private var dur: Float = 0F
    private val uris = mutableListOf<Uri>()
    private var statistics: Statistics? = null
    private var mHandler: Handler? = null

    /*Booleans*/
    private var playWhenReady = true
    private var isBlurred = false
    private var isDistortion = false
    private var isCompression = false
    private var isMuted = false
    private var isNoEffect = false
    private var blurAndMuted = false
    var mTopBlur = false
    var mBottomBlur = false
    var mFullBlur = false
    var isBlurAndMuted = false

    companion object {
        var topBlur: Boolean = false
        var nonBlur: Boolean = false
        var bottomBlur: Boolean = false
        var fullBlur: Boolean = false
    }

    private val updateProgressAction = Runnable {
        updateProgress()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.homeToolbarId.toolbarNameTv.text = getString(R.string.censor_video)
        popUpForCompressedVideo = DialogueForShowVideoProgress()
        createFilesAndDirectories()

        video = intent.getStringExtra(VIDEO_PATH)
        fileUri = Uri.parse(video)
        originalPath = this.getRealPathFromUri(applicationContext, Uri.parse(video))
        uris.add(fileUri)
        getHeight(originalPath!!)
        executeResolutionDown()

//        getHeight(originalPath!!)
//        if (videoWidth > 600) {
//            isCompression = true
//            executeResolutionDownn()
//        }
        disableShareAndSave()

        audioChangeLayout = binding.playerView.findViewById(R.id.audioLayout)
        audioDistortion = binding.playerView.findViewById(R.id.cv_distortion)
        audioRemove = binding.playerView.findViewById(R.id.cv_audio_removed)
        duration = binding.playerView.findViewById(R.id.duration)
        totalTime = binding.playerView.findViewById(R.id.endTime)

        if (finalVideo.isNotEmpty()) {
            preparePlayer(Uri.parse(finalVideo))
            var videoWatchedTime: Long = 0
            Handler().postDelayed({ //Do your work
                videoWatchedTime = exoPlayer!!.currentPosition / 1000
            }, 1000)
            duration.text = videoWatchedTime.toString()
        } else {
            if (isCompression) {
                binding.playerView.viewGone()
            } else {
                binding.playerView.viewVisible()
                preparePlayer(Uri.parse(originalPath))
                mHandler = Handler()
                mHandler?.post(updateProgressAction)
                updateProgress()
                exoPlayer?.addListener(videoPlayerListener)
            }
        }
        listeners()
        setBottomLayout()
        setNonBlur()
        getTotalDuration(originalPath!!)

    }

    /* That function is creating files and folders */
    private fun createFilesAndDirectories() {
        file1 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        file2 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        file3 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        topBlurOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        bottomBlurOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        fullBlurOutputFile =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())

        if (!file1!!.exists()) {
            file1!!.mkdirs()
        }

        if (!file2!!.exists()) {
            file2!!.mkdirs()
        }

        if (!file3!!.exists()) {
            file3!!.mkdirs()
        }

        if (!topBlurOutputFile!!.exists()) {
            topBlurOutputFile!!.mkdirs()
        }

        if (!bottomBlurOutputFile!!.exists()) {
            bottomBlurOutputFile!!.mkdirs()
        }

        if (!fullBlurOutputFile!!.exists()) {
            fullBlurOutputFile!!.mkdirs()
        }

        val downloadFile = File(file1, generateBlurName() + ".mp4")
        val downloadFile2 = File(file2, "VIDEO_" + System.currentTimeMillis() + ".mp4")
        val downloadFile3 = File(file3, "VIDEO2_" + System.currentTimeMillis() + ".mp4")

        val topBlurDownload =
            File(topBlurOutputFile, CENSORX_TOP_BLUR + System.currentTimeMillis() + ".mp4")
        val bottomBlurDownload =
            File(bottomBlurOutputFile, CENSORX_BOTTOM_BLUR + System.currentTimeMillis() + ".mp4")
        val fullBlurDownload =
            File(fullBlurOutputFile, CENSORX_FULL_BLUR + System.currentTimeMillis() + ".mp4")

        topBlurOutput = topBlurDownload.absolutePath
        bottomBlurOutput = bottomBlurDownload.absolutePath
        fullBlurOutput = fullBlurDownload.absolutePath
        outPutForMutedBlurredVideo = downloadFile.absolutePath
        secondOutputForBlur = downloadFile3.absolutePath
        lessResolatedFile = downloadFile2.absolutePath
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.release()
        mHandler?.removeCallbacks(updateProgressAction)
    }

    override fun onResume() {
        super.onResume()
        if (finalVideo.isNotEmpty()) {
            preparePlayer(Uri.parse(finalVideo))
            mHandler = Handler()
            mHandler?.post(updateProgressAction)
            updateProgress()
            exoPlayer?.addListener(videoPlayerListener)
        } else {
            if (isCompression) {
                binding.playerView.viewGone()
            } else {
                preparePlayer(Uri.parse(originalPath))
                mHandler = Handler()
                mHandler?.post(updateProgressAction)
                updateProgress()
                exoPlayer?.addListener(videoPlayerListener)
            }
        }

    }

    /* This function is call for scale down the resolution of videos */
    private fun executeResolutionDown() {
//        val cmd = arrayOf(
//            "-i",
//            getNewPath(originalPath!!),
//            "-vf",
//            "scale=480:852",
//            "-b:v",
//            "8000k",
//            lessResolatedFile
//        )
        val cmd = arrayOf(
            "-i",
            getNewPath(originalPath!!),
            "-vf",
            "scale=480:852",
            "-map",
            "0",
            "-c:a",
            "copy",
            "-c:s",
            "copy",
            lessResolatedFile
        )
        execFfmpegForResolutionDown(cmd)
    }


    private fun executeResolutionDownn() {
        getHeight(originalPath!!)
        if (videoWidth > 500) {
            GlobalScope.launch {
                VideoCompressor.start(
                    context = applicationContext,
                    uris,
                    isStreamable = false,
                    sharedStorageConfiguration = SharedStorageConfiguration(
                        saveAt = SaveLocation.movies, videoName = "compressed_video"
                    ),
                    // OR AND NOT BOTH
                    appSpecificStorageConfiguration = AppSpecificStorageConfiguration(
                        videoName = "compressed_video", // => required name
                        subFolderName = "my-videos" // => optional and ONLY if exists
                    ),
                    configureWith = Configuration(
                        quality = VideoQuality.MEDIUM,
                        isMinBitrateCheckEnabled = true,
                        videoBitrateInMbps = 5, /*Int, ignore, or null*/
                        videoWidth = 852.0,
                        videoHeight = 480.0,
                    ),

                    listener = object : CompressionListener {
                        override fun onProgress(index: Int, percent: Float) {
                            //Update UI
                            if (percent <= 100 && percent.toInt() % 5 == 0) runOnUiThread {
                                popUpForCompressedVideo.showProgress(percent)
                                popUpForCompressedVideo.isCancelable = false
                            }
                        }

                        override fun onStart(index: Int) {
//                        popUpForCompressedVideo = DialogueForShowVideoProgress()
                            popUpForCompressedVideo.show(supportFragmentManager, "")
                        }

                        override fun onSuccess(index: Int, size: Long, path: String?) {
                            showToast("Success1")
                            getHeight(path!!)
                            originalPath = path
                            preparePlayer(Uri.parse(path))
                            popUpForCompressedVideo.dismiss()
                        }

                        override fun onFailure(index: Int, failureMessage: String) {
                            Log.wtf("failureMessage", failureMessage)
                        }

                        override fun onCancelled(index: Int) {
                            Log.wtf("TAG", "compression has been cancelled")
                            // make UI changes, cleanup, etc
                        }
                    },
                )
            }
        }
    }


    /* This function is call for update the seekbar and show time */
    private fun updateProgress() {
        if (exoPlayer != null) {
            val timeLeft: Long = exoPlayer!!.duration - exoPlayer!!.contentPosition
            val delayMs: Long = TimeUnit.SECONDS.toMillis(1)
            mHandler?.postDelayed(updateProgressAction, delayMs)

            if (calculateTimeLeft(timeLeft) == "-12:-55") {
                duration.text = Constants.formatSeconds(totalDuration)
            } else {
                "-${this.calculateTimeLeft(timeLeft)}".also { duration.text = it }
            }
        }
    }

    /* This function is call for calculate remaining time */
    private fun calculateTimeLeft(timeLeft: Long): String {
        return String.format(
            "%d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(timeLeft) % TimeUnit.HOURS.toMinutes(1),
            TimeUnit.MILLISECONDS.toSeconds(timeLeft) % TimeUnit.MINUTES.toSeconds(1)
        )
    }

    /* This function is initialize and set exoplayer */
    private fun preparePlayer(uri: Uri) {
        val loadControl: LoadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, 16))
            .setBufferDurationsMs(
                MIN_BUFFER_DURATION,
                MAX_BUFFER_DURATION,
                MIN_PLAYBACK_START_BUFFER,
                MIN_PLAYBACK_RESUME_BUFFER
            )
            .setTargetBufferBytes(-1)
            .setPrioritizeTimeOverSizeThresholds(true).build()

        val trackSelector: TrackSelector = DefaultTrackSelector(this@VideoEditActivity)

        exoPlayer = ExoPlayer.Builder(this@VideoEditActivity)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build()
        binding.playerView.player = exoPlayer
        exoPlayer?.playWhenReady = true
        val mediaItem = MediaItem.fromUri(uri)
        exoPlayer?.setMediaItem(mediaItem)
        exoPlayer?.prepare()
        exoPlayer?.play()
    }

    /* This function is releasing the player */
    private fun releasePlayer() {
        exoPlayer?.let { player ->
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.release()
            exoPlayer = null
        }
    }

    private val videoPlayerListener = object : Player.Listener {
        @Deprecated("Deprecated in Java")
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> {
                    totalTime.viewVisible()
                    duration.viewGone()
                    totalTime.text = Constants.formatSeconds(totalDuration)
                }
                Player.STATE_READY -> {
                    totalTime.viewGone()
                    duration.viewVisible()
                }
            }
        }

    }

    override fun onStop() {
        super.onStop()
        releasePlayer()
        mHandler?.removeCallbacks(updateProgressAction)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (progressId != 0L) {
            FFmpeg.cancel(progressId)
            FFmpeg.cancel()
        }
        mHandler?.removeCallbacks(updateProgressAction)
        releasePlayer()
        topBlur = false
        nonBlur = false
        bottomBlur = false
        fullBlur = false
        deleteFiles()
        clearSessionsOfFullTopAndBottomBlur()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listeners() {
        binding.blurSeekbar.setOnSeekBarChangeListener(this)
        binding.blurSeekbar.progress = 50
        binding.blurSeekbar.max = 100

        binding.homeToolbarId.ivBack.applyBoomEffect(true)
        binding.homeToolbarId.ivBack.setOnClickListener {
            exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                override fun onGoToHomeOk() {
                    finish()
                    exitPopup.dismiss()
                    if (progressId != 0L) {
                        FFmpeg.cancel(progressId)
                        FFmpeg.cancel()
                    }
                    deleteFiles()
                    clearSessionsOfFullTopAndBottomBlur()
                }

                override fun onGoToHomeCancel() {
                    exitPopup.dismiss()
                }

            }, "back")
            exitPopup.show(supportFragmentManager, "")
            exitPopup.isCancelable = true
        }

        binding.homeToolbarId.ivShare.applyBoomEffect(true)
        binding.homeToolbarId.ivShare.setOnClickListener {
            val uri = FileProvider.getUriForFile(
                this, "$packageName.provider", File(finalVideo)
            )

            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "video/*"
            sharingIntent.putExtra(
                Intent.EXTRA_TEXT,
                "Create By : Censor X \n\n https://play.google.com/store/apps/details?id=$packageName"
            )

            sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
            startActivity(Intent.createChooser(sharingIntent, "Share Video"))
        }

        binding.homeToolbarId.ivDownload.applyBoomEffect(true)
        binding.homeToolbarId.ivDownload.setOnClickListener {
            if (finalVideo.isNotEmpty()) saveVideo(finalVideo)
        }

        binding.buttonLayouts.buttonRemoveAudio.applyBoomEffect(true)
        binding.buttonLayouts.buttonRemoveAudio.setOnClickListener {
            resetAudioButtonItems()
            audioDistortion.viewGone()
            audioRemove.viewVisible()
            binding.buttonLayouts.buttonRemoveAudio.background = ContextCompat.getDrawable(
                this@VideoEditActivity, R.drawable.ss_corner_round_light_blue
            )

            binding.buttonLayouts.ivRemoveAudio.apply {
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
            binding.buttonLayouts.tvRemoveAudio.setTextColor(
                ContextCompat.getColor(
                    this, R.color.white
                )
            )
            muteVideoCommands()
        }

        binding.buttonLayouts.buttonRemoveDistortion.applyBoomEffect(true)
        binding.buttonLayouts.buttonRemoveDistortion.setOnClickListener {
            resetAudioButtonItems()
            binding.buttonLayouts.buttonRemoveDistortion.background = ContextCompat.getDrawable(
                this@VideoEditActivity, R.drawable.ss_corner_round_light_blue
            )

            binding.buttonLayouts.ivAddDistortion.apply {
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

            binding.buttonLayouts.tvAddDistortion.setTextColor(
                ContextCompat.getColor(
                    this@VideoEditActivity, R.color.white
                )
            )
            audioExtractCommand()
        }

        //todo
        binding.buttonNoEffect.applyBoomEffect(true)
        binding.buttonNoEffect.setOnClickListener {
            binding.buttonNoEffect.isEnabled = false
            isNoEffect = true
            audioDistortion.viewGone()
            audioRemove.viewGone()
            releasePlayer()
            resetAudioButtonItems()
            binding.buttonNoEffect.background = ContextCompat.getDrawable(
                this@VideoEditActivity, R.drawable.ss_corner_round_white
            )
            if (isBlurAndMuted) {
                setExoPlayer(secondOutputForBlur)
                isMuted = false
                isDistortion = false
                binding.buttonLayouts.buttonRemoveDistortion.isEnabled = true
            } else if (isBlurred && isMuted) {
                setExoPlayer(outputForBlurredVideo)
                isMuted = false
            } else if (isBlurred && isDistortion) {
                setExoPlayer(outputForBlurredVideo)
                isDistortion = false
            } else if (isMuted) {
                setExoPlayer(originalPath!!)
                isMuted = false
                binding.buttonLayouts.buttonRemoveAudio.isEnabled = true
            } else if (isDistortion) {
                setExoPlayer(originalPath!!)
                isDistortion = false
                binding.buttonLayouts.buttonRemoveDistortion.isEnabled = true
            }

            clearSessionsOfFullTopAndBottomBlur()
        }

        binding.cvNonBlur.setOnClickListener(this)
        binding.cvTopBlur.setOnClickListener(this)
        binding.cvBottomBlur.setOnClickListener(this)
        binding.cvFullBlur.setOnClickListener(this)
    }

    private fun setExoPlayer(url: String) {
        preparePlayer(Uri.parse(url))
        mHandler = Handler()
        mHandler?.post(updateProgressAction)
        updateProgress()
        exoPlayer?.addListener(videoPlayerListener)
        finalVideo = url
    }

    //Custom Bottom navigation View
    private fun setBottomLayout() {
        binding.ivCensor.apply {
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
        binding.tvCenser.setTextColor(
            ContextCompat.getColor(
                this, R.color.white
            )
        )

        binding.viewCensor.setBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )

        binding.viewAudio.setBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.grey_color
            )
        )
        binding.censorLayout.setOnClickListener {
            censorTab()
            setCensorVisibility()
            audioChangeLayout.viewGone()

        }
        binding.audioLayout.setOnClickListener {
            audioTab()
            setAudioVisibility()
            audioChangeLayout.viewVisible()
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
        binding.ivCensor.apply {
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
        binding.tvCenser.setTextColor(
            ContextCompat.getColor(
                this, R.color.white
            )
        )
        binding.viewCensor.setBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )

    }

    private fun audioTab() {
        resetItems()
        binding.ivSpeaker.apply {
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
        binding.tvAudio.setTextColor(
            ContextCompat.getColor(
                this, R.color.white
            )
        )

        binding.viewAudio.setBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )

    }

    private fun resetItems() {
        binding.ivCensor.apply {
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
        binding.tvCenser.setTextColor(
            ContextCompat.getColor(
                this, R.color.grey_color
            )
        )

        binding.viewCensor.setBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.grey_color
            )
        )

        binding.ivSpeaker.apply {
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
        binding.tvAudio.setTextColor(
            ContextCompat.getColor(
                this, R.color.grey_color
            )
        )

        binding.viewAudio.setBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.grey_color
            )
        )

    }

    private fun resetAudioButtonItems() {
        binding.buttonLayouts.buttonRemoveAudio.background = ContextCompat.getDrawable(
            this@VideoEditActivity, R.drawable.ss_corner_round_dark_blue
        )
        binding.buttonLayouts.buttonRemoveDistortion.background = ContextCompat.getDrawable(
            this@VideoEditActivity, R.drawable.ss_corner_round_dark_blue
        )
        binding.buttonLayouts.ivRemoveAudio.apply {
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
        binding.buttonLayouts.tvRemoveAudio.setTextColor(
            ContextCompat.getColor(
                this, R.color.grey_color
            )
        )

        binding.buttonLayouts.ivAddDistortion.apply {
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
        binding.buttonLayouts.tvAddDistortion.setTextColor(
            ContextCompat.getColor(
                this, R.color.grey_color
            )
        )
    }

    /* These are the clicks of blur filters */
    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.cv_non_blur -> {
                nonBlur = true
                binding.cvTopBlur.isEnabled = true
                binding.cvBottomBlur.isEnabled = true
                binding.cvFullBlur.isEnabled = true
                isBlurred = false
                isBlurAndMuted = false
                blurAndMuted = false
                finalVideo = ""
                binding.blurLinearlayout.viewGone()
                defaultView()
                setNonBlur()
                releasePlayer()
                if (isBlurred && isMuted) {
                    finalVideo = mutedVideo
                    preparePlayer(Uri.parse(mutedVideo))
                } else if (isBlurred && isDistortion) {
                    setExoPlayer(finalDistortedVideo)
                } else if (topBlur || bottomBlur || fullBlur) {
                    setExoPlayer(fileUri.toString())
                }
            }

            R.id.cv_top_blur -> {
                defaultView()
                setTopBlur()
                blurVideoCommands(0, binding.blurSeekbar.progress)
            }
            R.id.cv_bottom_blur -> {
                defaultView()
                setBottomBlur()
                blurVideoCommands(1, binding.blurSeekbar.progress)
            }
            R.id.cv_full_blur -> {
                defaultView()
                setFullBlur()
                blurVideoCommands(2, binding.blurSeekbar.progress)
            }
        }
    }

    /* This function is set default view of filter layout */
    private fun defaultView() {
        binding.cvNonBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this, R.color.un_selected_button
            )
        )
        binding.tvNoneBlur.setTextColor(ContextCompat.getColor(this, R.color.grey_color))
        binding.ivNoneBlur.setColorFilter(
            ContextCompat.getColor(
                this, R.color.grey_color
            ), PorterDuff.Mode.SRC_IN
        )


        binding.cvTopBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this, R.color.un_selected_button
            )
        )
        binding.tvTopBlur.setTextColor(ContextCompat.getColor(this, R.color.grey_color))
        binding.ivTopBlur.setColorFilter(
            ContextCompat.getColor(
                this, R.color.grey_color
            ), PorterDuff.Mode.SRC_IN
        )


        binding.cvBottomBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this, R.color.un_selected_button
            )
        )
        binding.tvBottomBlur.setTextColor(ContextCompat.getColor(this, R.color.grey_color))
        binding.ivBottomBlur.setColorFilter(
            ContextCompat.getColor(
                this, R.color.grey_color
            ), PorterDuff.Mode.SRC_IN
        )


        binding.cvFullBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this, R.color.un_selected_button
            )
        )
        binding.tvFullBlur.setTextColor(ContextCompat.getColor(this, R.color.grey_color))
        binding.ivFullBlur.setImageResource(R.drawable.ic_full_blur)
    }

    private fun setNonBlur() {
        binding.cvNonBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.selected_btn_color
            )
        )
        binding.tvNoneBlur.setTextColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )

        binding.ivNoneBlur.setColorFilter(
            ContextCompat.getColor(this@VideoEditActivity, R.color.white), PorterDuff.Mode.SRC_IN
        )

    }

    private fun setTopBlur() {
        binding.cvTopBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.selected_btn_color
            )
        )
        binding.tvTopBlur.setTextColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )


        binding.ivTopBlur.setColorFilter(
            ContextCompat.getColor(this@VideoEditActivity, R.color.white), PorterDuff.Mode.SRC_IN
        )

    }

    private fun setBottomBlur() {
        binding.cvBottomBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.selected_btn_color
            )
        )
        binding.tvBottomBlur.setTextColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )

        binding.ivBottomBlur.setColorFilter(
            ContextCompat.getColor(this@VideoEditActivity, R.color.white), PorterDuff.Mode.SRC_IN
        )

    }

    private fun setFullBlur() {
        binding.cvFullBlur.setCardBackgroundColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.selected_btn_color
            )
        )
        binding.tvFullBlur.setTextColor(
            ContextCompat.getColor(
                this@VideoEditActivity, R.color.white
            )
        )
        binding.ivFullBlur.setImageResource(R.drawable.ic_full_blur_white)
    }

    override fun onProgressChanged(seekBar: SeekBar, progress: Int, p2: Boolean) {
        val whatToSay: String = progress.toString()
        binding.textView.text = whatToSay
        val `val` = progress * (seekBar.width - 3 * seekBar.thumbOffset) / seekBar.max
        "$progress%".also { binding.textView.text = it }
        binding.textView.x = seekBar.x + `val` + seekBar.thumbOffset / 2
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    //fixme
    override fun onStopTrackingTouch(seeBar: SeekBar?) {
        if (topBlur) {
            clearSessionTopBlur()
            blurVideoCommands(0, seeBar!!.progress)
        } else if (bottomBlur) {
            clearSessionBottomBlur()
            blurVideoCommands(1, seeBar!!.progress)
        } else {
            clearSessionFullBlur()
            blurVideoCommands(2, seeBar!!.progress)
        }
    }

    /* This function is set for execute ffmpeg command for scale down resolution of image
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */
    private fun execFfmpegForResolutionDown(
        command: Array<String>
    ) {
        showDialog()
        enableStatisticsCallback()
        isCompression = true
        val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {
                    binding.playerView.viewVisible()
                    isCompression = false
                    originalPath = lessResolatedFile
                    getHeight(lessResolatedFile)
                    preparePlayer(Uri.parse(lessResolatedFile))
                    mHandler = Handler()
                    mHandler?.post(updateProgressAction)
                    updateProgress()
                    exoPlayer?.addListener(videoPlayerListener)
                    popUpForCompressedVideo.dismiss()
                }
                Config.RETURN_CODE_CANCEL -> {
                    isDistortion = false
                    Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
                }
            }
        }

        progressId = executionId
    }


    /* In this function we execute all blur video commands video different checks
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */

    private fun blurVideoCommands(type: Int, radius: Int) {
        when (type) {
            0 -> {
                if (isDistortion) {
                    topBlurCommand(finalDistortedVideo, topBlurOutput, type, radius)
                    topBlurCommandWithOriginalPath(radius)
                } else if (isMuted) {
                    topBlurCommand(mutedVideo, topBlurOutput, type, radius)
                    topBlurCommandWithOriginalPath(radius)
                } else {
                    if (getTopBlur() != "") {
                        isBlurred = true
                        nonBlur = false
                        binding.cvTopBlur.isEnabled = false
                        binding.cvBottomBlur.isEnabled = true
                        binding.cvFullBlur.isEnabled = true
                        binding.blurLinearlayout.viewVisible()
                        outputForBlurredVideo = getTopBlur()
                        releasePlayer()
                        finalVideo = ""
                        setExoPlayer(getTopBlur())
                        enableShareAndSave()
                    } else {
                        topBlurCommand(originalPath!!, topBlurOutput, type, radius)
                    }
                }
            }
            1 -> {
                if (isDistortion) {
                    bottomBlurCommand(finalDistortedVideo, bottomBlurOutput, type, radius)
                    bottomBlurCommandWithOriginalInput(radius)
                } else if (isMuted) {
                    bottomBlurCommand(mutedVideo, bottomBlurOutput, type, radius)
                    bottomBlurCommandWithOriginalInput(radius)
                } else {
                    if (getBottomBlur() != "") {
                        isBlurred = true
                        nonBlur = false
                        binding.cvTopBlur.isEnabled = true
                        binding.cvBottomBlur.isEnabled = false
                        binding.cvFullBlur.isEnabled = true
                        binding.blurLinearlayout.viewVisible()
                        releasePlayer()
                        finalVideo = ""
                        setExoPlayer(getBottomBlur())
                        enableShareAndSave()
                    } else {
                        bottomBlurCommand(originalPath!!, bottomBlurOutput, type, radius)
                    }
                }
            }
            else -> {
                if (isDistortion) {
                    fullBlurCommand(finalDistortedVideo, fullBlurOutput, type, radius)
                    fullBlurCommandWithOriginalInput(radius)
                } else if (isMuted) {
                    fullBlurCommand(mutedVideo, fullBlurOutput, type, radius)
                    fullBlurCommandWithOriginalInput(radius)
                } else {
                    if (getFullBlur() != "") {
                        isBlurred = true
                        nonBlur = false
                        binding.cvTopBlur.isEnabled = true
                        binding.cvBottomBlur.isEnabled = true
                        binding.cvFullBlur.isEnabled = false
                        binding.blurLinearlayout.viewVisible()
                        releasePlayer()
                        finalVideo = ""
                        setExoPlayer(getFullBlur())
                        enableShareAndSave()
                    } else {
                        fullBlurCommand(originalPath!!, fullBlurOutput, type, radius)
                    }
                }
            }
        }
    }

    /* These function is command of topBlur
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */
    private fun topBlurCommand(input: String, output: String, type: Int, radius: Int) {
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
        execFfmpegBinaryForBlur(cmd, output, type, radius)
    }

    private fun topBlurCommandWithOriginalPath(radius: Int) {
        getHeight(originalPath!!)
        val cmd2 = arrayOf(
            "-y",
            "-i",
            originalPath!!,
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
            secondOutputForBlur,
            "-hide_banner"
        )
        execFfmpegBinaryForBlurForOriginalVideoPath(cmd2, secondOutputForBlur)
    }


    /* These function is command of bottomBlur
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */
    private fun bottomBlurCommand(input: String, output: String, type: Int, radius: Int) {
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
        execFfmpegBinaryForBlur(cmd, output, type, radius)

    }

    private fun bottomBlurCommandWithOriginalInput(radius: Int) {
        getHeight(originalPath!!)
        val cmd2 = arrayOf(
            "-y",
            "-i",
            originalPath!!,
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
            secondOutputForBlur,
            "-hide_banner"
        )
        execFfmpegBinaryForBlurForOriginalVideoPath(cmd2, secondOutputForBlur)
    }

    /* These function is command of fullBlur
    *  https://github.com/tanersener/mobile-ffmpeg
    *  */
    private fun fullBlurCommand(input: String, output: String, type: Int, radius: Int) {
        getHeight(input)
        val cmd = arrayOf(
            "-y",
            "-i",
            input,
            "-filter_complex",
            "[0:v]crop=" + videoWidth + ":" + videoHeight + ":" + 0 + ":" + 0 + ",gblur=$radius[blurred];[0:v][blurred]overlay=" + 0 + ":" + 0 + "[v]",
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
        execFfmpegBinaryForBlur(cmd, output, type, radius)

    }

    private fun fullBlurCommandWithOriginalInput(radius: Int) {
        getHeight(originalPath!!)
        val cmd2 = arrayOf(
            "-y",
            "-i",
            originalPath!!,
            "-filter_complex",
            "[0:v]crop=" + videoWidth + ":" + videoHeight + ":" + 0 + ":" + 0 + ",gblur=$radius[blurred];[0:v][blurred]overlay=" + 0 + ":" + 0 + "[v]",
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
            secondOutputForBlur,
            "-hide_banner"
        )
        execFfmpegBinaryForBlurForOriginalVideoPath(cmd2, secondOutputForBlur)
    }

    /* This function is actually execution of blur commands */
    private fun execFfmpegBinaryForBlur(
        command: Array<String>, outPut: String, type: Int, radius: Int
    ) {
        showDialog()
        exoPlayer?.pause()
        enableStatisticsCallback()
        lifecycleScope.launch {
            Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        isBlurred = true
                        dismissDialog()
                        releasePlayer()
                        finalVideo = ""
                        outputForBlurredVideo = outPut
                        nonBlur = false
                        setExoPlayer(outPut)
                        binding.blurLinearlayout.viewVisible()
                        enableShareAndSave()
                        when (type) {
                            0 -> {
                                setTopBlur(outPut)
                                binding.cvTopBlur.isEnabled = false
                                binding.cvBottomBlur.isEnabled = true
                                binding.cvFullBlur.isEnabled = true
                                topBlur = true
                                bottomBlur = false
                                fullBlur = false
                                topSeekBar = radius
                                if (isMuted) {
                                    mTopBlur = true
                                    mBottomBlur = false
                                    mFullBlur = false
                                    blurAndMuted = true
                                    mradius = radius
                                }
                            }
                            1 -> {
                                setBottomBlurPref(outPut)
                                binding.cvTopBlur.isEnabled = true
                                binding.cvBottomBlur.isEnabled = false
                                binding.cvFullBlur.isEnabled = true
                                bottomSeekBar = radius
                                topBlur = false
                                bottomBlur = true
                                fullBlur = false
                                if (isMuted) {
                                    mTopBlur = false
                                    mBottomBlur = true
                                    mFullBlur = false
                                    blurAndMuted = true
                                    mradius = radius
                                    bottomBlur = true
                                }
                            }
                            2 -> {
                                binding.cvTopBlur.isEnabled = true
                                binding.cvBottomBlur.isEnabled = true
                                binding.cvFullBlur.isEnabled = false
                                topBlur = false
                                bottomBlur = false
                                fullBlur = true
                                fullSeekBar = radius
                                setFullBlurPref(outPut)
                                if (isMuted) {
                                    mTopBlur = false
                                    mBottomBlur = false
                                    mFullBlur = true
                                    blurAndMuted = true
                                    mradius = radius
                                    fullBlur = true
                                }
                            }
                        }
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isBlurred = false
                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }

            progressId = executionId
        }
    }

    /* This function is execute blur command with actual path or default that we get from gallery so that we can use this blur video when user want to remove mute and distortion */
    private fun execFfmpegBinaryForBlurForOriginalVideoWithoutBlurForSoundDistortion(
        command: Array<String>, ourPut: String
    ) {
        showDialog()
        exoPlayer?.pause()
        enableStatisticsCallback()
        lifecycleScope.launch {
            Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        dismissDialog()
                        blurAndMuteVideo = ourPut

                        val folder = File(
                            Environment.getExternalStorageDirectory().toString() + "/extractAudio"
                        )
                        if (!folder.exists()) {
                            folder.mkdir()
                        }

                        val outputForExtractAudio: String =
                            getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + "/extract_audio_output.mp3"

                        val cmd = arrayOf(
                            "-y", "-i", // input path
                            blurAndMuteVideo, "-f",  // output format
                            "wav", "-ab",  // encode speed
                            "64k", "-vn",  // dont want video
                            outputForExtractAudio // output path
                        )
                        execFfmpegForExtractVoiceFromVideo(
                            cmd, outputForExtractAudio, blurAndMuteVideo
                        )
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                    else -> {

                    }
                }
            }

            progressId = executionId
        }
    }

    /* This function is execute blur command with actual path or default that we get from gallery so that we can use this blur video when user want to remove mute and distortion */
    private fun execFfmpegBinaryForBlurForOriginalVideoPath(
        command: Array<String>, ourPut: String
    ) {
        lifecycleScope.launch {
            Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        isBlurAndMuted = true
                        newVideo = ourPut
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }
            progressId = executionId
        }
    }

    //fixme
    /* This function is mute the video sound */
    private fun muteVideoCommands() {
        if (isBlurred && isDistortion) {
            muteCommand(outputForBlurredVideo)
        } else if (isBlurred) {
            muteCommand(outputForBlurredVideo)
        } else if (isDistortion) {
            muteCommand(finalOutput)
        } else {
            muteCommand(originalPath!!)
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
        showDialog()
        exoPlayer?.pause()
        enableStatisticsCallback()
        lifecycleScope.launch {
            Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        finalVideo = ""
                        dismissDialog()
                        releasePlayer()
                        setExoPlayer(outPut)
                        enableShareAndSave()
                        if (!isBlurred) mutedVideo = outPut
                        isMuted = true
                        isDistortion = false
                        binding.buttonLayouts.buttonRemoveAudio.isEnabled = false
                        binding.buttonLayouts.buttonRemoveDistortion.isEnabled = true

                        binding.buttonNoEffect.background = ContextCompat.getDrawable(
                            this@VideoEditActivity, R.drawable.ss_corner_round_white_blue_corener
                        )
                        binding.buttonNoEffect.isEnabled = true
                        if (isBlurred) {
                            val file2 =
                                File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
                            if (!file2.exists()) {
                                file2.mkdirs()
                            }
                            val downloadFile2 =
                                File(file2, "AUDIOREMOVE" + System.currentTimeMillis() + ".mp4")
                            val outPutForMute2 = downloadFile2.absolutePath

                            val cmd2 = arrayOf(
                                "-i",
                                getNewPath(originalPath!!),
                                "-c",
                                "copy",
                                "-an",
                                outPutForMute2
                            )
                            execFfmpegBinaryForMuteAudioWithOriginalInput(cmd2, outPutForMute2)
                        }
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                }
            }
            progressId = executionId
        }
    }

    /* This function is execute the mute video command with original Path that came from gallery */
    private fun execFfmpegBinaryForMuteAudioWithOriginalInput(
        command: Array<String>, outPut: String
    ) {
        Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
        val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {
                    mutedVideo = outPut
                }
                Config.RETURN_CODE_CANCEL -> {
                    Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                    Log.e(Config.TAG, "Async command execution canceled by user")
                }
            }
        }
        progressId = executionId

    }

    /* This function is extract the audio or wav file from video or mp4. */
    private fun audioExtractCommand() {
        folder = File(Environment.getExternalStorageDirectory().toString() + "/extractAudio")
        if (!folder!!.exists()) {
            folder!!.mkdir()
        }
        val outputForExtractAudio: String =
            getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + "/extract_audio_output.mp3"

        if (blurAndMuted) {
            when {
                mTopBlur -> {
                    getHeight(originalPath!!)
                    val cmd = arrayOf(
                        "-y",
                        "-i",
                        getNewPath(originalPath!!),
                        "-filter_complex",
                        "[0:v]crop=" + videoWidth + ":" + videoHeight / 2 + ":" + videoHeight % 2 + ":" + 0 + ",gblur=$mradius[blurred];[0:v][blurred]overlay=" + videoHeight % 2 + ":" + 0 + "[v]",
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
                        outPutForMutedBlurredVideo,
                        "-hide_banner"
                    )
                    execFfmpegBinaryForBlurForOriginalVideoWithoutBlurForSoundDistortion(
                        cmd, outPutForMutedBlurredVideo
                    )
                }
                mBottomBlur -> {
                    getHeight(originalPath!!)
                    val cmd = arrayOf(
                        "-y",
                        "-i",
                        getNewPath(originalPath!!),
                        "-filter_complex",
                        "[0:v]crop=" + videoWidth + ":" + videoHeight / 2 + ":" + 0 + ":" + videoHeight / 2 + ",gblur=$mradius[blurred];[0:v][blurred]overlay=" + 0 + ":" + videoHeight / 2 + "[v]",
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
                        outPutForMutedBlurredVideo,
                        "-hide_banner"
                    )
                    execFfmpegBinaryForBlurForOriginalVideoWithoutBlurForSoundDistortion(
                        cmd, outPutForMutedBlurredVideo
                    )
                }
                else -> {
                    getHeight(originalPath!!)
                    val cmd = arrayOf(
                        "-y",
                        "-i",
                        getNewPath(originalPath!!),
                        "-filter_complex",
                        "[0:v]crop=" + videoWidth + ":" + videoHeight + ":" + 0 + ":" + 0 + ",gblur=$mradius[blurred];[0:v][blurred]overlay=" + 0 + ":" + 0 + "[v]",
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
                        outPutForMutedBlurredVideo,
                        "-hide_banner"
                    )
                    execFfmpegBinaryForBlurForOriginalVideoWithoutBlurForSoundDistortion(
                        cmd, outPutForMutedBlurredVideo
                    )
                }
            }

            val cmd1 = arrayOf(
                "-y", "-i", // input path
                getNewPath(originalPath!!), "-f",  // output format
                "wav", "-ab",  // encode speed
                "64k", "-vn",  // dont want video
                outputForExtractAudio // output path
            )
            execFfmpegForExtractVoiceFromVideoWithOriginalVideoOrPath(
                cmd1, outputForExtractAudio, getNewPath(originalPath!!)
            )
        } else if (nonBlur) {
            val cmd = arrayOf(
                "-y", "-i", // input path
                getNewPath(originalPath!!), "-f",  // output format
                "wav", "-ab",  // encode speed
                "64k", "-vn",  // dont want video
                outputForExtractAudio // output path
            )
            execFfmpegForExtractVoiceFromVideo(
                cmd, outputForExtractAudio, getNewPath(originalPath!!)
            )
        } else if (isBlurred) {
            val cmd = arrayOf(
                "-y", "-i", // input path
                outputForBlurredVideo, "-f",  // output format
                "wav", "-ab",  // encode speed
                "64k", "-vn",  // dont want video
                outputForExtractAudio // output path
            )

            val cmd1 = arrayOf(
                "-y", "-i", // input path
                getNewPath(originalPath!!), "-f",  // output format
                "wav", "-ab",  // encode speed
                "64k", "-vn",  // dont want video
                outputForExtractAudio // output path
            )

            execFfmpegForExtractVoiceFromVideo(cmd, outputForExtractAudio, outputForBlurredVideo)
            execFfmpegForExtractVoiceFromVideoWithOriginalVideoOrPath(
                cmd1, outputForExtractAudio, getNewPath(originalPath!!)
            )
        } else {
            val cmd = arrayOf(
                "-y", "-i", // input path
                getNewPath(originalPath!!), "-f",  // output format
                "wav", "-ab",  // encode speed
                "64k", "-vn",  // dont want video
                outputForExtractAudio // output path
            )
            execFfmpegForExtractVoiceFromVideo(
                cmd, outputForExtractAudio, getNewPath(originalPath!!)
            )
        }
    }

    /* This function is execute mp4 to wav command */
    private fun execFfmpegForExtractVoiceFromVideo(
        command: Array<String>, strAudioPath: String, inputForAttachVoice: String
    ) {
        lifecycleScope.launch {
            Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        enableShareAndSave()
                        audioChangeLayout.viewVisible()
                        doSoundTouchProcessing(strAudioPath, inputForAttachVoice)
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isDistortion = false
                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                    else -> {

                    }
                }
            }
            progressId = executionId
        }
    }

    /* This function is execute to extract audio or mp3 from mp4 with original Video */
    private fun execFfmpegForExtractVoiceFromVideoWithOriginalVideoOrPath(
        command: Array<String>, strAudioPath: String, inputForAttachVoice: String
    ) {
        lifecycleScope.launch {
            Log.d(ContentValues.TAG, "Started command : ffmpeg " + command.contentToString())
            val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
                when (returnCode) {
                    Config.RETURN_CODE_SUCCESS -> {
                        doSoundTouchProcessingWithOriginalVideo(strAudioPath, inputForAttachVoice)
                    }
                    Config.RETURN_CODE_CANCEL -> {
                        isDistortion = false
                        Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                        Log.e(Config.TAG, "Async command execution canceled by user")
                    }
                    else -> {

                    }
                }
            }

            progressId = executionId
        }
    }

    /*  Function that does the SoundTouch processing */
    private fun doSoundTouchProcessing(input: String, inputForAttachVoice: String): Long {
        val st = SoundTouch()
        st.setTempo(1f)
        st.setPitchSemiTones(6f)
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
            Toast.makeText(this, "Failure: $err", Toast.LENGTH_SHORT).show()
            return -1L
        }
        return 0L
    }

    /*  Function that does the SoundTouch processing with original video input only.
    Source Url:
    https://github.com/qingmei2/soundtouch-android
*/
    private fun doSoundTouchProcessingWithOriginalVideo(
        input: String, inputForAttachVoice: String
    ): Long {
        val st = SoundTouch()
        st.setTempo(1f)
        st.setPitchSemiTones(6f)
        val extortedAudioOutput: String =
            getExternalFilesDir(Environment.DIRECTORY_DCIM)?.absolutePath + "/pitch_change_output.mp3"

        val res = st.processFile(input, extortedAudioOutput)
        val folder = File(Environment.getExternalStorageDirectory().toString() + "/ExtractedVideos")
        if (!folder.exists()) {
            folder.mkdir()
        }
        val file1 =
            File(getExternalFilesDir(applicationContext.resources.getString(R.string.app_name)).toString())
        if (!file1.exists()) {
            file1.mkdirs()
        }
        val downloadFile = File(file1, generateBlurName() + ".mp4")

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

        execFfmpegForAttachAudioWithVideoWithOriginalInput(cmd, finalOutput)
        if (res != 0) {
            Toast.makeText(this, "Failure: $err", Toast.LENGTH_SHORT).show()
            return -1L
        }
        return 0L
    }

    /*  Function that attach the distorted voice to video again */
    private fun execFfmpegForAttachAudioWithVideo(
        command: Array<String>, distortedFilePath: String
    ) {
        if (blurAndMuted) {
            showDialog()
            exoPlayer?.pause()
            enableStatisticsCallback()
        }

        val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {
                    dismissDialog()
                    releasePlayer()
                    finalVideo = ""
                    setExoPlayer(distortedFilePath)
                    isDistortion = true
                    isMuted = false
                    audioDistortion.viewVisible()
                    audioRemove.viewGone()
                    enableShareAndSave()
                    binding.buttonNoEffect.background = ContextCompat.getDrawable(
                        this@VideoEditActivity, R.drawable.ss_corner_round_white_blue_corener
                    )

                    binding.buttonNoEffect.isEnabled = true
                    if (!isBlurred) finalDistortedVideo = distortedFilePath
                    binding.buttonLayouts.buttonRemoveAudio.isEnabled = true
                    binding.buttonLayouts.buttonRemoveDistortion.isEnabled = false
                }
                Config.RETURN_CODE_CANCEL -> {
                    isDistortion = false
                    Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                    Log.e(Config.TAG, "Async command execution canceled by user")
                }
            }
        }
        progressId = executionId
    }

    /*  Function that attach the distorted voice with original video */
    private fun execFfmpegForAttachAudioWithVideoWithOriginalInput(
        command: Array<String>, distortedFilePath: String
    ) {
        val executionId: Long = FFmpeg.executeAsync(command) { _, returnCode ->
            when (returnCode) {
                Config.RETURN_CODE_SUCCESS -> {
                    finalDistortedVideo = distortedFilePath
                }
                Config.RETURN_CODE_CANCEL -> {
                    Toast.makeText(this@VideoEditActivity, "Fail", Toast.LENGTH_SHORT).show()
                    Log.e(Config.TAG, "Async command execution canceled by user")
                }
            }
        }
        progressId = executionId
    }

    /* Save video to gallery by creating folder*/
    private fun saveVideo(_pathOf: String) {
        val pathOf = _pathOf
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
        setSaveDialogue()

    }

    /* Set the save dialogue */
    private fun setSaveDialogue() {
        exoPlayer?.pause()
        val dialog = Dialog(this@VideoEditActivity)
        dialog.setContentView(R.layout.custom_progressbar)

        val progress = dialog.findViewById<ProgressBar>(R.id.progressBar)
        val textView = dialog.findViewById<TextView>(R.id.tv)
        val ivDone = dialog.findViewById<ImageView>(R.id.iv_done)

        val mHandler = Handler()
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
                mHandler.post(Runnable {
                    if (progressBarStatus == 99) {
                        ivDone.viewVisible()
                        progress.viewGone()
                        textView.text = getString(R.string.video_saved)
                    } else {
                        progress.progress = progressBarStatus
                        "Exporting Video ${progressBarStatus}%".also { textView.text = it }
                    }

                })
            }

        }.start()

        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)
        dialog.show()

        val handler = Handler()
        handler.postDelayed({
            dialog.cancel()
            if (exoPlayer != null) exoPlayer?.play()
            disableShareAndSave()
        }, 4000)
    }

    private fun showDialog() {
        statistics = null
        Config.resetStatistics()
        popUpForCompressedVideo.show(supportFragmentManager, "")
        popUpForCompressedVideo.isCancelable = false
    }

    private fun dismissDialog() {
        popUpForCompressedVideo.dismiss()
    }

    /* Enable callBack that show progress of video that is converted */
    private fun enableStatisticsCallback() {
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
                    popUpForCompressedVideo.showProgressForBlur(
                        String.format("%s", completePercentage).toFloat()
                    )
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
            Integer.valueOf(
                it
            )
        }!!
        Log.d("dddddddd", "widh" + videoWidth)
        Log.d("dddddddd", "heigth" + videoHeight)
    }

    /* This function is get total duration of video */
    private fun getTotalDuration(uri: String) {
        totalDuration = Constants.getDuration(this@VideoEditActivity, Uri.parse(uri))
        lastMaxValue = totalDuration
        val range: Int = (lastMaxValue - 0).toInt()
        val rangeVal = range.toString()
        dur = rangeVal.toFloat() * 1000
    }

    private fun disableShareAndSave() {
        binding.homeToolbarId.ivDownload.isEnabled = false
        binding.homeToolbarId.ivDownload.alpha = 0.5f
        binding.homeToolbarId.ivShare.isEnabled = false
        binding.homeToolbarId.ivShare.alpha = 0.5f
    }

    private fun enableShareAndSave() {
        binding.homeToolbarId.ivDownload.isEnabled = true
        binding.homeToolbarId.ivDownload.alpha = 1f
        binding.homeToolbarId.ivShare.isEnabled = true
        binding.homeToolbarId.ivShare.alpha = 1f
    }

    override fun onBackPressed() {
        exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
            override fun onGoToHomeOk() {
                onBackPressedDispatcher.onBackPressed() //with this line
                exitPopup.dismiss()
                deleteFiles()
                if (progressId != 0L) {
                    FFmpeg.cancel(progressId)
                    FFmpeg.cancel()
                }
                clearSessionsOfFullTopAndBottomBlur()
            }

            override fun onGoToHomeCancel() {
                exitPopup.dismiss()

            }

        }, "back")
        exitPopup.show(supportFragmentManager, "")
        exitPopup.isCancelable = true
    }

    private fun deleteFiles() {
        if (file3 != null) file3!!.delete()
        if (file1 != null) file1!!.delete()
        if (file2 != null) file2!!.delete()
        if (fileForMute != null) fileForMute!!.delete()
        if (extractAudioFile != null) extractAudioFile!!.delete()
        if (folder != null) folder!!.delete()
        if (topBlurOutputFile != null) topBlurOutputFile!!.delete()
        if (bottomBlurOutputFile != null) bottomBlurOutputFile!!.delete()
        if (fullBlurOutputFile != null) fullBlurOutputFile!!.delete()
    }

    /* This function is used for clear the session of top bottom and full blur*/
    private fun clearSessionsOfFullTopAndBottomBlur() {
        clearSessionTopBlur()
        clearSessionBottomBlur()
        clearSessionFullBlur()
    }

}