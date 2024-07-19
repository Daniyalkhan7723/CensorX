package com.censorchi.utils

import android.app.Activity
import android.media.MediaMetadataRetriever
import android.net.Uri

object Constants {
    const val PERMISSION_REQUEST_CODE = 34
    const val REQUEST_CODE_PICK_IMAGE = 1
    const val SHARED_PREFERENCE_TOP = "shared preferences Top"
    const val SHARED_PREFERENCE_BOTTOM = "shared preferences Bottom"
    const val SHARED_PREFERENCE_FULL = "shared preferences Full"
    const val SHARED_PREFERENCE_BOOLEAN = "shared preferences Boolean"
    const val IMAGE_DIRECTORY = "/CensorX_qImages"
    const val SCALING_FACTOR = 3
    const val GET_STARTED = "getStarted"
    const val IS_BACK = "isBack"
    const val TOP_BLUR = "topBlur"
    const val RequestPermissionCode_write = 1
    const val BOTTOM_BLUR = "bottomBlur"
    const val FULL_BLUR = "fullBlur"
    const val TAG = "FACE_DETECT_TAG"
    const val CENSOR_X = "CENSOR_X"

    //Minimum Video you want to buffer while Playing
    const val MIN_BUFFER_DURATION = 2000

    //Max Video you want to buffer during PlayBack
    const val MAX_BUFFER_DURATION = 5000

    //Min Video you want to buffer before start Playing it
    const val MIN_PLAYBACK_START_BUFFER = 1500

    //Min video You want to buffer when user resumes video
    const val MIN_PLAYBACK_RESUME_BUFFER = 2000

    const val Full_VIDEO_PATH = "_FULLVIDEO"
    const val CENSORX_TOP_BLUR = "_CROPTOP"
    const val CROP_BOTTOM_BLUR= "CROPBOTTOM_"
    const val CROP_BOTTOM_BLUR_TWO= "CROPBOTTOMTWO_"
    const val CENSORX_BOTTOM_BLUR= "CENSORX_"
    const val CENSORX_FULL_BLUR = "_CENSORX_"
    const val IMAGE_PATH = "imagePath"
    const val VIDEO_PATH = "videoPath"
    const val IMAGE = "image"
    const val VIDEO = "video"

    fun getDuration(context: Activity?, videoPath: Uri?): Long {
        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, videoPath)
            val time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val timeInMillisec = time!!.toLong()
            retriever.release()
            return timeInMillisec / 1000
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return 0
    }

    fun formatSeconds(timeInSeconds: Long): String {
        val hours = timeInSeconds / 3600
        val secondsLeft = timeInSeconds - hours * 3600
        val minutes = secondsLeft / 60
        val seconds = secondsLeft - minutes * 60
        var formattedTime = ""
        if (hours < 10 && hours != 0L) {
            formattedTime += "0"
            formattedTime += "$hours:"
        }
        if (minutes < 10) formattedTime += "0"
        formattedTime += "$minutes:"
        if (seconds < 10) formattedTime += "0"
        formattedTime += seconds
        return formattedTime
    }

}