package com.censorchi.views.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.censorchi.databinding.ActivityAllowAccessBinding
import com.censorchi.utils.sharedPreference.AppStorage.getIsBack
import com.censorchi.utils.sharedPreference.AppStorage.setGetStarted
import com.censorchi.utils.sharedPreference.AppStorage.setIsBack
import com.censorchi.utils.applyBoomEffect
import com.censorchi.views.popUp.ExitDialogue

class AllowAccessActivity : BaseActivity() {
    private lateinit var binding: ActivityAllowAccessBinding
    lateinit var exitPopup: ExitDialogue

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAllowAccessBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            buttonAllow.apply {
                applyBoomEffect()
                setOnClickListener {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        checkAndRequestPermission(
                            arrayOf(
                                Manifest.permission.READ_MEDIA_IMAGES,
                                Manifest.permission.READ_MEDIA_VIDEO,
                                Manifest.permission.READ_MEDIA_AUDIO,

                                )
                        ) { permissionResults ->
                            if (permissionResults.none { it.value != PackageManager.PERMISSION_GRANTED }) {
                                val intent = Intent(applicationContext, HomeActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(intent)
                                finishAffinity()
                                setGetStarted(true)
                            } else {
                                setGetStarted(false)
                                if (getIsBack()) {
                                    exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                                        override fun onGoToHomeOk() {
                                            startActivity(Intent().apply {
                                                action =
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                                data = Uri.fromParts("package", packageName, null)
                                            })
                                            exitPopup.dismiss()
                                        }

                                        override fun onGoToHomeCancel() {
                                            exitPopup.dismiss()
                                        }

                                    }, "permission")
                                    exitPopup.show(supportFragmentManager, "")
                                    exitPopup.isCancelable = true
                                } else {
                                    setIsBack(true)
                                    finish()
                                }
                            }
                        }
                    } else {
                        checkAndRequestPermission(
                            arrayOf(
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                        ) { permissionResults ->
                            if (permissionResults.none { it.value != PackageManager.PERMISSION_GRANTED }) {
                                val intent = Intent(applicationContext, HomeActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                startActivity(intent)
                                finishAffinity()
                                setGetStarted(true)
                            } else {
                                setGetStarted(false)
                                if (getIsBack()) {
                                    exitPopup = ExitDialogue(object : ExitDialogue.GoToHome {
                                        override fun onGoToHomeOk() {
                                            startActivity(Intent().apply {
                                                action =
                                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                                data = Uri.fromParts("package", packageName, null)
                                            })
                                            exitPopup.dismiss()
                                        }

                                        override fun onGoToHomeCancel() {
                                            exitPopup.dismiss()
                                        }

                                    }, "permission")
                                    exitPopup.show(supportFragmentManager, "")
                                    exitPopup.isCancelable = true
                                } else {
                                    setIsBack(true)
                                    finish()
                                }
                            }
                        }
                    }

                }
            }

            dontAllow.apply {
                applyBoomEffect()
                setOnClickListener {
                    finish()
                }
            }
        }
        setStatusBar()
    }
}