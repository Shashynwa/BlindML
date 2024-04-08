package com.example.blindml

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_IMAGE_PICK = 2

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var speechRecognizerIntent: Intent

    // Declare OkHttpClient as a property
    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize SpeechRecognizer and Intent
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, this.packageName)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {}

            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (matches != null) {
                    for (result in matches) {
                        if (result.equals("capture image", ignoreCase = true)) {
                            dispatchTakePictureIntent()
                            break
                        } else if (result.equals("choose image", ignoreCase = true)) {
                            chooseImageFromGallery()
                            break
                        }
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        // Start listening for speech
        speechRecognizer.startListening(speechRecognizerIntent)

        // Set click listeners for buttons
        findViewById<Button>(R.id.buttonCapture).setOnClickListener {
            // Check camera permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Request camera permission if not granted
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.CAMERA),
                    REQUEST_IMAGE_CAPTURE
                )
            } else {
                // Start the camera intent
                dispatchTakePictureIntent()
            }
        }

        findViewById<Button>(R.id.buttonChoose).setOnClickListener {
            // Start the activity to choose image from gallery
            chooseImageFromGallery()
        }
    }

    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
        }
    }

    private fun chooseImageFromGallery() {
        val pickPhotoIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickPhotoIntent, REQUEST_IMAGE_PICK)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start camera intent
                dispatchTakePictureIntent()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Image captured successfully, display it
            val imageBitmap = data?.extras?.get("data") as Bitmap

            // Scale the bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 800, 800 * imageBitmap.height / imageBitmap.width, false)

            findViewById<ImageView>(R.id.imageView).setImageBitmap(scaledBitmap)

            // Convert the Bitmap to ByteArray
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val imageBytes = stream.toByteArray()

            // Display a Toast message
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

            // Send the image to the server
            sendImageToServer(imageBytes)
        } else if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK) {
            // Image picked successfully, display it
            val imageUri = data?.data as Uri
            findViewById<ImageView>(R.id.imageView).setImageURI(imageUri)

            // Convert the picked image to Bitmap
            val imageBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)

            // Scale the bitmap
            val scaledBitmap = Bitmap.createScaledBitmap(imageBitmap, 800, 800 * imageBitmap.height / imageBitmap.width, false)

            // Convert the Bitmap to ByteArray
            val stream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.PNG, 90, stream)
            val imageBytes = stream.toByteArray()

            // Display a Toast message
            Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()

            // Send the image to the server
            sendImageToServer(imageBytes)
        }
    }

    private fun sendImageToServer(imageBytes: ByteArray) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", "image.png",
                        imageBytes.toRequestBody("image/png".toMediaTypeOrNull(), 0, imageBytes.size))
                    .build()

                // Replace with the correct server URL
                val serverUrl = "http://192.168.203.117:5000/upload"

                val request = Request.Builder()
                    .url(serverUrl)
                    .post(requestBody)
                    .build()

                // Use the client property for the network request
                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                        e.printStackTrace()
                        Log.e("MainActivity", "Upload failed", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        lifecycleScope.launch(Dispatchers.IO) {
                            val message = response.body?.string()
                            withContext(Dispatchers.Main) {
                                if (!response.isSuccessful) {
                                    Toast.makeText(this@MainActivity, "Upload failed: ${response.code}", Toast.LENGTH_SHORT).show()
                                } else {
                                    if (message != null) {
                                        // Display the message in a Toast
                                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@MainActivity, "Upload succeeded, but no response message", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            Log.d("MainActivity", "Server response: $message")
                        }
                    }
                })
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Don't forget to free up resources used by SpeechRecognizer
        speechRecognizer.destroy()
    }
}