package ir.matarata.robotremote

import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private val nodeMcuURL = "http://192.168.4.1/remote"
    private var gun3State = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ma_iv_left.setOnTouchListener { _, event ->
            //change the color of arrow when they are on hold by user
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"left\",\"state\":\"on\"}")
                    volleyJsonReqArrows(jsonObj, "left")
                    ma_iv_left.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                }
                MotionEvent.ACTION_UP -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"left\",\"state\":\"off\"}")
                    volleyJsonReqArrows(jsonObj, "left")
                    ma_iv_left.colorFilter = null
                }
            }
            true
        }
        ma_iv_right.setOnTouchListener { _, event ->
            //change the color of arrow when they are on hold by user
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"right\",\"state\":\"on\"}")
                    volleyJsonReqArrows(jsonObj, "right")
                    ma_iv_right.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                }
                MotionEvent.ACTION_UP -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"right\",\"state\":\"off\"}")
                    volleyJsonReqArrows(jsonObj, "right")
                    ma_iv_right.colorFilter = null
                }
            }
            true
        }
        ma_iv_up.setOnTouchListener { _, event ->
            //change the color of arrow when they are on hold by user
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"forward\",\"state\":\"on\"}")
                    volleyJsonReqArrows(jsonObj, "forward")
                    ma_iv_up.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                }
                MotionEvent.ACTION_UP -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"forward\",\"state\":\"off\"}")
                    volleyJsonReqArrows(jsonObj, "forward")
                    ma_iv_up.colorFilter = null
                }
            }
            true
        }
        ma_iv_down.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"backward\",\"state\":\"on\"}")
                    volleyJsonReqArrows(jsonObj, "backward")
                    ma_iv_down.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                }
                MotionEvent.ACTION_UP -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"backward\",\"state\":\"off\"}")
                    volleyJsonReqArrows(jsonObj, "backward")
                    ma_iv_down.colorFilter = null
                }
            }
            true
        }
        ma_btn_gun1.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"gun1\"}")
                    volleyJsonReq(jsonObj, "gun1")
                }
                MotionEvent.ACTION_UP -> {
                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                }
            }
            true
        }
        ma_btn_gun2.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"gun2\",\"state\":\"on\"}")
                    volleyJsonReq(jsonObj, "gun2")
                }
                MotionEvent.ACTION_UP -> {
                    val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"gun2\",\"state\":\"off\"}")
                    volleyJsonReq(jsonObj, "gun2")
                }
            }
            true
        }
        ma_btn_gun3.setOnClickListener {
            if (!gun3State) {
                val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"gun3\",\"state\":\"on\"}")
                volleyJsonReq(jsonObj, "gun3")
            } else if (gun3State) {
                val jsonObj = JSONObject("{\"token\":\"MatarataSecretToken1994\",\"request\":\"gun3\",\"state\":\"off\"}")
                volleyJsonReq(jsonObj, "gun3")
            }
        }

    }

    private fun volleyJsonReq(jsonObj: JSONObject, elementName: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            nodeMcuURL,
            jsonObj,
            Response.Listener<JSONObject?> { response ->
                val res: String? = response?.getString("result")
                if (res.equals("done")) {
                    when (elementName) {
                        "gun2" -> {
                            if (jsonObj.getString("state") == "on") {
                                ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                            } else {
                                ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                            }
                        }
                        "gun3" -> {
                            if (jsonObj.getString("state") == "on") {
                                gun3State = true //its on
                                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                            } else {
                                gun3State = false //its off
                                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                            }
                        }
                    }
                    ma_tv_connectionState.text = getString(R.string.connection_state_connected)
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                } else {
                    when (elementName) {
                        "gun2" -> {
                            ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                        }
                        "gun3" -> {
                            gun3State = false //its off
                            ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                        }
                    }
                    ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
                }
            },
            Response.ErrorListener {
                when (elementName) {
                    "gun2" -> {
                        ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                    }
                    "gun3" -> {
                        gun3State = false //its off
                        ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                    }
                }
                ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            500,
            1,
            0.8F
        )
        requestQueue.add(jsonObjectRequest)
    }

    private fun volleyJsonReqArrows(jsonObj: JSONObject, arrowDirection: String) {
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            nodeMcuURL,
            jsonObj,
            Response.Listener<JSONObject?> { response ->
                val res: String? = response?.getString("result")
                if (res.equals("done")) {
                    when (arrowDirection) {
                        "left" -> {
                            if (jsonObj.getString("state") == "on") {
                                ma_iv_left.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                            } else {
                                ma_iv_left.colorFilter = null
                            }
                        }
                        "right" -> {
                            if (jsonObj.getString("state") == "on") {
                                ma_iv_right.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                            } else {
                                ma_iv_right.colorFilter = null
                            }
                        }
                        "forward" -> {
                            if (jsonObj.getString("state") == "on") {
                                ma_iv_up.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                            } else {
                                ma_iv_up.colorFilter = null
                            }
                        }
                        "backward" -> {
                            if (jsonObj.getString("state") == "on") {
                                ma_iv_down.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent), android.graphics.PorterDuff.Mode.MULTIPLY)
                            } else {
                                ma_iv_down.colorFilter = null
                            }
                        }
                    }
                    ma_tv_connectionState.text = getString(R.string.connection_state_connected)
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                } else {
                    when (arrowDirection) {
                        "left" -> {
                            ma_iv_left.colorFilter = null
                        }
                        "right" -> {
                            ma_iv_right.colorFilter = null
                        }
                        "forward" -> {
                            ma_iv_up.colorFilter = null
                        }
                        "backward" -> {
                            ma_iv_down.colorFilter = null
                        }
                    }
                    ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
                }
            },
            Response.ErrorListener {
                when (arrowDirection) {
                    "left" -> {
                        ma_iv_left.colorFilter = null
                    }
                    "right" -> {
                        ma_iv_right.colorFilter = null
                    }
                    "forward" -> {
                        ma_iv_up.colorFilter = null
                    }
                    "backward" -> {
                        ma_iv_down.colorFilter = null
                    }
                }
                ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            500,
            1,
            0.7F
        )
        requestQueue.add(jsonObjectRequest)
    }

}
