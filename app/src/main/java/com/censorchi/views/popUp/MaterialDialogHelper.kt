package com.censorchi.views.popUp

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.view.*
import androidx.fragment.app.DialogFragment
import com.censorchi.R
import com.censorchi.databinding.CustomProgressbarBinding
import com.censorchi.utils.viewGone
import com.censorchi.utils.viewVisible

class MaterialDialogHelper(var type: String) : DialogFragment() {
    private lateinit var binding: CustomProgressbarBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog!!.window!!.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        dialog?.setCanceledOnTouchOutside(false)
    }

    override fun onStart() {
        super.onStart()
        requireDialog().window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = layoutInflater.inflate(R.layout.custom_progressbar, container, false)
        binding = CustomProgressbarBinding.bind(view)

        when (type) {
            "ImageSave" -> {
                binding.ivDone.viewVisible()
                binding.progressBar.viewGone()
                binding.tv.text = getString(R.string.image_saved)
            }
            "VideoSave" -> {
                binding.ivDone.viewVisible()
                binding.progressBar.viewGone()
                binding.tv.text = getString(R.string.video_saved)
            }
            "videoProcessing" -> {
                binding.ivDone.viewGone()
                binding.progressBar.viewVisible()
                binding.tv.text = getString(R.string.processing_video)
            }
            "imageProcessing" -> {
                binding.ivDone.viewGone()
                binding.progressBar.viewVisible()
                binding.tv.text = getString(R.string.processing_image)

            }
            "ProcessingComplete" -> {
                binding.ivDone.viewVisible()
                binding.progressBar.viewGone()
                binding.tv.text = getString(R.string.processing_complete)

            }
            else -> {
                //Class variables
                //Class variables
                binding.ivDone.viewGone()
                binding.progressBar.viewVisible()
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
                            binding.progressBar.progress = progressBarStatus
                            "Exporting Video ${progressBarStatus}%".also { binding.tv.text = it }

                        })
                    }
                }.start()


            }
        }


        return view
    }

    override fun onResume() {
        val window: Window? = dialog!!.window
        val size = Point()
        // Store dimensions of the screen in `size`
        // Store dimensions of the screen in `size`
        val display: Display = window!!.windowManager.defaultDisplay.also {
            it.getSize(size)
        }
        // Set the width of the dialog proportional to 75% of the screen width
        // Set the width of the dialog proportional to 75% of the screen width
        window.setLayout((size.x * 0.90).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        // Call super onResume after sizing
        // Call super onResume after sizing
        super.onResume()
    }

}