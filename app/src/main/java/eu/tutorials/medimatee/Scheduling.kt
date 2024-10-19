package eu.tutorials.medimatee

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.graphics.Color
import android.text.Spannable

import android.text.style.ForegroundColorSpan

import java.util.Calendar
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore



class Scheduling : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var timeButton1: Button
    private lateinit var timeButton2: Button
    private lateinit var timeButton3: Button
    private var selectedTime1: String? = null
    private var selectedTime2: String? = null
    private var selectedTime3: String? = null
    lateinit var Time1Text:TextView
    lateinit var Time2Text:TextView
    lateinit var Time3Text:TextView
//    lateinit var setTime1:TextView
//    lateinit var setTime2:TextView
//    lateinit var setTime3:TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if hardware UUID is stored in SharedPreferences
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val storedHardwareUuid = sharedPref.getString("hardware_uuid", null)

        if (storedHardwareUuid == null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        enableEdgeToEdge()
        setContentView(R.layout.activity_scheduling)

        // Initialize buttons
        timeButton1 = findViewById(R.id.timeButton1)
        timeButton2 = findViewById(R.id.timeButton2)
        timeButton3 = findViewById(R.id.timeButton3)
        Time1Text=findViewById(R.id.Time1text)
        Time2Text=findViewById(R.id.Time2text)
        Time3Text=findViewById(R.id.Time3text)
//        setTime1 = findViewById(R.id.setTime1)
//        setTime2 = findViewById(R.id.setTime2)
//        setTime3 = findViewById(R.id.setTime3)
        // Fetch stored times from Firestore
        fetchTimesFromFirestore(storedHardwareUuid)

        // Set click listeners to show TimePickerDialogs
        timeButton1.setOnClickListener { showTimePickerDialog(1, storedHardwareUuid) }
        timeButton2.setOnClickListener { showTimePickerDialog(2, storedHardwareUuid) }
        timeButton3.setOnClickListener { showTimePickerDialog(3, storedHardwareUuid) }

        // Apply window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Function to fetch times from Firestore
    private fun fetchTimesFromFirestore(hardwareUuid: String) {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val storedCollectionName = sharedPref.getString("collection_name", null)

        if (storedCollectionName != null) {
            db.collection(storedCollectionName).document(hardwareUuid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        selectedTime1 = document.getString("time1") ?: ""
                        selectedTime2 = document.getString("time2") ?: ""
                        selectedTime3 = document.getString("time3") ?: ""

                        // Create SpannableString for colored text
                        fun getColoredTimeText(label: String, time: String): SpannableString {
                            val fullText = "$label    $time"
                            val spannable = SpannableString(fullText)
                            val startIndex = fullText.indexOf(time)
                            val endIndex = startIndex + time.length
                            spannable.setSpan(
                                ForegroundColorSpan(Color.GRAY),  // Grey color for the time
                                startIndex,
                                endIndex,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            return spannable
                        }

                        // Update TextViews with colored time
                        Time1Text.text = getColoredTimeText("Time for 1st med:", selectedTime1!!)
                        Time2Text.text = getColoredTimeText("Time for 2nd med:", selectedTime2!!)
                        Time3Text.text = getColoredTimeText("Time for 3rd med:", selectedTime3!!)

                        // Update button text conditionally
                        timeButton1.text = if (selectedTime1!!.isNotEmpty()) "Reset" else "Set"
                        timeButton2.text = if (selectedTime2!!.isNotEmpty()) "Reset" else "Set"
                        timeButton3.text = if (selectedTime3!!.isNotEmpty()) "Reset" else "Set"

                    } else {
                        Log.d("Firestore", "No document found for hardware UUID: $hardwareUuid")
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error fetching document", e)
                }
        }
    }


    // Function to show TimePickerDialog based on the button clicked
    private fun showTimePickerDialog(buttonNumber: Int, hardwareUuid: String) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(this,
            { _, selectedHour, selectedMinute ->
                val selectedTime = String.format("%02d:%02d", selectedHour, selectedMinute)

                // Create SpannableString for colored text
                fun getColoredTimeText(label: String, time: String): SpannableString {
                    val fullText = "$label    $time"
                    val spannable = SpannableString(fullText)
                    val startIndex = fullText.indexOf(time)
                    val endIndex = startIndex + time.length
                    spannable.setSpan(
                        ForegroundColorSpan(Color.GRAY),  // Grey color for the time
                        startIndex,
                        endIndex,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    return spannable
                }

                when (buttonNumber) {
                    1 -> {
                        selectedTime1 = selectedTime
                        // Set the grey-colored time for Time1Text
                        Time1Text.text = getColoredTimeText("Time for 1st med:", selectedTime)
                    }
                    2 -> {
                        selectedTime2 = selectedTime
                        // Set the grey-colored time for Time2Text
                        Time2Text.text = getColoredTimeText("Time for 2nd med:", selectedTime)
                    }
                    3 -> {
                        selectedTime3 = selectedTime
                        // Set the grey-colored time for Time3Text
                        Time3Text.text = getColoredTimeText("Time for 3rd med:", selectedTime)
                    }
                }
                // Save the times locally and update Firestore
                saveTimesToLocalAndFirestore(hardwareUuid)
            }, hour, minute, true
        )
        timePickerDialog.show()
    }

    // Function to save the selected times to local storage and Firestore
    private fun saveTimesToLocalAndFirestore(hardwareUuid: String) {
        // Retrieve shared preferences
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        // Save selected times to SharedPreferences
        editor.putString("time1", selectedTime1)
        editor.putString("time2", selectedTime2)
        editor.putString("time3", selectedTime3)
        editor.apply()

        // Retrieve the stored collection name
        val storedCollectionName = sharedPref.getString("collection_name", null)

        if (storedCollectionName != null) {
            // Prepare the times data to be updated
            val timesMap = hashMapOf<String, Any?>(
                "time1" to selectedTime1,
                "time2" to selectedTime2,
                "time3" to selectedTime3
            )

            // Store the times in the correct collection based on the hardware UUID
            db.collection(storedCollectionName).document(hardwareUuid)
                .update(timesMap)
                .addOnSuccessListener {
                    Log.d("Firestore", "Times successfully updated in collection: $storedCollectionName for hardware UUID: $hardwareUuid")
                }
                .addOnFailureListener { e ->
                    Log.w("Firestore", "Error updating times in collection: $storedCollectionName for hardware UUID: $hardwareUuid", e)
                }
        } else {
            Log.e("Firestore", "Collection name not found in SharedPreferences")
        }
    }
}

