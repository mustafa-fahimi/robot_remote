package ir.matarata.robotremote

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    private val nodeMcuURL = "http://192.168.4.1/remote"
    private lateinit var requestQueue: RequestQueue
    private var gun3State = false
    private var joystickState = ""
    private var mappedStrength = 0
    private var mappedStrengthReverse = 0
    private var onPaused = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        ma_btn_settings.setOnClickListener {
            intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            finish()
        }
        ma_jsv_joystick.setOnMoveListener({ angle, strength ->
            if(onPaused){
                joystickState = "motor_off"
                val jsonObj = JSONObject(
                    "{\"token\":\"MatarataSecretToken1994\"," +
                            "\"request\":\"$joystickState\"," +
                            "\"strength\":\"0\"," +
                            "\"strengthRev\":\"1023\"}"
                )
                volleyJsonReqJoystick(jsonObj)
                return@setOnMoveListener
            }
            if (strength > 15 && (angle in 315..360 || angle in 0..45)) {
                joystickState = "motor_right"
            } else if (strength > 12 && angle in 45..135) {
                joystickState = "motor_forward"
            } else if (strength > 12 && angle in 135..225) {
                joystickState = "motor_left"
            } else if (strength > 12 && angle in 225..315) {
                joystickState = "motor_backward"
            } else if (strength < 12) {
                joystickState = "motor_off"
            }
            //Converted strength to range [0..1023]
            mappedStrength = mapRange(IntRange(0, 100), IntRange(500, 1030), strength)
            //Converted strength to range [1023..0]
            mappedStrengthReverse = mapRange(IntRange(0, 100), IntRange(530, 0), strength)
            val jsonObj = JSONObject(
                "{\"token\":\"MatarataSecretToken1994\"," +
                        "\"request\":\"$joystickState\"," +
                        "\"strength\":\"$mappedStrength\"," +
                        "\"strengthRev\":\"$mappedStrengthReverse\"}"
            )
            volleyJsonReqJoystick(jsonObj)
        }, 100)

    }

    private fun volleyJsonReq(jsonObj: JSONObject, elementName: String) {
        requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            nodeMcuURL,
            jsonObj,
            Response.Listener<JSONObject?> { response ->
                val res: String? = response?.getString("result")
                if (res.equals("done")) {
                    when (elementName) {
                        "gun1" -> {
                            val tempVoltage = response?.getString("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                        }
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

    private fun volleyJsonReqJoystick(jsonObj: JSONObject) {
        requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            nodeMcuURL,
            jsonObj,
            Response.Listener<JSONObject?> { response ->
                val res: String? = response?.getString("result")
                if (res.equals("done")) {
                    ma_tv_connectionState.text = getString(R.string.connection_state_connected)
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                } else {
                    ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
                }
            },
            Response.ErrorListener {
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

    private fun mapRange(range1: IntRange, range2: IntRange, value: Int): Int {
        return try {
            range2.start + (value - range1.start) * (range2.endInclusive - range2.start) / (range1.endInclusive - range1.start)
        }catch (e: Exception){
            0
        }
    }

    override fun onPause() {
        onPaused = true
        super.onPause()
    }

    override fun onStop() {
        onPaused = true
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        onPaused = false
        ma_jsv_joystick.resetButtonPosition()
    }

    override fun onRestart() {
        super.onRestart()
        onPaused = false
        ma_jsv_joystick.resetButtonPosition()
    }

}

class IntRange(override val start: Int, override val endInclusive: Int) : ClosedRange<Int>
