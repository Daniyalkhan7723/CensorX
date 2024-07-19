package com.censorchi.views.activities

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.lifecycle.lifecycleScope
import com.censorchi.R
import com.censorchi.databinding.ActivitySplashBinding
import com.censorchi.utils.sharedPreference.AppStorage
import com.censorchi.utils.sharedPreference.AppStorage.getGetStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {
    lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        clearSession()
        fullScreenWithStatusBarWhiteIcon()
        binding.tvCensorX.animation = AnimationUtils.loadAnimation(this, R.anim.top_animation)
        binding.tvBlurFaces.animation = AnimationUtils.loadAnimation(this, R.anim.top_animation)


        lifecycleScope.launch {
            delay(4000)
            nextScreen()

        }
    }

    private fun clearSession() {
        AppStorage.clearSessionTopBlur()
        AppStorage.clearSessionBottomBlur()
        AppStorage.clearSessionFullBlur()
    }

    private fun nextScreen() {
        if (!getGetStarted()){
            startActivity(Intent(this@SplashActivity, GetStartedActivity::class.java))
            finish()
        }else{
            startActivity(Intent(this@SplashActivity, HomeActivity::class.java))
            finish()
        }

    }
}