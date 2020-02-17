package ir.matarata.robotremote

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_setting.*
import org.json.JSONObject

class SettingActivity : AppCompatActivity() {

    private val nodeMcuURL = "http://192.168.4.1/remote"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        sa_btn_submit.setOnClickListener {
            when {
                sa_et_newPassword.text.isNullOrEmpty() -> {
                    sa_et_newPasswordAgain.error = "نباید خالی باشد"
                    return@setOnClickListener
                }
                sa_et_newPasswordAgain.text.isNullOrEmpty() -> {
                    sa_et_newPasswordAgain.error = "نباید خالی باشد"
                    return@setOnClickListener
                }
                sa_et_newPassword.text.toString().length < 8 -> {
                    sa_et_newPassword.error = "حداقل 8 کاراکتر باشد"
                    return@setOnClickListener
                }
                sa_et_newPasswordAgain.text != sa_et_newPassword.text -> {
                    sa_et_newPasswordAgain.error = "تکرار رمز اشتباه است"
                    return@setOnClickListener
                }
            }
            val newPassword = sa_et_newPassword.text.toString()
            val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"changePassword\",\"newPassword\":\"$newPassword\"}")
            volleyJsonReq(jsonObj)
        }

    }

    private fun volleyJsonReq(jsonObj: JSONObject) {
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            nodeMcuURL,
            jsonObj,
            Response.Listener<JSONObject?> { response ->
                val res: String? = response?.getString("result")
                if (res.equals("done")) {

                } else {

                }
            },
            Response.ErrorListener {

            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            500,
            1,
            1F
        )
        requestQueue.add(jsonObjectRequest)
    }

}
