package com.censorchi.views.activities

import android.annotation.SuppressLint
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.DisplayMetrics
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.censorchi.R
import com.censorchi.databinding.ActivityImageEditBinding
import com.censorchi.utils.*
import com.censorchi.utils.Constants.CENSOR_X
import com.censorchi.utils.Constants.IMAGE_PATH
import com.censorchi.utils.Constants.SCALING_FACTOR
import com.censorchi.utils.Constants.TAG
import com.censorchi.utils.imageBlurUtils.ImageViewTouchAndDraw
import com.censorchi.utils.imageBlurUtils.PinchImageView
import com.censorchi.utils.imageBlurUtils.ResizeImage
import com.censorchi.views.popUp.ExitDialogue
import com.censorchi.views.popUp.MaterialDialogHelper
import com.google.android.exoplayer2.util.NalUnitUtil
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import cz.msebera.android.httpclient.conn.params.ConnPerRouteBean
import cz.msebera.android.httpclient.impl.client.cache.CacheConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.abs

class ImageEditActivity : BaseActivity(), View.OnTouchListener, SeekBar.OnSeekBarChangeListener {
    private lateinit var binding: ActivityImageEditBinding
    private var brushWidth = 50
    private var colorized: Bitmap? = null
    private var bitmap: Bitmap? = null
    private var blrValue = 0
    private lateinit var detector: FaceDetector
    private var currentMode = 1
    private var imagePath: String? = null
    private var imageViewHeight = 0
    private var imageViewWidth = 0
    private var mPaint: Paint? = null
    private var mX = 0f
    private var mY = 0f
    private var myCombineCanvas: Canvas? = null
    private var myCombinedBitmap: Bitmap? = null
    private var pcanvas: Canvas? = null
    private var saveShareBitmap: Bitmap? = null
    private var screenHeight = 0
    private var screenWidth = 0
    private var tmpPath: Path? = null
    private var topImage: Bitmap? = null
    private var newBitmap: Bitmap? = null
    private var checkBitmap: Bitmap? = null
    private var currentShowingIndex = -1
    private lateinit var exitPopup: ExitDialogue
    private lateinit var bitmapsForUndo: ArrayList<Bitmap>
    private lateinit var lastTenDataList: ArrayList<Bitmap>
    private lateinit var listForCheckActiveAutoBlur: ArrayList<String>
    private lateinit var listForCheckActiveAutoBlurLastTen: ArrayList<String>
    private var isZoomRequired = false
    private var brushSize = false
    private var isFirstTimeLaunch = false
    private var unableTouchBlur: Boolean = false
    private var selectAndDeselectButtonCheck: Boolean = false
    private var autoBlurActive: Boolean = false

    companion object {
        @JvmField
        var setScroll: Boolean = false
    }

    private fun getMatrixValues(m: Matrix): FloatArray {
        val values = FloatArray(9)
        m.getValues(values)
        return values
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ActivityImageEditBinding.inflate(layoutInflater).also { binding = it }
        fullScreenWithStatusBarWhiteIcon()
        setContentView(binding.root)
        initValues()
        disableShareAndSave()

        if (topImage == null) {
            Toast.makeText(this@ImageEditActivity, getString(R.string.image_not_supported), Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        addToUndoList()
        binding.blurImage.setImageBitmapReset(topImage, true, null)
        binding.blurImage1.setImageBitmapReset(topImage, true, null)
        binding.blurImage1.setOnTouchListener(this)
        binding.blurSeekbar.setOnSeekBarChangeListener(this)
        binding.blurSeekbar.progress = 50
        binding.blurSeekbar.max = 100

        setDefaultLayoutForButtons()
        listeners()
        setButtonsVisibility()
    }

    /* Initialize Values */
    private fun initValues() {
        bitmapsForUndo = ArrayList()
        lastTenDataList = ArrayList()
        listForCheckActiveAutoBlur = ArrayList()
        listForCheckActiveAutoBlurLastTen = ArrayList()
        isZoomRequired = true
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        screenWidth = displayMetrics.widthPixels
        screenHeight = displayMetrics.heightPixels

        try {
            imagePath = intent.getStringExtra(IMAGE_PATH)
        } catch (e: java.lang.Exception) {
            Log.d(TAG, e.toString())
        }
        topImage = ResizeImage(
            applicationContext
        ).getBitmap(imagePath, screenWidth)

        val realTimeFdo = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE).enableTracking()
            .build()
        detector = FaceDetection.getClient(realTimeFdo)
    }


    /* This method is used for detect faces from bitmap with ML kit
    * https://developers.google.com/ml-kit/guides
    * */
    private fun analyzePhoto(detectBitmap: Bitmap) {
         val smallerBitmap = Bitmap.createScaledBitmap(
            detectBitmap,
            detectBitmap.width / SCALING_FACTOR,
            detectBitmap.height / SCALING_FACTOR,
            false
        )
        val image = InputImage.fromBitmap(smallerBitmap, 0)

        detector.process(image).addOnSuccessListener {
             if (it.isEmpty()) {
                autoBlurActive = true
                Toast.makeText(this, getString(R.string.no_face_found), Toast.LENGTH_SHORT).show()
            } else {

                if (newBitmap != null) {
                    newBitmap = createFinalImage()!!
                }

                for (face in it) {
                    val rect = face.boundingBox
                    rect.set(
                        rect.left * SCALING_FACTOR,
                        rect.top * (SCALING_FACTOR - 1),
                        rect.right * (SCALING_FACTOR),
                        rect.bottom * SCALING_FACTOR + 90
                    )

                    if (newBitmap != null) {
                        cropDetectFace(newBitmap!!, face)
                    } else {
                        cropDetectFace(detectBitmap, face)
                    }
                }

                if (newBitmap != null) {
                    bitmap = newBitmap
                    binding.blurImage.setImageBitmapReset(newBitmap, true, null)
                    binding.blurImage1.setImageBitmapReset(newBitmap, true, null)

                    if (bitmapsForUndo.size >= 11) {
                        if (currentShowingIndex != 0) {
                            currentShowingIndex--
                            bitmapsForUndo[0].recycle()
                            bitmapsForUndo.removeAt(0)
                        }
                    }
                    addToUndoListTwo(newBitmap!!)
                    setButtonsVisibility()
                    enableShareAndSave()
                    autoBlurActive = true
                    binding.buttonLayouts.buttonRemoveAudio.isEnabled = false
                }
            }
        }.addOnFailureListener {
             Toast.makeText(this, "Failed due to ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /* This method is used for crop detect Faces */
    private fun cropDetectFace(bitmap: Bitmap, faces: Face) {
        var mBitmap = bitmap
        val rect = faces.boundingBox
        val x = rect.left.coerceAtLeast(0)
        val y = rect.top.coerceAtLeast(0)
        val width = rect.width()
        val height = rect.height()

        val croppedBitmap = Bitmap.createBitmap(
            mBitmap,
            x,
            y,
            if (x + width > mBitmap.width) mBitmap.width - x else width,
            if (y + height > mBitmap.height) mBitmap.height - y else height
        )

        val mutableBitmap = mBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val c = Canvas(mutableBitmap)
        var map1: Bitmap? = null
        var map2: Bitmap? = null
        for (i in 0..9) {
            map1 = if (map2 != null) {
                blurImage(map2, 25)
            } else {
                blurImage(croppedBitmap, 25)
            }
            map2 = map1
        }
        if (map1 != null) {
            c.drawBitmap(map1, rect.left.toFloat(), rect.top.toFloat(), null)
        }
        newBitmap = mutableBitmap
    }

    /* This method is used for blur detect faces with renderscript */
    private fun blurImage(input: Bitmap, radius: Int): Bitmap? {
        //Radius range (0 < r <= 25)
        var mradius = radius.toFloat()
        if (mradius <= 0) {
            mradius = 0.1f
        } else if (mradius > 25) {
            mradius = 25.0f
        }
        val rsScript = RenderScript.create(this@ImageEditActivity)
        val alloc: Allocation = Allocation.createFromBitmap(rsScript, input)
        val blur = ScriptIntrinsicBlur.create(rsScript, Element.U8_4(rsScript))
        blur.setRadius(mradius)
        blur.setInput(alloc)
        val result = Bitmap.createBitmap(input.width, input.height, input.config)
        val outAlloc: Allocation = Allocation.createFromBitmap(rsScript, result)
        blur.forEach(outAlloc)
        outAlloc.copyTo(result)
        rsScript.destroy()
        return result
    }

    /* This algo is used for blur live image by swipe */
    private fun fastBlur(sentBitmap: Bitmap, radius: Int): Bitmap? {
        val bitmap = sentBitmap.copy(sentBitmap.config, true)
        if (radius < 1) {
            return null
        }
        var i: Int
        var y: Int
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
         bitmap.getPixels(pix, 0, w, 0, 0, w, h)
        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1
        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        val vMin = IntArray(w.coerceAtLeast(h))
        var divSum = div + 1 shr 1
        divSum *= divSum
        val dv = IntArray(divSum * 256)
        i = 0
        while (i < divSum * 256) {
            dv[i] = i / divSum
            i++
        }
        var yi = 0
        var yw = 0
        val stack = java.lang.reflect.Array.newInstance(
            Integer.TYPE, div, 3
        ) as Array<IntArray>
        val r1 = radius + 1
        y = 0
        while (y < h) {
            var bsum = 0
            var gsum = 0
            var rsum = 0
            var boutsum = 0
            var goutsum = 0
            var routsum = 0
            var binsum = 0
            var ginsum = 0
            var rinsum = 0
            i = -radius
            while (i <= radius) {
                val p = pix[wm.coerceAtMost(i.coerceAtLeast(0)) + yi]
                val sir = stack[i + radius]
                sir[0] = 16711680 and p shr 16
                sir[1] = 65280 and p shr 8
                sir[2] = p and NalUnitUtil.EXTENDED_SAR
                val rbs = r1 - abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            var stackpointer = radius
            var x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                var sir = stack[(stackpointer - radius + div) % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (y == 0) {
                    vMin[x] = (x + radius + 1).coerceAtMost(wm)
                }
                val p = pix[vMin[x] + yw]
                sir[0] = 16711680 and p shr 16
                sir[1] = 65280 and p shr 8
                sir[2] = p and NalUnitUtil.EXTENDED_SAR
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi++
                x++
            }
            yw += w
            y++
        }
        for (x in 0 until w) {
            var bsum = 0
            var gsum = 0
            var rsum = 0
            var boutsum = 0
            var goutsum = 0
            var routsum = 0
            var binsum = 0
            var ginsum = 0
            var rinsum = 0
            var yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = 0.coerceAtLeast(yp) + x
                val sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                val rbs = r1 - abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            var stackpointer = radius
            y = 0
            while (y < h) {
                pix[yi] = -16777216 and pix[yi] or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]
                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum
                var sir = stack[(stackpointer - radius + div) % div]
                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]
                if (x == 0) {
                    vMin[y] = Math.min(y + r1, hm) * w
                }
                val p = x + vMin[y]
                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]
                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]
                rsum += rinsum
                gsum += ginsum
                bsum += binsum
                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]
                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]
                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]
                yi += w
                y++
            }
        }
        Log.e("pix", w.toString() + " " + h + " " + pix.size)
        bitmap.setPixels(pix, 0, w, 0, 0, w, h)
        return bitmap
    }

    /* This method is used for combined blur image and draw image */
    private fun combinedTopImage(bmp1: Bitmap?, bmp2: Bitmap?) {
        if (myCombinedBitmap != null) {
            myCombinedBitmap!!.recycle()
            myCombinedBitmap = null
        }
        myCombinedBitmap =
            Bitmap.createBitmap(imageViewWidth, imageViewHeight, Bitmap.Config.ARGB_8888)
        myCombineCanvas = Canvas()
        myCombineCanvas!!.setBitmap(myCombinedBitmap)
        myCombineCanvas!!.drawBitmap(bmp1!!, 0.0f, 0.0f, null)
        myCombineCanvas!!.drawBitmap(bmp2!!, 0.0f, 0.0f, null)
        if (bitmap != null) {
            bitmap!!.recycle()
            bitmap = null
            bitmap = Bitmap.createBitmap(imageViewWidth, imageViewHeight, Bitmap.Config.ARGB_8888)
        }
        pcanvas = Canvas()
        pcanvas!!.setBitmap(bitmap)
        pcanvas!!.drawBitmap(myCombinedBitmap!!, 0.0f, 0.0f, null)
    }

    /* This is listener that handle touch when user touch on image */
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (unableTouchBlur) {
            val z = false
            val view = v as ImageViewTouchAndDraw
            view.scaleType = ImageView.ScaleType.MATRIX
            binding.blurImage.scaleType = ImageView.ScaleType.MATRIX
            if (isZoomRequired) {
                return try {
                    binding.blurImage.drawMode = ImageViewTouchAndDraw.TouchMode.IMAGE
                    binding.blurImage1.drawMode = ImageViewTouchAndDraw.TouchMode.IMAGE
                    binding.blurImage.onTouchEvent(event)
                    drawMode(1, 1)
                    view.onTouchEvent(event)
                } catch (e: IllegalArgumentException) {
                    z
                }
            }
            val mInvertedMatrix = Matrix()
            val m1 = Matrix(view.imageMatrix)
            mInvertedMatrix.reset()
            val v1: FloatArray = getMatrixValues(m1)
            m1.invert(m1)
            val v2: FloatArray = getMatrixValues(m1)
            mInvertedMatrix.postTranslate(-v1[2], -v1[5])
            mInvertedMatrix.postScale(v2[0], v2[4])
            pcanvas!!.setMatrix(mInvertedMatrix)
            when (event.action) {
                0 -> {
                     touchStart(event.x, event.y)
                }
                CacheConfig.DEFAULT_MAX_UPDATE_RETRIES -> touchUp()
                ConnPerRouteBean.DEFAULT_MAX_CONNECTIONS_PER_ROUTE -> {
                     touchMove(event.x, event.y)
                }
            }

            view.setImageBitmap(bitmap)
            if (event.action == MotionEvent.ACTION_UP) {
                if (bitmapsForUndo.size >= 11) {
                    if (currentShowingIndex != 0) {
                        currentShowingIndex--
                        bitmapsForUndo[0].recycle()
                        bitmapsForUndo.removeAt(0)
                        listForCheckActiveAutoBlur.removeAt(0)
                    }
                }
                if (newBitmap != null) {
                    checkBitmap = createFinalImage()
                 }
                addToUndoList()
                enableShareAndSave()
            }
            setButtonsVisibility()
            return true

        } else {
            val z = false
            val view = v as ImageViewTouchAndDraw
            view.scaleType = ImageView.ScaleType.MATRIX
            binding.blurImage.scaleType = ImageView.ScaleType.MATRIX
            if (isZoomRequired) {
                return try {
                    binding.blurImage.drawMode = ImageViewTouchAndDraw.TouchMode.IMAGE
                    binding.blurImage1.drawMode = ImageViewTouchAndDraw.TouchMode.IMAGE
                    binding.blurImage.onTouchEvent(event)
                    view.onTouchEvent(event)
                } catch (e: IllegalArgumentException) {
                    z
                }
            }
            val mInvertedMatrix = Matrix()
            val m1 = Matrix(view.imageMatrix)
            mInvertedMatrix.reset()
            val v1: FloatArray = getMatrixValues(m1)
            m1.invert(m1)
            val v2: FloatArray = getMatrixValues(m1)
            mInvertedMatrix.postTranslate(-v1[2], -v1[5])
            mInvertedMatrix.postScale(v2[0], v2[4])
            return false
        }
    }

    /* This is method is draw or paint that we touch on image */
    private fun drawMode(previousMode: Int, currentMode: Int) {
        if (isFirstTimeLaunch) {
            isFirstTimeLaunch = false
            when (currentMode) {
                CacheConfig.DEFAULT_MAX_UPDATE_RETRIES -> {
                    binding.blurImage.setImageBitmapReset(colorized, true, null)
                    binding.blurImage1.setImageBitmapReset(bitmap, true, null)
                    return
                }
                ConnPerRouteBean.DEFAULT_MAX_CONNECTIONS_PER_ROUTE -> {
                    binding.blurImage.setImageBitmapReset(topImage, true, null)
                    binding.blurImage1.setImageBitmapReset(bitmap, true, null)
                    return
                }
                else -> return
            }
        }
        if (previousMode != 0) {
            combinedTopImage(
                (binding.blurImage.drawable as BitmapDrawable).bitmap,
                (binding.blurImage1.drawable as BitmapDrawable).bitmap
            )
        }
        when (currentMode) {
            CacheConfig.DEFAULT_MAX_UPDATE_RETRIES -> {
                binding.blurImage.setImageBitmapReset(colorized, false, null)
                binding.blurImage1.setImageBitmapReset(bitmap, false, null)
                binding.blurImage1.setOnTouchListener(this)
                return
            }
            ConnPerRouteBean.DEFAULT_MAX_CONNECTIONS_PER_ROUTE -> {
                binding.blurImage.setImageBitmapReset(topImage, false, null)
                binding.blurImage1.setImageBitmapReset(bitmap, false, null)
                binding.blurImage1.setOnTouchListener(this)
                return
            }
            else -> return
        }
    }


    /* Here we initialize the paint variables  */
    private fun initFunction(mBitmap: Bitmap) {
        mPaint = Paint().apply {
            alpha = 0
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = brushWidth.toFloat()
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            maskFilter = BlurMaskFilter(15.0f, BlurMaskFilter.Blur.NORMAL)
            isFilterBitmap = false
        }
        tmpPath = Path()
        bitmap = Bitmap.createBitmap(imageViewWidth, imageViewHeight, Bitmap.Config.ARGB_8888)
        pcanvas = Canvas().apply {
            setBitmap(bitmap)
            drawBitmap(mBitmap, 0.0f, 0.0f, null)
        }
        isFirstTimeLaunch = true
        drawMode(0, currentMode)
    }

    /* This method is call when touch is start on image  */

    private fun touchStart(x: Float, y: Float) {
        tmpPath!!.reset()
        tmpPath!!.moveTo(x, y)
        mX = x
        mY = y
        mPaint!!.strokeWidth = brushWidth.toFloat()
    }

    /* This method is call when we touch on image  */
    private fun touchMove(x: Float, y: Float) {
        val dx = Math.abs(x - mX)
        val dy = Math.abs(y - mY)
        if (dx >= PinchImageView.MIN_SCALE || dy >= PinchImageView.MIN_SCALE) {
            tmpPath!!.quadTo(mX, mY, (mX + x) / 2.0f, (mY + y) / 2.0f)
            pcanvas!!.drawPath(tmpPath!!, mPaint!!)
            tmpPath!!.reset()
            tmpPath!!.moveTo((mX + x) / 2.0f, (mY + y) / 2.0f)
            mX = x
            mY = y
        }
    }

    private fun touchUp() {
        tmpPath!!.reset()
    }

    /* This method a final bitmap for us that we can save */
    private fun createFinalImage(): Bitmap? {
        if (!(saveShareBitmap == null || saveShareBitmap!!.isRecycled)) {
            saveShareBitmap!!.recycle()
            saveShareBitmap = null
            System.gc()
        }
        saveShareBitmap =
            Bitmap.createBitmap(topImage!!.width, topImage!!.height, Bitmap.Config.ARGB_8888)
        val bottom = (binding.blurImage.drawable as BitmapDrawable).bitmap
        val finalCanvas = Canvas()
        finalCanvas.setBitmap(saveShareBitmap)
        finalCanvas.drawBitmap(bottom, 0.0f, 0.0f, null)
        finalCanvas.drawBitmap(bitmap!!, 0.0f, 0.0f, null)
        return saveShareBitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!(saveShareBitmap == null || saveShareBitmap!!.isRecycled)) {
            saveShareBitmap!!.recycle()
            saveShareBitmap = null
            System.gc()
        }
        if (!(bitmap == null || bitmap!!.isRecycled)) {
            bitmap!!.recycle()
            bitmap = null
            System.gc()
        }
        if (!(myCombinedBitmap == null || myCombinedBitmap!!.isRecycled)) {
            myCombinedBitmap!!.recycle()
            myCombinedBitmap = null
            System.gc()
        }
        if (!(colorized == null || colorized!!.isRecycled)) {
            colorized!!.recycle()
            colorized = null
            System.gc()
        }
        if (topImage != null && !topImage!!.isRecycled) {
            topImage!!.recycle()
            topImage = null
            System.gc()
        }
    }

    /* This method is call when we change progress of seekBar */
    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
        if (brushSize) {
            brushWidth = progress + 10
        }
        blrValue = progress
        val whatToSay: String = progress.toString()
        binding.textView.text = whatToSay

        val `val` = progress * (seekBar.width - 3 * seekBar.thumbOffset) / seekBar.max
        "$progress%".also { binding.textView.text = it }
        binding.textView.x = seekBar.x + `val` + seekBar.thumbOffset / 20

    }

    override fun onStartTrackingTouch(p0: SeekBar?) {

    }

    override fun onStopTrackingTouch(p0: SeekBar?) {
        if (blrValue == 0) {
            blrValue = 1
        }
        colorized = fastBlur(topImage!!, blrValue)!!.copy(Bitmap.Config.ARGB_8888, true)
        drawMode(1, 1)
    }


    /* This method is used for set default layout for buttons */
    private fun setDefaultLayoutForButtons() {
        binding.homeToolbarId.toolbarNameTv.text = getString(R.string.censor_image)
        binding.buttonLayouts.tvRemoveAudio.text = getString(R.string.auto_blur_faces)
        binding.buttonLayouts.tvAddDistortion.text = getString(R.string.brush)
        binding.buttonLayouts.ivRemoveAudio.setImageResource(R.drawable.ic_dot_unfilled)
        binding.buttonLayouts.ivRemoveAudio.setColorFilter(
            ContextCompat.getColor(this, R.color.grey_color),
            PorterDuff.Mode.SRC_IN
        )
        binding.buttonLayouts.ivAddDistortion.setImageResource(R.drawable.ic_brush)

    }

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    private fun listeners() {
        binding.ivZoom.setOnClickListener {
            if (!selectAndDeselectButtonCheck) {
                if (!isZoomRequired) {
                    isZoomRequired = true
                }
                selectAndDeselectButtonCheck = true
                binding.ivZoom.setCardBackgroundColor(
                    ContextCompat.getColor(
                        this, R.color.rippleEffect
                    )
                )
            } else {
                isZoomRequired = false
                selectAndDeselectButtonCheck = false
                binding.ivZoom.setCardBackgroundColor(
                    ContextCompat.getColor(
                        this, R.color.darkBlue
                    )
                )
            }

        }
        binding.homeToolbarId.ivBack.applyBoomEffect(true)
        binding.homeToolbarId.ivBack.setOnClickListener {
            exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                override fun onGoToHomeOk() {
                    onBackPressedDispatcher.onBackPressed() //with this line
                    exitPopup.dismiss()
                }

                override fun onGoToHomeCancel() {
                    exitPopup.dismiss()
                }
            }, "back")
            exitPopup.show(supportFragmentManager, "")
            exitPopup.isCancelable = true


        }

        binding.homeToolbarId.ivDownload.applyBoomEffect(true)
        binding.homeToolbarId.ivDownload.setOnClickListener {
            if (bitmap != null) saveImage(createFinalImage()!!) else saveImage(topImage!!)
        }

        binding.homeToolbarId.ivShare.applyBoomEffect(true)
        binding.homeToolbarId.ivShare.setOnClickListener {
            if (bitmap != null) shareImageAndText(createFinalImage()!!) else shareImageAndText(
                topImage!!
            )
        }
        binding.buttonLayouts.buttonRemoveAudio.applyBoomEffect(true)
        binding.buttonLayouts.buttonRemoveAudio.setOnClickListener {
            selectAndDeselectButtonCheck = false
            unableTouchBlur = false
            binding.seekLay.visibility = View.INVISIBLE
            resetButtonItems()
            activeBlurButton()
            if (bitmap != null) {
                analyzePhoto(createFinalImage()!!)
            } else {
                analyzePhoto(topImage!!)
            }
        }

        binding.buttonLayouts.buttonRemoveDistortion.applyBoomEffect(true)
        binding.buttonLayouts.buttonRemoveDistortion.setOnClickListener {
            if (!selectAndDeselectButtonCheck) {
                isZoomRequired = false
                selectAndDeselectButtonCheck = true
                unableTouchBlur = true
                binding.seekLay.viewVisible()
                if (!autoBlurActive) resetButtonItems()
                activeBrushBlurButton()

                if (bitmap != null) {
                    imageViewWidth = createFinalImage()!!.width
                    imageViewHeight = createFinalImage()!!.height
                    colorized = fastBlur(createFinalImage()!!, 70)
                    initFunction(createFinalImage()!!)
                } else {
                    imageViewWidth = topImage!!.width
                    imageViewHeight = topImage!!.height
                    colorized = fastBlur(topImage!!, 70)
                    initFunction(topImage!!)
                }


            } else {
                if (!autoBlurActive) resetButtonItems()
                else {
                    resetBrush()
                }
                isZoomRequired = true
                selectAndDeselectButtonCheck = false
                binding.seekLay.inVisible()
            }
        }

//        binding.ivUndo.applyBoomEffect(true)
        binding.ivUndo.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this@ImageEditActivity, R.anim.fadein)
            binding.ivUndo.startAnimation(animation)
            if (newBitmap != null) {
                if (!newBitmap!!.isRecycled) {
                    newBitmap!!.recycle()
                }
            }
            newBitmap = getUndoBitmap()
            setButtonsVisibility()
            binding.blurImage.setImageBitmapReset(newBitmap, true, null)
            binding.blurImage1.setImageBitmapReset(newBitmap, true, null)

            if (newBitmap != null) {
                imageViewWidth = newBitmap!!.width
                imageViewHeight = newBitmap!!.height
                colorized = fastBlur(newBitmap!!, 70)
                initFunction(newBitmap!!)
            } else {
                imageViewWidth = topImage!!.width
                imageViewHeight = topImage!!.height
                colorized = fastBlur(topImage!!, 70)
                initFunction(topImage!!)
            }
        }

        binding.ivRedo.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.fadein)
            binding.ivRedo.startAnimation(animation)
            enableShareAndSave()
            if (newBitmap != null) {
                if (!newBitmap!!.isRecycled) {
                    newBitmap!!.recycle()
                }
            }
            newBitmap = getRedoBitmap()
            setButtonsVisibility()

            binding.blurImage.setImageBitmapReset(newBitmap, true, null)
            binding.blurImage1.setImageBitmapReset(newBitmap, true, null)

            if (newBitmap != null) {
                imageViewWidth = newBitmap!!.width
                imageViewHeight = newBitmap!!.height
                colorized = fastBlur(newBitmap!!, 70)
                initFunction(newBitmap!!)
            } else {
                imageViewWidth = topImage!!.width
                imageViewHeight = topImage!!.height
                colorized = fastBlur(topImage!!, 70)
                initFunction(topImage!!)
            }
        }
    }

    private fun activeBrushBlurButton() {
        binding.buttonLayouts.buttonRemoveDistortion.background =
            ContextCompat.getDrawable(
                this@ImageEditActivity, R.drawable.ss_corner_round_light_blue
            )
        binding.buttonLayouts.ivAddDistortion.apply {
            setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_brush_filled
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
                this, R.color.white
            )
        )
    }

    private fun activeBlurButton() {
        binding.buttonLayouts.buttonRemoveAudio.background = ContextCompat.getDrawable(
            this@ImageEditActivity, R.drawable.ss_corner_round_light_blue
        )

        binding.buttonLayouts.ivRemoveAudio.apply {
            setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_dot_unfilled
                )
            )
            setColorFilter(ContextCompat.getColor(context, R.color.grey_color))
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
    }

    private fun setButtonsVisibility() {
        if (currentShowingIndex > 0) {
            binding.ivUndo.setImageResource(R.drawable.ic_undo_filled)
             binding.ivUndo.isEnabled = true
        } else {
             binding.ivUndo.setImageResource(R.drawable.ic_undo)
            binding.ivUndo.isEnabled = false
        }

        if (currentShowingIndex + 1 < bitmapsForUndo.size) {
            binding.ivRedo.setImageResource(R.drawable.ic_redo_filled)
            binding.ivRedo.isEnabled = true
        } else {
            binding.ivRedo.setImageResource(R.drawable.ic_redo)
            binding.ivRedo.isEnabled = false
        }

    }

    private fun resetButtonItems() {
        binding.buttonLayouts.buttonRemoveAudio.background =
            ContextCompat.getDrawable(this@ImageEditActivity, R.drawable.ss_corner_round_dark_blue)
        binding.buttonLayouts.buttonRemoveDistortion.background =
            ContextCompat.getDrawable(this@ImageEditActivity, R.drawable.ss_corner_round_dark_blue)
        binding.buttonLayouts.ivRemoveAudio.apply {
            setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_dot_unfilled
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
                    context, R.drawable.ic_brush
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

    private fun resetBrush() {
        binding.buttonLayouts.buttonRemoveDistortion.background =
            ContextCompat.getDrawable(this@ImageEditActivity, R.drawable.ss_corner_round_dark_blue)
        binding.buttonLayouts.ivAddDistortion.apply {
            setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_brush
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

    private fun resetAutoBlurButton() {
        binding.buttonLayouts.buttonRemoveAudio.background =
            ContextCompat.getDrawable(this@ImageEditActivity, R.drawable.ss_corner_round_dark_blue)

        binding.buttonLayouts.ivRemoveAudio.apply {
            setImageDrawable(
                ContextCompat.getDrawable(
                    context, R.drawable.ic_dot_unfilled
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
    }

    /*  Default Undo Redo Operations, Add bitmaps into list for further use */
    private fun addToUndoList() {
        try {
            recycleBitmapList(++currentShowingIndex)
            if (bitmap != null) {
                val undoRedoBitmap = createFinalImage()
                listForCheckActiveAutoBlur.add("brush")
                bitmapsForUndo.add(undoRedoBitmap!!.copy(undoRedoBitmap.config, true))
            } else {
                listForCheckActiveAutoBlur.add("brush")
                bitmapsForUndo.add(topImage!!.copy(topImage!!.config, true))
            }

        } catch (error: OutOfMemoryError) {
            bitmapsForUndo[1].recycle()
            bitmapsForUndo.removeAt(1)
            bitmapsForUndo.add(newBitmap!!.copy(newBitmap!!.config, true))
        }
    }

    /* That function will recycler bitmap on the case of use undo and add some new operation */

    private fun recycleBitmapList(fromIndex: Int) {
        while (fromIndex < bitmapsForUndo.size) {
            bitmapsForUndo[fromIndex].recycle()
            bitmapsForUndo.removeAt(fromIndex)
        }
        while (fromIndex < listForCheckActiveAutoBlur.size) {
            listForCheckActiveAutoBlur.removeAt(fromIndex)
        }
    }


    /* This method is used for get undo images after clicking undo button */
    private fun getUndoBitmap(): Bitmap? {
        if (currentShowingIndex - 1 >= 0) currentShowingIndex -= 1 else currentShowingIndex = 0

        lifecycleScope.launch {
            for (i in listForCheckActiveAutoBlur.indices) {
                if (i == currentShowingIndex + 1) {
                    if (listForCheckActiveAutoBlur[i] == "autoBlur") {
                        resetAutoBlurButton()
                        isZoomRequired = false
                        binding.buttonLayouts.buttonRemoveAudio.isEnabled = true

                    }
                }
            }
        }
        if (currentShowingIndex == 0) {
            binding.buttonLayouts.buttonRemoveAudio.isEnabled = true
            disableShareAndSave()
            if (autoBlurActive) resetAutoBlurButton()
        }

        return bitmapsForUndo[currentShowingIndex].copy(
            bitmapsForUndo[currentShowingIndex].config, true
        )
    }

    /* This method is used for get redo images after clicking redo button */
    private fun getRedoBitmap(): Bitmap? {
        if (currentShowingIndex + 1 < bitmapsForUndo.size) currentShowingIndex += 1 else currentShowingIndex =
            bitmapsForUndo.size - 1

        lifecycleScope.launch {
            for (i in listForCheckActiveAutoBlur.indices) {
                if (i == currentShowingIndex) {
                    if (listForCheckActiveAutoBlur[i] == "autoBlur") {
                        activeBlurButton()
                        binding.buttonLayouts.buttonRemoveAudio.isEnabled = false
                        isZoomRequired = false
                    }
                }
            }
        }

        if (currentShowingIndex == bitmapsForUndo.size - 1) {
            if (unableTouchBlur) binding.seekLay.viewVisible()
            else binding.seekLay.inVisible()
        }

        return bitmapsForUndo[currentShowingIndex].copy(
            bitmapsForUndo[currentShowingIndex].config,
            true
        )
    }

    /*Add to undo list for auto face detection blur*/
    private fun addToUndoListTwo(mBitmap: Bitmap) {
        try {
            recycleBitmapList(++currentShowingIndex)
            listForCheckActiveAutoBlur.add("autoBlur")
            bitmapsForUndo.add(mBitmap.copy(mBitmap.config, true))
        } catch (error: OutOfMemoryError) {
            bitmapsForUndo[1].recycle()
            bitmapsForUndo.removeAt(1)
            bitmapsForUndo.add(mBitmap.copy(mBitmap.config, true))
        }
    }

    private fun enableShareAndSave() {
        binding.homeToolbarId.ivDownload.isEnabled = true
        binding.homeToolbarId.ivDownload.alpha = 1f
        binding.homeToolbarId.ivShare.isEnabled = true
        binding.homeToolbarId.ivShare.alpha = 1f
    }

    private fun disableShareAndSave() {
        binding.homeToolbarId.ivDownload.isEnabled = false
        binding.homeToolbarId.ivDownload.alpha = 0.5f
        binding.homeToolbarId.ivShare.isEnabled = false
        binding.homeToolbarId.ivShare.alpha = 0.5f
    }

    /* This function is used for save final image*/
    private fun saveImage(myBitmap: Bitmap): String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val imageDirectory: File = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    .toString() + Constants.IMAGE_DIRECTORY
            )
        } else {
            File(
                Environment.getExternalStorageDirectory().toString() + Constants.IMAGE_DIRECTORY
            )
        }
        // have the object build the directory structure, if needed.
        if (!imageDirectory.exists()) {
            imageDirectory.mkdirs()
        }
        try {
            val f = File(
                imageDirectory,
                CENSOR_X + Calendar.getInstance().timeInMillis.toString() + ".jpg"
            )
            if (!f.parentFile.exists()) f.parentFile?.mkdirs()
            if (!f.exists()) f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(
                applicationContext, arrayOf(f.path), arrayOf("image/jpeg"), null
            )

            fo.close()
            val popup = MaterialDialogHelper("ImageSave")
            popup.show(supportFragmentManager, "")
            popup.isCancelable = true
            lifecycleScope.launch {
                delay(2000)
                popup.dismiss()
            }

            disableShareAndSave()

            return f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        return ""
    }

    override fun onBackPressed() {
        exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
            override fun onGoToHomeOk() {
                onBackPressedDispatcher.onBackPressed() //with this line
                exitPopup.dismiss()
            }

            override fun onGoToHomeCancel() {
                exitPopup.dismiss()
            }
        }, "back")
        exitPopup.show(supportFragmentManager, "")
        exitPopup.isCancelable = true
    }
}