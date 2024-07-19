package com.censorchi.views.activities

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.censorchi.databinding.ActivityGetStartedBinding
import com.censorchi.utils.applyBoomEffect


class GetStartedActivity : BaseActivity() {
    private lateinit var binding: ActivityGetStartedBinding
    private var doubleBackToExitPressedOnce = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGetStartedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setStatusBar()

        binding.btnGetStarted.applyBoomEffect(true)
        binding.btnGetStarted.setOnClickListener {
            startActivity(Intent(this@GetStartedActivity, AllowAccessActivity::class.java))
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?) = if (keyCode == KeyEvent.KEYCODE_BACK) {
        doubleBackToExitPressedOnce = if (doubleBackToExitPressedOnce) {
            moveTaskToBack(true)
            false
        } else {
            Toast.makeText(this, "Press again to exit", Toast.LENGTH_SHORT).show()
            true
        }
        true
    } else false

    override fun onBackPressed() = Unit


}