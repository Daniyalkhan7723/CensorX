package com.censorchi.views.popUp

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.censorchi.R
import com.censorchi.databinding.CameraGalleryPopupBinding
import com.censorchi.views.callBacks.OpenGalleryCallBack


class CameraGalleryPopup(private var listener: OpenGalleryCallBack) : DialogFragment() {

    private lateinit var binding: CameraGalleryPopupBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireDialog().window?.setBackgroundDrawableResource(R.drawable.popup_background)
        dialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.setCanceledOnTouchOutside(true)
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
        val view = layoutInflater.inflate(R.layout.camera_gallery_popup, container, false)
        binding = CameraGalleryPopupBinding.bind(view)

        binding.tvCancel.setOnClickListener {
            dismiss()
        }

        binding.layoutGallery.setOnClickListener {
            listener.onOpenGallery()
            dismiss()
        }
        binding.layoutCamera.setOnClickListener {
            listener.onOpenCamera()
            dismiss()
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