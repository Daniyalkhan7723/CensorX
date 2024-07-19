package com.censorchi.views.popUp

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.censorchi.R
import com.censorchi.databinding.CustomProgressbarBinding
import com.censorchi.utils.viewGone
import com.censorchi.utils.viewVisible

class AskPermsiionDialogue(var type:String)  : DialogFragment() {
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

        if (type=="ImageSave"){
            binding.ivDone.viewVisible()
            binding.progressBar.viewGone()
            binding.tv.text="Image Saved"
        }
        if (type=="VideoSave"){
            binding.ivDone.viewVisible()
            binding.progressBar.viewGone()
            binding.tv.text="Video Saved"
        }
        else if (type=="videoProcessing"){
            binding.ivDone.viewGone()
            binding.progressBar.viewVisible()
            binding.tv.text="Processing Video"

        }
        else if (type=="imageProcessing"){
            binding.ivDone.viewGone()
            binding.progressBar.viewVisible()
            binding.tv.text="Processing Image"

        }
        else if (type=="ProcessingComplete"){
            binding.ivDone.viewVisible()
            binding.progressBar.viewGone()
            binding.tv.text="Processing Complete"

        }


        return view
    }
    override fun onResume() {
        val window: Window? = dialog!!.window
        val size = Point()
        // Store dimensions of the screen in `size`
        // Store dimensions of the screen in `size`
        val display: Display = window!!.windowManager.defaultDisplay
        display.getSize(size)
        // Set the width of the dialog proportional to 75% of the screen width
        // Set the width of the dialog proportional to 75% of the screen width
        window.setLayout((size.x * 0.90).toInt(), WindowManager.LayoutParams.WRAP_CONTENT)
        window.setGravity(Gravity.CENTER)
        // Call super onResume after sizing
        // Call super onResume after sizing
        super.onResume()
    }

}