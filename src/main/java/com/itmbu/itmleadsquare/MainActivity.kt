package com.itmbu.itmleadsquare

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.itmbu.itmleadsquare.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.Manifest
import android.widget.Toast


class MainActivity : AppCompatActivity() {
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var binding: ActivityMainBinding
    private var isCalling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // Listen to call state
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            // Permission already granted, now start listening
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } else {
            // Ask for permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_PHONE_STATE), 101)
        }


        binding.buttonCall.setOnClickListener {
            val number = binding.textMyNumber.text.toString()
            Log.e("mob", "onCreate: "+ number)
            if (number.isNotEmpty()) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
                    val intent = Intent(Intent.ACTION_CALL)
                    intent.data = Uri.parse("tel:$number")
                    startActivity(intent)
                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
                }
            }
        }
    }

    private val callStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    isCalling = true
                }
                TelephonyManager.CALL_STATE_IDLE -> {
                    if (isCalling) {
                        // Call ended
                        showCallLog()
                        isCalling = false
                    }
                }
            }
        }
    }

    private fun showCallLog() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CALL_LOG), 2)
            return
        }

        val cursor = contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            null,
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.let {
            if (it.moveToFirst()) {
                val number = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER))
                val type = it.getInt(it.getColumnIndexOrThrow(CallLog.Calls.TYPE))
                val date = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val duration = it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)).toInt()

                val callType = when (type) {
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                }

                val callStatus = if (duration > 0) "Answered" else "Unanswered"

                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.getDefault())
                val callDate = sdf.format(Date(date))

                val logText = """
                Number: $number
                Type: $callType
                Status: $callStatus
                Date: $callDate
                Duration: ${duration} sec
            """.trimIndent()

                binding.textCallLog.text = logText
            }
            it.close()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now you can start listening
                telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            } else {
                Toast.makeText(this, "Permission denied to read phone state", Toast.LENGTH_SHORT).show()
            }
        }
    }

}