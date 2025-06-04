package com.itmbu.itmleadsquare

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.itmbu.itmleadsquare.api.Api
import com.itmbu.itmleadsquare.databinding.ActivityLoginBinding
import com.itmbu.itmleadsquare.databinding.ActivityMainBinding
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    var pdDialog: ProgressDialog? = null
    lateinit var sharedPreference: SharedPreferences
    lateinit var loginPrefsEditor: SharedPreferences.Editor
    private var doubleBackToExitPressedOnce = false
    private var saveLogin: Boolean? = null
    var username: String? = null
    var password: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreference = getSharedPreferences("relproPref", Context.MODE_PRIVATE)
        loginPrefsEditor = sharedPreference.edit();
        saveLogin = sharedPreference.getBoolean("saveLogin", false);
        if (saveLogin == true) {
            binding.edittextId.setText(sharedPreference.getString("username", ""));
            binding.edittextPassword.setText(sharedPreference.getString("password", ""));
            binding.checkRemember.setChecked(true);
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(getResources().getColor(R.color.theme_red));
        }
        val bar: ActionBar? = supportActionBar
        if (bar != null) {
            bar.setBackgroundDrawable(ColorDrawable(resources.getColor(R.color.theme_red)))
        }

        init()
    }

    fun init() {
        binding.buttonLogin.setOnClickListener { view ->
            if (binding.edittextId.length() == 0) {
                binding.edittextId.requestFocus();
                binding.edittextId.setError("FIELD CANNOT BE EMPTY");
            } else if (binding.edittextPassword.length() == 9) {
                binding.edittextPassword.requestFocus();
                binding.edittextPassword.setError("FIELD CANNOT BE EMPTY");
            } else {
                // Save username only if the checkbox is checked
                if (binding.checkRemember.isChecked) {
                    loginPrefsEditor.putBoolean("saveLogin", true);
                    loginPrefsEditor.putString("username", binding.edittextId.text.toString());
                    loginPrefsEditor.putString("password", binding.edittextPassword.text.toString());
                    loginPrefsEditor.commit();
                } else {
                    loginPrefsEditor.clear();
                    loginPrefsEditor.commit();
                }

                callLoginApi()

            }

        }

    }

    private fun callLoginApi() {
        pdDialog = ProgressDialog(this@LoginActivity)
        pdDialog!!.setTitle("Please wait...")
        pdDialog!!.setCancelable(false)

        val url = Api.LOGIN
        Log.e("LOGIN","res_login"+ url!!)
        pdDialog!!.show()
        val stringRequest = object : StringRequest(
            Request.Method.POST, url.toString(),
            Response.Listener<String> { response ->
                pdDialog!!.dismiss()
                Log.e("LOGIN","res_login "+ response!!)
                try {
                    val jsonObject = JSONObject(response)

                    val error = jsonObject.getBoolean("error")
                    val message = jsonObject.getString("message")
                    val aid = jsonObject.getInt("aid")
                    val aname = jsonObject.getString("aname")
                    val records = jsonObject.getString("records")

                    val editor = sharedPreference.edit()
                    editor.putString("username", binding.edittextId.text.toString())
                    editor.putInt("userid", aid)
                    editor.putString("name", aname)
                    editor.putString("email", records)
                    editor.apply()

                    Log.e("LOGIN","User ID " + aid)
                    Log.e("LOGIN","User Name" + aname)
                    Log.e("LOGIN","User Email" + records)

                    if (!error) {
                        Toast.makeText(this@LoginActivity, "Login Success", Toast.LENGTH_LONG).show()
                        val login = Intent(this@LoginActivity, LeadDisplayActivity::class.java)
                        val bundle = Bundle()
                        bundle.putInt("staffId", aid)
                        login.putExtras(bundle)
                        Log.e("LOGIN","User ID " + aid)
                        startActivity(login)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_LONG).show()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("API Error", "Exception: ${e.message}")
                }

            },
            Response.ErrorListener { error ->
                pdDialog!!.dismiss()
                Log.e("RequestError", "Registration Error: " + error.toString())
                Toast.makeText(applicationContext, "Registration Error !!", Toast.LENGTH_LONG).show()
            }
        ) {
            override fun getParams(): MutableMap<String, String> {
                val params: MutableMap<String, String> = HashMap()
                params["email"] = binding.edittextId.text.toString()
                params["pwd"] = binding.edittextPassword.text.toString()
                params["type"] = "0"
                return params
            }
        }
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(stringRequest)
    }

    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000) // Reset the flag after 2 seconds
    }

}