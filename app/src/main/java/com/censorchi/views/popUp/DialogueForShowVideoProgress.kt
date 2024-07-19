package com.censorchi.views.popUp

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.censorchi.R
import com.censorchi.databinding.DialogueForVideoProgressBinding

class DialogueForShowVideoProgress() : DialogFragment() {
    private lateinit var binding: DialogueForVideoProgressBinding

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
        val view = layoutInflater.inflate(R.layout.dialogue_for_video_progress, container, false)
        binding = DialogueForVideoProgressBinding.bind(view)

        return view
    }

    fun showProgress(progresss: Float) {
        val progress = "Processing Video ${progresss.toLong()}%"
        binding.progressBar.progress = progresss.toInt()
        binding.tv.text = progress
    }

    fun showProgressForBlur(progresss: Float) {
        if (progresss <= 100.0) {
            val progress = "Processing Video ${progresss.toLong()}%"
            binding.progressBar.progress = progresss.toInt()
            binding.tv.text = progress
        }
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