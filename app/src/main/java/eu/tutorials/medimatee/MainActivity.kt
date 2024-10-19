package eu.tutorials.medimatee

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {
    lateinit var db: FirebaseFirestore
    lateinit var inputHardwareId: EditText
    lateinit var inputPhone: EditText
    lateinit var checkButton: Button
    lateinit var logo: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        enableEdgeToEdge()
        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        Log.d("MainActivity", "Firestore initialized")

        // UI elements
        inputHardwareId = findViewById(R.id.inputHardwareId)
        checkButton = findViewById(R.id.checkButton)
        logo = findViewById(R.id.imageView)
        inputPhone=findViewById(R.id.inputPhone)
        // Button click listener to check hardware UUID
        checkButton.setOnClickListener {
            val hardwareUuid = "Generated Short UUID: " + inputHardwareId.text.toString().trim()
            Log.d("MainActivity", "Entered Hardware UUID: $hardwareUuid")

            if (hardwareUuid.isNotEmpty()) {
                checkIfHardwareExists(hardwareUuid)
            } else {
                Toast.makeText(this, "Please enter a hardware ID", Toast.LENGTH_SHORT).show()
                Log.d("MainActivity", "Hardware ID is empty")
            }

        }
    }

    private fun checkIfHardwareExists(hardwareUuid: String) {
        // Get all user collections (user_1, user_2, etc.)
        val userCollectionRefs = (1..100).map { "user_$it" }
        Log.d("MainActivity", "Checking collections: $userCollectionRefs")

        var exists = false
        var collectionNameWithUuid: String? = null
        var completedRequests = 0

        // Check each user collection
        for (collectionName in userCollectionRefs) {
            db.collection(collectionName).get().addOnSuccessListener { documents ->
                for (document in documents) {
                    val uuid = document.getString("hardware_uuid")

                    if (uuid == hardwareUuid) {
                        exists = true
                        collectionNameWithUuid = collectionName
                        storeHardwareUuidLocally(hardwareUuid, collectionNameWithUuid!!)

                        // Update only specific fields in the document
                        val documentId = document.id // Get the document ID
                        val updateData = mapOf(
                            "phone_number" to inputPhone.text.toString().trim() // Store phone number
                        )

                        // Update the document without deleting other fields
                        db.collection(collectionName)
                            .document(documentId)
                            .update(updateData)
                            .addOnSuccessListener {
                                Log.d("MainActivity", "Document successfully updated")
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainActivity", "Error updating document: ${e.message}")
                            }

                        break
                    }
                }
                completedRequests++

                // If all requests are complete, show a message
                if (completedRequests == userCollectionRefs.size) {
                    if (exists) {
                        Toast.makeText(this, "Hardware ID exists: $hardwareUuid", Toast.LENGTH_SHORT).show()
                        navigateToSchedulingPage()
                    } else {
                        Toast.makeText(this, "Hardware ID does not exist", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching collections: ${exception.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "Error fetching collections: ${exception.message}")
            }

            // Break the outer loop if a match was found
            if (exists) break
        }
    }



    // Function to store both hardware UUID and collection name in SharedPreferences
    private fun storeHardwareUuidLocally(hardwareUuid: String, collectionName: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("hardware_uuid", hardwareUuid)
            putString("collection_name", collectionName)
            apply()
        }
        Log.d("MainActivity", "Stored Hardware UUID and collection name: $hardwareUuid in $collectionName")
    }

    // Function to navigate to the Scheduling activity
    private fun navigateToSchedulingPage() {
        val intent = Intent(this, CameraPreview::class.java)
        startActivity(intent)
        finish() // Optional: To close the current MainActivity so the user can't return using the back button
        Log.d("MainActivity", "Navigating to Scheduling page")
    }
}
