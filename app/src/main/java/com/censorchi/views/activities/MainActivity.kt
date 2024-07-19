package com.censorchi.views.activities

import android.app.Activity
import android.app.Dialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import com.censorchi.R
import com.iceteck.silicompressorr.SiliCompressor
import java.io.File
import java.net.URISyntaxException


class MainActivity : AppCompatActivity() {

    // Initialize variable
    var btSelect: Button? = null
    var videoView1: VideoView? = null
    var videoView2: VideoView? = null
    var textView1: TextView? = null
    var textView2: TextView? = null
    var textView3: TextView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Assign variable
        btSelect = findViewById(R.id.bt_select);
        videoView1 = findViewById(R.id.video_view1);
        videoView2 = findViewById(R.id.Video_view2);
        textView1 = findViewById(R.id.TextView1);
        textView2 = findViewById(R.id.text_view2);
        textView3 = findViewById(R.id.TextView3);

        btSelect!!.setOnClickListener {
            selectVideo()
        }


    }

    private fun selectVideo() {
        // Initialize intent
        val intent = Intent(Intent.ACTION_PICK)
        // Set type
        intent.setType("video/*")
        // set action
        intent.setAction(Intent.ACTION_GET_CONTENT)
        // Start activity result
        startActivityForResult(
            Intent.createChooser(intent, "Select Video"),
            100
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode, permissions, grantResults
        )
        // check condition
        if (requestCode == 1 && grantResults.size > 0 && (grantResults[0]
                    == PackageManager.PERMISSION_GRANTED)
        ) {
            // When permission is granted
            // Call method
            selectVideo()
        } else {
            // When permission is denied
            // Display Toast
            Toast
                .makeText(
                    applicationContext,
                    "Permission Denied !",
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        @Nullable data: Intent?
    ) {
        super.onActivityResult(
            requestCode, resultCode,
            data
        )
        // Check condition
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            // When result is ok
            // Initialize Uri
            val uri: Uri? = data.data
            // Set video uri
            videoView1!!.setVideoURI(uri)
            // Initialize file
            val file = File(
                Environment.getExternalStorageDirectory()
                    .getAbsolutePath()
            )
            // Create compress video method
            CompressVideo().execute("false", uri.toString(), file.path)
        }
    }


    private inner class CompressVideo : AsyncTask<String?, String?, String?>() {
        // Initialize dialog
        var dialog: Dialog? = null
        protected override fun onPreExecute() {
            super.onPreExecute()
            dialog = ProgressDialog.show(
                this@MainActivity, "", "Compressing...")
            // Display dialog
        }

        protected override fun doInBackground(vararg strings: String?): String? {
            // Initialize video path
            var videoPath: String? = null
            try {
                // Initialize uri
                val uri = Uri.parse(strings[1])
                // Compress video
                videoPath = SiliCompressor.with(this@MainActivity)
                    .compressVideo(uri, strings[2])
            } catch (e: URISyntaxException) {
                e.printStackTrace()
            }
            // Return Video path
            return videoPath
        }

        protected override fun onPostExecute(s: String?) {
            super.onPostExecute(s)
            // Dismiss dialog
            // Dismiss dialog
            dialog?.dismiss()
            // Visible all views
            videoView1?.visibility = View.VISIBLE
            textView1?.visibility = View.VISIBLE
            videoView2?.visibility = View.VISIBLE
            textView2?.visibility = View.VISIBLE
            textView3?.visibility = View.VISIBLE

            // Initialize file
            val file = File(s)
            // Initialize uri
            val uri = Uri.fromFile(file)
            // set video uri
            videoView2?.setVideoURI(uri)

            // start both video
            videoView1?.start()
            videoView2?.start()

            // Compress video size
            val size = file.length() / 1024f
            // Set size on text view
            textView3?.text = String.format("Size : %.2f KB", size)
        }
    }

}