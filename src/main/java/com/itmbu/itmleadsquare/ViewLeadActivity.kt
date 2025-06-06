package com.itmbu.itmleadsquare

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.ProgressDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CallLog
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.view.isVisible
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.itmbu.itmleadsquare.api.Api
import com.itmbu.itmleadsquare.databinding.ActivityViewLeadBinding
import com.itmbu.itmleadsquare.model.NewLeadsList
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ViewLeadActivity : AppCompatActivity() {
    private var parsedDate: Date? = null
    private lateinit var binding: ActivityViewLeadBinding
    var pdDialog: ProgressDialog? = null
    var leadId: Int? = null
    var studentName: String? = ""
    var leadStatusId: Int? = null
    var leadStatus: String? = ""
    var studentNumber: String? = ""
    var courseId: String? = ""
    var courseName: String? = ""
    var status: String? = ""
    var source: String? = ""
    var city: String? = ""
    var email: String? = ""
    var createdOn: String? = ""
    private lateinit var telephonyManager: TelephonyManager
    private var isCalling = false
    var totalDuration: Int? = null
    var mobNumber: String? = null
    var callingType: String? = null
    var callingStatus: String? = null
    var callingDate: String? = null
    var followupDate: String? = null
    var needBroucher: Int? = 0
    private var selectedCourse: String = ""
    private var selectedCallDisposition: String = ""
    private var selectedReason: String = ""
    private var selectedBroucher: String = ""
    private var selectedLeadStatus: String = ""
    private var selectedCallOutcome: String = ""
    var staffId: Int? = null
    // Convert to display format
    val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewLeadBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val navIcon = binding.toolbar.navigationIcon
        navIcon?.setTint(ContextCompat.getColor(this, R.color.white))
        binding.toolbar.navigationIcon = navIcon
        supportActionBar?.title = "Leads Data"

        binding.toolbar.setNavigationOnClickListener {
//            onBackPressedDispatcher.onBackPressed()
            val login = Intent(this@ViewLeadActivity, LeadDisplayActivity::class.java)
            val bundle = Bundle()
            bundle.putInt("staffId", staffId!!)
            login.putExtras(bundle)
            startActivity(login)
            finish()
        }


        val bundle = intent.extras

        // Check if the Bundle is not null
        if (bundle != null) {
            staffId = bundle.getInt("staffId")
            leadId = bundle.getInt("leadId")
            studentName = bundle.getString("studentName")
            leadStatusId = bundle.getInt("leadStatusId")
            leadStatus = bundle.getString("leadStatus")
        }
        Log.e("LEAD", "onCreate: " + leadId)

        // Listen to call state
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permission already granted, now start listening
            telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
        } else {
            // Ask for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                101
            )
        }

        init()
    }

    fun init() {

        callLeadsViewApi()

        val labelDis = "Call Disposition"
        val asterisk = " *"
        val coloredDis = SpannableString(labelDis + asterisk)
        coloredDis.setSpan(ForegroundColorSpan(Color.RED), labelDis.length, coloredDis.length, 0)
        binding.textCallDisposition.hint = coloredDis

        val labelOut = "OutCome"
        val coloredOut = SpannableString(labelOut + asterisk)
        coloredOut.setSpan(ForegroundColorSpan(Color.RED), labelOut.length, coloredOut.length, 0)
        binding.textCallOutcome.hint = coloredOut

        val labelRes = "Reason"
        val coloredRes = SpannableString(labelRes + asterisk)
        coloredRes.setSpan(ForegroundColorSpan(Color.RED), labelRes.length, coloredRes.length, 0)
        binding.textCallReason.hint = coloredRes

        val labelFollow = "FollowUp Date"
        val coloredFollow = SpannableString(labelFollow + asterisk)
        coloredFollow.setSpan(
            ForegroundColorSpan(Color.RED),
            labelFollow.length,
            coloredFollow.length,
            0
        )
        binding.textCallFollowUp.hint = coloredFollow

        val labelLeadStatus = "Lead Status"
        val coloredLead = SpannableString(labelLeadStatus + asterisk)
        coloredLead.setSpan(
            ForegroundColorSpan(Color.RED),
            labelLeadStatus.length,
            coloredLead.length,
            0
        )
        binding.textCallLeadStatus.hint = coloredLead

        binding.imageCall.setOnClickListener {
            val sMob = binding.textMobile.text.toString()
            Log.e("mob", "onCreate: " + sMob)
            if (!sMob.isNullOrEmpty()) {
                val callPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                val logPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG)

                if (callPermission == PackageManager.PERMISSION_GRANTED &&
                    logPermission == PackageManager.PERMISSION_GRANTED) {

                    // Both permissions are granted, proceed with the call
                    Log.e("VIEWLEAD", "callLeadsViewApi: "+ sMob)
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$sMob")
                    }
                    startActivity(intent)

                } else {
                    // Request both permissions if not granted
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_CALL_LOG),
                        1
                    )
                }
            }
        }

        binding.edittextSelectedCourse.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_selected_course, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(dialogView)

            val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxMtech)
            val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxBtech)
            val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxDiploma)
            val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxMca)
            val cb5 = dialogView.findViewById<CheckBox>(R.id.checkboxBca)
            val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

            // Only one can be selected at a time
            val checkboxes = listOf(cb1, cb2, cb3, cb4, cb5)
            checkboxes.forEach { checkbox ->
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        checkboxes.filter { it != buttonView }.forEach { it.isChecked = false }
                    }
                }
            }

            // Restore checked state
            when (selectedCourse) {
                "M.tech" -> cb1.isChecked = true
                "B.tech" -> cb2.isChecked = true
                "Diploma" -> cb3.isChecked = true
                "MCA" -> cb4.isChecked = true
                "BCA" -> cb5.isChecked = true
            }

            btnDone.setOnClickListener {
                val result = when {
                    cb1.isChecked -> "M.tech"
                    cb2.isChecked -> "B.tech"
                    cb3.isChecked -> "Diploma"
                    cb4.isChecked -> "MCA"
                    cb5.isChecked -> "BCA"
                    else -> ""
                }

                selectedCourse= result
                binding.edittextSelectedCourse.setText(result)

                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }


        binding.edittextCallDisposition.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_call_disposition, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(dialogView)

            val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxHadConversation)
            val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxUncontactable)
            val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxInvalid)
            val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

            // Only one can be selected at a time
            val checkboxes = listOf(cb1, cb2, cb3)
            checkboxes.forEach { checkbox ->
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        checkboxes.filter { it != buttonView }.forEach { it.isChecked = false }
                    }
                }
            }

            // Restore checked state
            when (selectedCallDisposition) {
                "Had a Conversation" -> cb1.isChecked = true
                "Uncontactable" -> cb2.isChecked = true
                "Invalid" -> cb3.isChecked = true
            }

            btnDone.setOnClickListener {
                val result = when {
                    cb1.isChecked -> "Had a Conversation"
                    cb2.isChecked -> "Uncontactable"
                    cb3.isChecked -> "Invalid"
                    else -> ""
                }

                selectedCallDisposition = result
                binding.edittextCallDisposition.setText(result)

                // Show/hide fields based on result
                when {
                    result.contains("Had a Conversation") -> {
                        binding.textCallOutcome.visibility = View.VISIBLE
                        binding.textCallReason.visibility = View.VISIBLE
                        binding.textNeedBroucher.visibility = View.VISIBLE
                    }

                    result.contains("Uncontactable") || result.contains("Invalid") -> {
                        binding.textCallOutcome.visibility = View.GONE
                        binding.textCallReason.visibility = View.VISIBLE
//                        binding.textCallFollowUp.visibility = View.VISIBLE
                    }

                    else -> {
                        binding.textCallOutcome.visibility = View.GONE
                        binding.textCallReason.visibility = View.GONE
//                        binding.textCallFollowUp.visibility = View.GONE
                        binding.textNeedBroucher.visibility = View.GONE
                    }
                }
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }

        binding.edittextCallOutcome.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_call_outcome, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(dialogView)

            val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxCold)
            val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxInterested)
            val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxNotInterested)
            val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

            // Restore checked state
            selectedCallOutcome?.split(", ")?.forEach { item ->
                when (item) {
                    "Cold" -> cb1.isChecked = true
                    "Interested" -> cb2.isChecked = true
                    "Not Interested" -> cb3.isChecked = true
                }
            }

            // Only one can be selected at a time
            val checkboxes = listOf(cb1, cb2, cb3)
            checkboxes.forEach { checkbox ->
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        checkboxes.filter { it != buttonView }.forEach { it.isChecked = false }
                    }
                }
            }

            btnDone.setOnClickListener {
                val selected = mutableListOf<String>()
                if (cb1.isChecked) selected.add("Cold")
                if (cb2.isChecked) selected.add("Interested")
                if (cb3.isChecked) selected.add("Not Interested")

                val result = selected.joinToString(", ")
                selectedCallOutcome = result
                binding.edittextCallOutcome.setText(result)
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }

        binding.edittextReason.setOnClickListener {
            if (selectedCallOutcome == "Cold") {
                val dialogView = layoutInflater.inflate(R.layout.dialog_call_reason_cold, null)
                val bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.setContentView(dialogView)

                val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxCallmelater)
                val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxNeedtodiscuss)
                val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxWaitingresult)
                val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxNottherighttime)
                val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

                // Restore checked state
                selectedReason.split(", ").forEach { item ->
                    when (item) {
                        "Call me Later" -> cb1.isChecked = true
                        "Need to Discuss" -> cb2.isChecked = true
                        "Waiting for Results" -> cb3.isChecked = true
                        "Not the Right Time" -> cb4.isChecked = true
                    }
                }

                // Only one can be selected at a time
                val checkboxes = listOf(cb1, cb2, cb3, cb4)
                checkboxes.forEach { checkbox ->
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            checkboxes.filter { it != buttonView }
                                .forEach { it.isChecked = false }
                        }
                    }
                }

                btnDone.setOnClickListener {
                    val selected = mutableListOf<String>()
                    if (cb1.isChecked) selected.add("Call me Later")
                    if (cb2.isChecked) selected.add("Need to Discuss")
                    if (cb3.isChecked) selected.add("Waiting for Results")
                    if (cb4.isChecked) selected.add("Not the Right Time")

                    val result = selected.joinToString(", ")
                    selectedReason = result
                    binding.edittextReason.setText(result)
                    bottomSheetDialog.dismiss()
                }
                bottomSheetDialog.show()
            } else if (selectedCallOutcome == "Interested") {
                val dialogView =
                    layoutInflater.inflate(R.layout.dialog_call_reason_interested, null)
                val bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.setContentView(dialogView)

                val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxVerypositive)
                val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxNeedtovisitcampus)
                val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxDiscusswithparents)
                val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxContactmelater)
                val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

                // Restore checked state
                selectedReason.split(", ").forEach { item ->
                    when (item) {
                        "Very Positive" -> cb1.isChecked = true
                        "Need to Visit Campus" -> cb2.isChecked = true
                        "Discuss with Parents" -> cb3.isChecked = true
                        "Contact me Later" -> cb4.isChecked = true
                    }
                }

                // Only one can be selected at a time
                val checkboxes = listOf(cb1, cb2, cb3, cb4)
                checkboxes.forEach { checkbox ->
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            checkboxes.filter { it != buttonView }
                                .forEach { it.isChecked = false }
                        }
                    }
                }

                btnDone.setOnClickListener {
                    val selected = mutableListOf<String>()
                    if (cb1.isChecked) selected.add("Very Positive")
                    if (cb2.isChecked) selected.add("Need to Visit Campus")
                    if (cb3.isChecked) selected.add("Discuss with Parents")
                    if (cb4.isChecked) selected.add("Contact me Later")

                    val result = selected.joinToString(", ")
                    selectedReason = result
                    binding.edittextReason.setText(result)
                    bottomSheetDialog.dismiss()
                }
                bottomSheetDialog.show()
            } else if (selectedCallOutcome == "Not Interested") {
                val dialogView =
                    layoutInflater.inflate(R.layout.dialog_call_reason_not_interested, null)
                val bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.setContentView(dialogView)

                val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxFeestructure)
                val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxJoiningelsewhere)
                val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxNotlooking)
                val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxCalldisconnect)
                val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

                // Restore checked state
                selectedReason.split(", ").forEach { item ->
                    when (item) {
                        "Fee Structure" -> cb1.isChecked = true
                        "Joining Elsewhere" -> cb2.isChecked = true
                        "Not Looking for Any Program" -> cb3.isChecked = true
                        "Call Disconnect" -> cb4.isChecked = true
                    }
                }

                // Only one can be selected at a time
                val checkboxes = listOf(cb1, cb2, cb3, cb4)
                checkboxes.forEach { checkbox ->
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            checkboxes.filter { it != buttonView }
                                .forEach { it.isChecked = false }
                        }
                    }
                }

                btnDone.setOnClickListener {
                    val selected = mutableListOf<String>()
                    if (cb1.isChecked) selected.add("Fee Structure")
                    if (cb2.isChecked) selected.add("Joining Elsewhere")
                    if (cb3.isChecked) selected.add("Not Looking for Any Program")
                    if (cb4.isChecked) selected.add("Call Disconnect")

                    val result = selected.joinToString(", ")
                    selectedReason = result
                    binding.edittextReason.setText(result)
                    bottomSheetDialog.dismiss()
                }
                bottomSheetDialog.show()
            } else if (selectedCallDisposition == "Uncontactable") {
                val dialogView = layoutInflater.inflate(R.layout.dialog_call_reason_uncontact, null)
                val bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.setContentView(dialogView)

                val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxCallnotreceived)
                val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxSwitchedOff)
                val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxNotReachable)
                val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxRinging)
                val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

                // Restore checked state
                selectedReason.split(", ").forEach { item ->
                    when (item) {
                        "Call not Received" -> cb1.isChecked = true
                        "Switched Off" -> cb2.isChecked = true
                        "Not Reachable" -> cb3.isChecked = true
                        "Ringing" -> cb4.isChecked = true
                    }
                }

                // Only one can be selected at a time
                val checkboxes = listOf(cb1, cb2, cb3, cb4)
                checkboxes.forEach { checkbox ->
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            checkboxes.filter { it != buttonView }
                                .forEach { it.isChecked = false }
                        }
                    }
                }

                btnDone.setOnClickListener {
                    val selected = mutableListOf<String>()
                    if (cb1.isChecked) selected.add("Call not Received")
                    if (cb2.isChecked) selected.add("Switched Off")
                    if (cb3.isChecked) selected.add("Not Reachable")
                    if (cb4.isChecked) selected.add("Ringing")

                    val result = selected.joinToString(", ")
                    selectedReason = result
                    binding.edittextReason.setText(result)
                    bottomSheetDialog.dismiss()
                }
                bottomSheetDialog.show()
            } else if (selectedCallDisposition == "Invalid") {
                val dialogView = layoutInflater.inflate(R.layout.dialog_call_reason_invalid, null)
                val bottomSheetDialog = BottomSheetDialog(this)
                bottomSheetDialog.setContentView(dialogView)

                val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxNumberinvalid)
                val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxWrongnumber)
                val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxTemporaryout)
                val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxIncomingnot)
                val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

                // Restore checked state
                selectedReason.split(", ").forEach { item ->
                    when (item) {
                        "Number Invalid" -> cb1.isChecked = true
                        "Wrong Number" -> cb2.isChecked = true
                        "Temporary Out of Service" -> cb3.isChecked = true
                        "Incoming not Available" -> cb4.isChecked = true
                    }
                }

                // Only one can be selected at a time
                val checkboxes = listOf(cb1, cb2, cb3, cb4)
                checkboxes.forEach { checkbox ->
                    checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                        if (isChecked) {
                            checkboxes.filter { it != buttonView }
                                .forEach { it.isChecked = false }
                        }
                    }
                }

                btnDone.setOnClickListener {
                    val selected = mutableListOf<String>()
                    if (cb1.isChecked) selected.add("Number Invalid")
                    if (cb2.isChecked) selected.add("Wrong Number")
                    if (cb3.isChecked) selected.add("Temporary Out of Service")
                    if (cb4.isChecked) selected.add("Incoming not Available")

                    val result = selected.joinToString(", ")
                    selectedReason = result
                    binding.edittextReason.setText(result)
                    bottomSheetDialog.dismiss()
                }
                bottomSheetDialog.show()
            }
        }

        binding.edittextFollowUp.setOnClickListener {
            val calendar = Calendar.getInstance()
            parsedDate?.let { calendar.time = it }

            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePicker = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(selectedYear, selectedMonth, selectedDay)
                }

                val newDisplayDate = outputFormat.format(selectedCalendar.time)
                val newStorageDate = inputFormat.format(selectedCalendar.time)

                // Update EditText and storage variable
                binding.edittextFollowUp.setText(newDisplayDate)
                followupDate = newStorageDate
            }, year, month, day)

            datePicker.show()
        }

        binding.edittextLeadStatus.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_lead_status, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(dialogView)

            val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxCold)
            val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxHot)
            val cb3 = dialogView.findViewById<CheckBox>(R.id.checkboxInter)
            val cb4 = dialogView.findViewById<CheckBox>(R.id.checkboxNotInter)
            val cb5 = dialogView.findViewById<CheckBox>(R.id.checkboxNoResp)
            val cb6 = dialogView.findViewById<CheckBox>(R.id.checkboxCompleted)
            val cb7 = dialogView.findViewById<CheckBox>(R.id.checkboxNew)
            val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

            // Restore checked state
            selectedLeadStatus.split(", ").forEach { item ->
                when (item) {
                    "Cold" -> cb1.isChecked = true
                    "Hot" -> cb2.isChecked = true
                    "Interested" -> cb3.isChecked = true
                    "Not Interested" -> cb4.isChecked = true
                    "Not Response" -> cb5.isChecked = true
                    "Completed" -> cb6.isChecked = true
                    "New" -> cb7.isChecked = true
                }
            }

            // Only one can be selected at a time
            val checkboxes = listOf(cb1, cb2, cb3, cb4, cb5, cb6, cb7)
            checkboxes.forEach { checkbox ->
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        checkboxes.filter { it != buttonView }.forEach { it.isChecked = false }
                    }
                }
            }

            btnDone.setOnClickListener {
                val selected = mutableListOf<String>()
                if (cb1.isChecked) selected.add("Cold")
                if (cb2.isChecked) selected.add("Hot")
                if (cb3.isChecked) selected.add("Interested")
                if (cb4.isChecked) selected.add("Not Interested")
                if (cb5.isChecked) selected.add("Not Response")
                if (cb6.isChecked) selected.add("Completed")
                if (cb7.isChecked) selected.add("New")

                val result = selected.joinToString(", ")
                selectedLeadStatus = result
                binding.edittextLeadStatus.setText(result)
                bottomSheetDialog.dismiss()
            }
            Log.e("VIEWLEAD", "init: " + binding.edittextLeadStatus.text.toString())
            bottomSheetDialog.show()
        }

        binding.edittextNeedBroucher.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_need_broucher, null)
            val bottomSheetDialog = BottomSheetDialog(this)
            bottomSheetDialog.setContentView(dialogView)

            val cb1 = dialogView.findViewById<CheckBox>(R.id.checkboxYes)
            val cb2 = dialogView.findViewById<CheckBox>(R.id.checkboxNo)
            val btnDone = dialogView.findViewById<Button>(R.id.btnDone)

            // Restore checked state
            selectedBroucher.split(", ").forEach { item ->
                when (item) {
                    "Yes" -> cb1.isChecked = true
                    "No" -> cb2.isChecked = true
                }
            }

            // Only one can be selected at a time
            val checkboxes = listOf(cb1, cb2)
            checkboxes.forEach { checkbox ->
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        checkboxes.filter { it != buttonView }.forEach { it.isChecked = false }
                    }
                }
            }

            btnDone.setOnClickListener {
                val selected = mutableListOf<String>()
                if (cb1.isChecked) selected.add("Yes")
                if (cb2.isChecked) selected.add("No")

                val result = selected.joinToString(", ")
                selectedBroucher = result
                binding.edittextNeedBroucher.setText(result)
                bottomSheetDialog.dismiss()
            }
            bottomSheetDialog.show()
        }


        binding.buttonSave.setOnClickListener {
            var broucher = binding.edittextNeedBroucher.text.toString()
            if (broucher == "Yes") {
                needBroucher == 1
            } else {
                needBroucher == 0
            }

            // Validate Call Disposition
            if (binding.edittextCallDisposition.text.isNullOrBlank()) {
                Toast.makeText(this@ViewLeadActivity, "Select Call Disposition", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }

            // Validate Call Outcome only if it's visible
            if (binding.textCallOutcome.isVisible && binding.edittextCallOutcome.text.isNullOrBlank()) {
                Toast.makeText(this@ViewLeadActivity, "Select Call Outcome", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate Reason only if visible
            if (binding.textCallReason.isVisible && binding.edittextReason.text.isNullOrBlank()) {
                Toast.makeText(this@ViewLeadActivity, "Select Reason", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Validate Lead Status
            if (binding.edittextLeadStatus.text.isNullOrBlank()) {
                Toast.makeText(this@ViewLeadActivity, "Select Lead Status", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            callLogPostApi(totalDuration, mobNumber, callingType, callingStatus, callingDate)

        }

    }

    private fun callLeadsViewApi() {
        pdDialog = ProgressDialog(this@ViewLeadActivity)
        pdDialog!!.setTitle("Please wait...")
        pdDialog!!.setCancelable(false)

        val url = Api.VIEW_STAFF_LEAD

        pdDialog!!.show()
        val stringRequest = object : StringRequest(
            Request.Method.POST, url.toString(),
            Response.Listener<String> { response ->
                pdDialog!!.dismiss()
                try {
                    val responseObj = JSONObject(response)
                    Log.e("res_view_lead", response)

                    val recordsArray = responseObj.getJSONArray("records")
                    val leadsList = ArrayList<NewLeadsList>()

                    for (i in 0 until recordsArray.length()) {
                        val recordObj = recordsArray.getJSONObject(i)

                        val sId = recordObj.getInt("sid")
                        val sName = recordObj.getString("sname")
                        val sEmail = recordObj.getString("semail")
                        val sMob = recordObj.getString("smob")
                        val city = recordObj.getString("city")
                        val grade12 = recordObj.getString("grade_12")
                        val grade10 = recordObj.getString("grade_10")
                        val created = recordObj.getString("created_at")
                        val status = recordObj.getString("status")
                        val source = recordObj.getString("source")
                        courseId = recordObj.getString("courseid")
                        val courseName = recordObj.getString("coursename")
                        val allocatedOn = recordObj.getString("allocated_on")
                        val fId = recordObj.getString("fid")
                        val leadId = recordObj.getString("lead_id")
                        val notes = recordObj.getString("notes")
                        val nextFollowDate = recordObj.getString("next_followup")
                        val followedOn = recordObj.getString("followed_on")
                        val leadStatus = recordObj.getString("leadstatus")
                        val mobNo = recordObj.getString("mob_no")
                        val type = recordObj.getString("type")
                        val callStatus = recordObj.getString("call_status")
                        val duration = recordObj.getString("duration")
                        val whatsappSend = recordObj.getString("whatsapp_send")
                        val callDisposition = recordObj.getString("call_disposition")
                        val outcome = recordObj.getString("outcome")
                        val reason = recordObj.getString("reason")

                        binding.textName.text = sName
                        binding.textMobile.text = sMob
                        selectedCourse = courseId as String
                        binding.textCourse.text = selectedCourse
                        binding.textCity.text = city
                        binding.textEmail.text = sEmail
                        binding.textSource.text = source
                        binding.textCreated.text = createdOn
                        binding.textStatus.text = leadStatus
                        binding.edittextSelectedCourse.setText(selectedCourse)
                        selectedCallDisposition = callDisposition
                        binding.edittextCallDisposition.setText(selectedCallDisposition ?: "")
                        selectedCallOutcome = outcome
                        binding.edittextCallOutcome.setText(selectedCallOutcome ?: "")
                        selectedReason = reason
                        binding.edittextReason.setText(selectedReason ?: "")

                        followupDate = if (nextFollowDate.trim().isNotEmpty()) nextFollowDate else allocatedOn


                        parsedDate = inputFormat.parse(followupDate)
                        val formattedDate = outputFormat.format(parsedDate)

                        // Set the formatted date to EditText
                        binding.edittextFollowUp.setText(formattedDate)

                        selectedLeadStatus = leadStatus
                        binding.edittextLeadStatus.setText(selectedLeadStatus ?: "")
                        selectedBroucher = whatsappSend
                        binding.edittextNeedBroucher.setText(selectedBroucher ?: "")
                        binding.edittextNotes.setText(notes ?: "")

                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("category_error", "Error: ${e.message}")
                }

            },
            Response.ErrorListener { error ->
                pdDialog!!.dismiss()
                Log.e("RequestError", "Registration Error: " + error.toString())
                Toast.makeText(applicationContext, "Something went wrong !!", Toast.LENGTH_LONG)
                    .show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["lead_id"] = leadId.toString()
                return params
            }
        }
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    private val callStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, incomingNumber: String?) {
            when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> {
                    isCalling = true
                }

                TelephonyManager.CALL_STATE_IDLE -> {
                    if (isCalling) {
                        isCalling = false
                        Handler(Looper.getMainLooper()).postDelayed({
                            showCallLog()
                        }, 2000) // Delay 2 seconds
                    }
                }

            }
        }
    }

    private fun showCallLog() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
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
                val duration =
                    it.getString(it.getColumnIndexOrThrow(CallLog.Calls.DURATION)).toInt()

                val callType = when (type) {
                    CallLog.Calls.OUTGOING_TYPE -> "Outgoing"
                    CallLog.Calls.INCOMING_TYPE -> "Incoming"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                }

//                val callStatus = if (duration > 0) "Answered" else "Unanswered"
                val callStatus = when (type) {
                    CallLog.Calls.INCOMING_TYPE -> if (duration > 0) "Answered" else "Missed"
                    CallLog.Calls.OUTGOING_TYPE -> if (duration > 0) "Answered" else "Not Answered"
                    CallLog.Calls.MISSED_TYPE -> "Missed"
                    else -> "Unknown"
                }
                val sdf = SimpleDateFormat("dd/MM/yyyy hh:mm:ss", Locale.getDefault())
                val callDate = sdf.format(Date(date))

                val minutes = duration / 60
                val seconds = duration % 60
                val totalCallTime = String.format("%02d:%02d", minutes, seconds)

                // Now, to convert back from durationFormatted to total seconds:
                val parts = totalCallTime.split(":")
                val durationFormatted = parts[0].toInt() * 60 + parts[1].toInt() // 2*60 + 30 = 150


                val logText = """
                Number: $number
                Type: $callType
                Status: $callStatus
                Date: $callDate
                Duration: ${durationFormatted} sec
                 """.trimIndent()

                Toast.makeText(this@ViewLeadActivity, duration.toString(), Toast.LENGTH_LONG).show()
                Log.e("VIEWLEAD", "showCallLog: " + logText)
                totalDuration = durationFormatted
                mobNumber = number
                callingType = callType
                callingStatus = callStatus
                callingDate = callDate
            }
            it.close()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 101) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, now you can start listening
                telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
            } else {
                Toast.makeText(this, "Permission denied to read phone state", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun callLogPostApi(
        totalDuration: Int?,
        mobNumber: String?,
        callingType: String?,
        callingStatus: String?,
        callingDate: String?
    ) {
        pdDialog = ProgressDialog(this@ViewLeadActivity)
        pdDialog!!.setTitle("Please wait...")
        pdDialog!!.setCancelable(false)

        val url = Api.STAFF_LEAD_POST
        Log.e("LOGIN", "res_login" + url!!)
        pdDialog!!.show()
        val stringRequest = object : StringRequest(
            Request.Method.POST, url.toString(),
            Response.Listener<String> { response ->
                pdDialog!!.dismiss()
                Log.e("LOGIN", "res_view_lead " + response!!)
                try {
                    val jsonObject = JSONObject(response)

                    val error = jsonObject.getBoolean("error")
                    val records = jsonObject.getBoolean("records")

                    if (!error) {
                        callThankyouDialog()
                        Toast.makeText(this@ViewLeadActivity, "Success", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(
                            this@ViewLeadActivity,
                            "Something went wrong!!",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("API Error", "Exception: ${e.message}")
                }

            },
            Response.ErrorListener { error ->
                pdDialog!!.dismiss()
                Log.e("RequestError", "Registration Error: " + error.toString())
                Toast.makeText(applicationContext, "Registration Error !!", Toast.LENGTH_LONG)
                    .show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["lead_id"] = leadId.toString()
                params["duration"] = totalDuration.toString()
                params["type"] = callingType.toString()
                params["notes"] = binding.edittextNotes.text.toString()
                params["next_followup"] = followupDate.toString()
                params["courseid"] = binding.edittextSelectedCourse.text.toString()
                params["status"] = binding.edittextLeadStatus.text.toString()
                params["mob_no"] = mobNumber.toString()
                params["call_status"] = callingStatus.toString()
                params["call_disposition"] = binding.edittextCallDisposition.text.toString()
                params["outcome"] = binding.edittextCallOutcome.text.toString()
                params["reason"] = binding.edittextReason.text.toString()
                params["whatsapp_send"] = needBroucher.toString()
                Log.e("VIEWLEAD", params.toString())
                return params
            }
        }
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    private fun callThankyouDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_thankyou, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.show()

        Handler(Looper.getMainLooper()).postDelayed({
            dialog.dismiss()
            val login = Intent(this@ViewLeadActivity, LeadDisplayActivity::class.java)
            val bundle = Bundle()
            bundle.putInt("staffId", staffId!!)
            login.putExtras(bundle)
            startActivity(login)
            finish()
        }, 3000)
    }

}