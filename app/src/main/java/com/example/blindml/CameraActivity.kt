package com.example.blindml

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class CameraActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.camera_fragment_container, CameraFragment())
                .commit()
        }
    }

    fun sendImageToMainActivity(imageBytes: ByteArray) {
        val resultIntent = Intent().apply {
            putExtra("image_bytes", imageBytes)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }
}