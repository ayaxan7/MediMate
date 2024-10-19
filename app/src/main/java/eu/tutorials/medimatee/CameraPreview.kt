package eu.tutorials.medimatee

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import android.widget.Toast
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class CameraPreview : AppCompatActivity() {

    private val cameraRequestCode = 1001
    private lateinit var imageUri: Uri
    private lateinit var capturedImageView: ImageView
    private lateinit var scannedTextView: TextView
    private lateinit var progressBar: ProgressBar

    // Tag for logs
    private val TAG = "CameraPreview"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        capturedImageView = findViewById(R.id.capturedImageView)
        scannedTextView = findViewById(R.id.scannedTextView)
        progressBar = findViewById(R.id.progressBar)
        val openCameraButton: Button = findViewById(R.id.openCameraButton)

        // Open camera when the button is clicked
        openCameraButton.setOnClickListener {
            openCamera()
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            // Create a file to store the image
            val photoFile = try {
                createImageFile()
            } catch (ex: Exception) {
                // Handle error and log it
                Log.e(TAG, "Error creating image file: ${ex.message}")
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
                return
            }

            if (photoFile != null) {
                Log.d(TAG, "Photo file created successfully: ${photoFile.absolutePath}")
                imageUri = FileProvider.getUriForFile(this, "${packageName}.fileprovider", photoFile)
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(intent, cameraRequestCode)
            }
        } else {
            Log.e(TAG, "No camera app found on the device.")
            Toast.makeText(this, "No Camera app found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        Log.d(TAG, "Creating an image file...")
        // Check if external storage is available
        val state = Environment.getExternalStorageState()
        if (state != Environment.MEDIA_MOUNTED) {
            throw Exception("External storage not available")
        }

        // Create an image file name with a unique timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
        Log.d(TAG, "Image file created: ${imageFile.absolutePath}")
        return imageFile
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        Log.d(TAG, "onActivityResult: RequestCode = $requestCode, ResultCode = $resultCode")

        if (requestCode == cameraRequestCode && resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Image captured successfully.")

            // Use contentResolver to get the image properly
            try {
                val inputStream = contentResolver.openInputStream(imageUri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                capturedImageView.setImageBitmap(bitmap)
                Log.d(TAG, "Image displayed in ImageView.")

                // Show the progress bar before text recognition
                progressBar.visibility = ProgressBar.VISIBLE

                // Process image for text recognition
                val image = InputImage.fromFilePath(this, imageUri)
                recognizeTextFromImage(image)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding image: ${e.message}")
                Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
            }

        } else {
            Log.w(TAG, "Image capture cancelled or failed.")
            Toast.makeText(this, "Image capture cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to recognize text using ML Kit
    private fun recognizeTextFromImage(image: InputImage) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                // Hide the progress bar after success
                progressBar.visibility = ProgressBar.GONE

                // Display the recognized text
                displayTextFromImage(visionText)

                // Show success toast
                Toast.makeText(this, "Text recognized successfully!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Hide the progress bar if failed
                progressBar.visibility = ProgressBar.GONE

                // Log and show failure toast
                Log.e(TAG, "Text recognition failed: ${e.message}")
                Toast.makeText(this, "Failed to recognize text", Toast.LENGTH_SHORT).show()
            }
    }

    // Function to display recognized text
// Function to display recognized text
    private fun displayTextFromImage(visionText: Text) {
        if (visionText.text.isNotEmpty()) {
            Log.d(TAG, "Text recognized: ${visionText.text}")

            // Apply some styling to the recognized text
            scannedTextView.text = visionText.text
            scannedTextView.visibility = TextView.VISIBLE
            scannedTextView.setTextColor(getColor(R.color.black)) // Change text color
            scannedTextView.setBackgroundColor(getColor(R.color.light_blue)) // Change background color
            scannedTextView.textSize = 18f // Change text size
            scannedTextView.setPadding(16, 16, 16, 16) // Add padding

            // Optional: You can also apply a different font if needed
            // scannedTextView.typeface = ResourcesCompat.getFont(this, R.font.your_custom_font)

            // Optionally, add animations
            scannedTextView.animate().alpha(1f).setDuration(300).start()
        } else {
            Log.d(TAG, "No text found in the image.")
            scannedTextView.text = "No text found"
            scannedTextView.setTextColor(getColor(R.color.red)) // Use a different color for "no text"
            scannedTextView.visibility = TextView.VISIBLE
        }
    }

}
