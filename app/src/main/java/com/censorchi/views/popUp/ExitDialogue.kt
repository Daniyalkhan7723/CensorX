package com.censorchi.views.popUp

import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import androidx.fragment.app.DialogFragment
import com.censorchi.R
import com.censorchi.databinding.ExitDialogueBinding
import com.censorchi.utils.applyBoomEffect

class ExitDialogue(
    private var goToLoginListener: GoToHome, var type: String
) : DialogFragment() {
    private lateinit var binding: ExitDialogueBinding

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
        val view = layoutInflater.inflate(R.layout.exit_dialogue, container, false)
        binding = ExitDialogueBinding.bind(view)


        if (type == "permission") {
            binding.tvTitle.text = "Permission Required"
            binding.tvDes.text =
                "Please allow access to your photos library to pick pictures and videos that needs to be blurred out"
            binding.btnYes.text = "Open Settings"
            binding.btnNo.text = "Cancel"
        }
        binding.btnYes.applyBoomEffect(true)
        binding.btnYes.setOnClickListener {
            goToLoginListener.onGoToHomeOk()
        }
        binding.btnNo.applyBoomEffect(true)
        binding.btnNo.setOnClickListener {
            goToLoginListener.onGoToHomeCancel()
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

    interface GoToHome {
        fun onGoToHomeOk()
        fun onGoToHomeCancel()
    }

}