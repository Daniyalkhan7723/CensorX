package com.censorchi.views.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.censorchi.R
import com.censorchi.databinding.ActivityHomeBinding
import com.censorchi.utils.Constants
import com.censorchi.utils.Constants.IMAGE
import com.censorchi.utils.Constants.REQUEST_CODE_PICK_IMAGE
import com.censorchi.utils.Constants.VIDEO

class HomeActivity : BaseActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var doubleBackToExitPressedOnce = false

    @SuppressLint("SetTextI18n", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fullScreenWithStatusBarWhiteIcon()

        try {
            val versionName: String = packageManager
                .getPackageInfo(packageName, 0).versionName
            "App version $versionName".also {
                binding.tvAppVersion.text = it
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        binding.ivTakeImage.setOnTouchListener { _, motionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.ivTakeImage.background =
                        ContextCompat.getDrawable(
                            this@HomeActivity,
                            R.drawable.background_round_ripple
                        )
                }
                MotionEvent.ACTION_UP -> {
                    binding.ivTakeImage.background =
                        ContextCompat.getDrawable(
                            this@HomeActivity,
                            R.drawable.backgroundround
                        )

                    checkAndRequestPermission(
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                    ) { permissionResults ->
                        if (permissionResults.none { it.value != PackageManager.PERMISSION_GRANTED }) {
                            val photoPickerIntent = Intent(Intent.ACTION_PICK)
                            photoPickerIntent.type = "image/* video/*"
                            startActivityForResult(
                                photoPickerIntent,
                                REQUEST_CODE_PICK_IMAGE
                            )//                    selectImageFromGallery()
                        } else {
                            showAlertDialog()
                        }
                    }

                }
                MotionEvent.ACTION_CANCEL -> {
                    ContextCompat.getDrawable(
                        this@HomeActivity,
                        R.drawable.backgroundround
                    )

                }
            }
            true

        }

    }


    override fun onResume() {
        super.onResume()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            startActivity(Intent(this@HomeActivity, GetStartedActivity::class.java))
            finish()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_PICK_IMAGE -> if (resultCode == RESULT_OK) {
                try {
                    val uri = data?.data
                    val a = getRealPathFromURI(this@HomeActivity, uri)
                    if (uri.toString().contains(IMAGE)) {
                        sendBroadcast(
                            Intent(
                                Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse(
                                    "file://$a"
                                )
                            )
                        )
                        val intent = Intent(this, ImageEditActivity::class.java)
                        intent.putExtra(Constants.IMAGE_PATH, a)
                        startActivity(intent)

                    } else if (uri.toString().contains(VIDEO)) {
                        val intent = Intent(this, VideoEditActivity::class.java)
                        intent.putExtra(Constants.VIDEO_PATH, uri.toString())
                        startActivity(intent)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

    }

    private fun getRealPathFromURI(context: Context, contentUri: Uri?): String? {
        var cursor: Cursor? = null
        return try {
            val proj = arrayOf(MediaStore.Images.Media.DATA)
            cursor = context.contentResolver.query(contentUri!!, proj, null, null, null)
            val columnIndex = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            cursor.run {
                moveToFirst()
                getString(columnIndex)
            }
        } finally {
            cursor?.close()
        }
    }

    private fun showAlertDialog() {
        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this@HomeActivity).apply {
            setTitle(getString(R.string.permission_required))
            setMessage(getString(R.string.please_allow))
        }
        alertDialogBuilder.setPositiveButton(
            getString(R.string.yes)
        ) { _, _ ->
            startActivity(Intent().apply {
                action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                data = Uri.fromParts("package", packageName, null)
            })
        }
        alertDialogBuilder.setNegativeButton(
            getString(R.string.no)
        ) { _, _ -> finish() }
        val alertDialog = alertDialogBuilder.create()
        alertDialog.show()
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