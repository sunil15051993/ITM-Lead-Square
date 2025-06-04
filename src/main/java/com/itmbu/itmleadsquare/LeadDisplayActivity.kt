package com.itmbu.itmleadsquare

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.itmbu.itmleadsquare.adapter.LeadsDisplayAdapter
import com.itmbu.itmleadsquare.api.Api
import com.itmbu.itmleadsquare.databinding.ActivityLeadDisplayBinding
import com.itmbu.itmleadsquare.databinding.ActivityLoginBinding
import com.itmbu.itmleadsquare.model.LeadsList
import com.itmbu.itmleadsquare.model.NewLeadsList
import org.json.JSONObject

class LeadDisplayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLeadDisplayBinding
    var pdDialog: ProgressDialog? = null
    var staffId: Int? = null
    var selectedFilter: String? = null
    lateinit var leadsAdapter: LeadsDisplayAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLeadDisplayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.theme_red));
        }
        val bar: ActionBar? = supportActionBar
        if (bar != null) {
            bar.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.theme_red)))
        }

        val bundle = intent.extras

        // Check if the Bundle is not null
        if (bundle != null) {
            staffId = bundle.getInt("staffId")
        }
        Log.e("LEAD", "onCreate: "+ staffId)

        init()
    }

    fun init(){
        binding.imageFilter.setOnClickListener {
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
            btnDone.visibility = View.GONE

            val checkboxes = listOf(cb1, cb2, cb3, cb4, cb5, cb6, cb7)

            // Restore previously selected
            when (selectedFilter) {
                "Cold" -> cb1.isChecked = true
                "Hot" -> cb2.isChecked = true
                "Interested" -> cb3.isChecked = true
                "Not Interested" -> cb4.isChecked = true
                "Not Response" -> cb5.isChecked = true
                "Completed" -> cb6.isChecked = true
                "New" -> cb7.isChecked = true
            }

            // Ensure only one is checked at a time
            checkboxes.forEach { checkbox ->
                checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (isChecked) {
                        checkboxes.filter { it != buttonView }.forEach { it.isChecked = false }
                        selectedFilter = checkbox.text.toString() // Save selection
                        Toast.makeText(this, "Selected: $selectedFilter", Toast.LENGTH_SHORT).show()
                        leadsAdapter.filterByStatus(selectedFilter)
                        bottomSheetDialog.dismiss()
                    }
                }
            }

            bottomSheetDialog.show()
        }
        callLeadsApi()

    }

    // look at this section
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.filter_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun callLeadsApi() {
        pdDialog = ProgressDialog(this@LeadDisplayActivity)
        pdDialog!!.setTitle("Please wait...")
        pdDialog!!.setCancelable(false)

        val url = Api.GET_STAFF_LEAD

        pdDialog!!.show()
        val stringRequest = object : StringRequest(
            Request.Method.POST, url.toString(),
            Response.Listener<String> { response ->
                pdDialog!!.dismiss()
                try {
                    val responseObj = JSONObject(response)
                    Log.e("res_category", response)

                    val recordsArray = responseObj.getJSONArray("records")
                    val leadsList = ArrayList<NewLeadsList>()

                    for (i in 0 until recordsArray.length()) {
                        val recordObj = recordsArray.getJSONObject(i)

                        val leadId = recordObj.getInt("lead_id")
                        val sName = recordObj.getString("sname")
                        val leadStatusId = recordObj.getInt("leadstatus_code")
                        val leadStatus = recordObj.getString("leadstatus_text")

//                        val sid = recordObj.getInt("sid")
//                        val sname = recordObj.getString("sname")
//                        val semail = recordObj.getString("semail")
//                        val smob = recordObj.getString("smob")
//                        val city = recordObj.getString("city")
//                        val grade12 = recordObj.getString("grade_12")
//                        val grade10 = recordObj.getString("grade_10")
//                        val created = recordObj.getString("created_at")
//                        val status = recordObj.getString("status")
//                        val source = recordObj.getString("source")
//                        val courseId = recordObj.getString("cid")
//                        val courseName = recordObj.getString("cname")
//                        val allocatedOn = recordObj.getString("allocated_on")
//
//                        val lead = LeadsList(
//                            sid, sname, semail, smob, city, grade12, grade10, created,
//                            status, source, courseId, courseName, allocatedOn
//                        )
                        val lead = NewLeadsList(leadId,sName,leadStatusId,leadStatus)
                        leadsList.add(lead)
                    }

                    // Setup RecyclerView
                    binding.recycleCategory.layoutManager = LinearLayoutManager(this)
                    leadsAdapter = LeadsDisplayAdapter(this, leadsList)
                    binding.recycleCategory.adapter = leadsAdapter

                    leadsAdapter.onCategoryItemClickListener = object : LeadsDisplayAdapter.OnCategoryItemClickListener {
                        override fun onCategoryItemClick(leads: NewLeadsList) {
                            val bundle = Bundle()
                            bundle.putInt("leadId", leads.leadId)
                            bundle.putString("studentName", leads.sName)
                            bundle.putInt("leadStatusId", leads.leadStatusId)
                            bundle.putString("leadStatus", leads.leadStatus)
                            bundle.putInt("staffId", staffId!!)

                            val intent = Intent(this@LeadDisplayActivity, ViewLeadActivity::class.java)
                            intent.putExtras(bundle)
                            startActivity(intent)
                            finish()
                        }
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("category_error", "Error: ${e.message}")
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
                params["staff_id"] = staffId.toString()
                return params
            }
        }
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

}