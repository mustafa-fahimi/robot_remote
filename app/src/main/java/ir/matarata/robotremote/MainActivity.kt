package ir.matarata.robotremote

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MATATAG"
    }

    private var gun3State = false
    private var joystickState = ""
    private var mappedStrength = 0
    private var onPaused = false
    private lateinit var handler: CoroutineExceptionHandler
    private lateinit var mSocket: WebSocket
    private lateinit var myJsonObject: JSONObject
    private lateinit var responseJsonObject: JSONObject
    private val socketURL = "ws://192.168.4.1:80"
    private lateinit var websSocketFactory: WebSocketFactory
    private var tempVoltage: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSystemBarColor(this, R.color.darker_blue)
        socketCreate()
        handler = CoroutineExceptionHandler { _, _ ->
            ma_tv_connectionState.text = getString(R.string.connection_state_problem)
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
        }

        ma_btn_gun1.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MatarataSecretToken1994")
                    myJsonObject.put("androidReq", "gun1")
                    socketSendReceive(myJsonObject)
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
                    ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MatarataSecretToken1994")
                    myJsonObject.put("androidReq", "gun2")
                    myJsonObject.put("state", "on")
                    socketSendReceive(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MatarataSecretToken1994")
                    myJsonObject.put("androidReq", "gun2")
                    myJsonObject.put("state", "off")
                    socketSendReceive(myJsonObject)
                }
            }
            true
        }
        ma_btn_gun3.setOnClickListener {
            if (!gun3State) {
                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                myJsonObject = JSONObject()
                myJsonObject.put("token", "MatarataSecretToken1994")
                myJsonObject.put("androidReq", "gun3")
                myJsonObject.put("state", "on")
                socketSendReceive(myJsonObject)
            } else if (gun3State) {
                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                myJsonObject = JSONObject()
                myJsonObject.put("token", "MatarataSecretToken1994")
                myJsonObject.put("androidReq", "gun3")
                myJsonObject.put("state", "off")
                socketSendReceive(myJsonObject)
            }
        }
        ma_btn_settings.setOnClickListener {
            mSocket.disconnect()
            intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            finish()
        }
        ma_jsv_joystick.setOnMoveListener({ angle, strength ->
            if (onPaused) {
                myJsonObject = JSONObject()
                myJsonObject.put("token", "MatarataSecretToken1994")
                myJsonObject.put("androidReq", "motor_off")
                myJsonObject.put("strength", "0")
                myJsonObject.put("strengthRev", "1023")
                socketSendReceive(myJsonObject)
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
                Log.d(TAG,"offing")
            }
            //Converted strength to range [0..1023]
            mappedStrength = mapRange(IntRange(0, 100), IntRange(400, 1030), strength)
            if(mappedStrength > 1023)
                mappedStrength = 1023
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MatarataSecretToken1994")
            myJsonObject.put("androidReq", joystickState)
            myJsonObject.put("strength", "$mappedStrength")
            Log.d(TAG, mappedStrength.toString())
            socketSendReceive(myJsonObject)
        }, 400)

    }

    private fun socketCreate() {
        websSocketFactory = WebSocketFactory().setConnectionTimeout(3000)
        mSocket = websSocketFactory.createSocket(socketURL)
        mSocket.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                if (text.toString() != "null")
                    processReceivedResult(text.toString())
            }

            override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
                mSocket.sendClose()
                mSocket.disconnect()
            }
        })
    }

    private fun socketSendReceive(jsonObj: JSONObject? = null) {
        if (mSocket.state.toString() == "CLOSED") {
            socketCreate()
        }
        CoroutineScope(handler).launch {
            if (mSocket.state.toString() == "CLOSED" || mSocket.state.toString() == "CREATED") {
                mSocket.connect()
            }
            while (true) {
                if (mSocket.state.toString() == "CLOSED")
                    break
                if (mSocket.isOpen) {
                    mSocket.sendText(jsonObj.toString())
                    break
                }
            }
        }
    }

    private fun processReceivedResult(res: String) {
        try {
            responseJsonObject = JSONObject(res)
            if (responseJsonObject.getString("espResult") == "done") {
                ma_tv_connectionState.text = getString(R.string.connection_state_connected)
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
                when (myJsonObject.getString("androidReq")) {
                    "gun2" -> {
                        tempVoltage = responseJsonObject.getDouble("voltage")
                        ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                    }
                    "gun3" -> {
                        if (myJsonObject.getString("state") == "on") {
                            gun3State = true //its on
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                        } else if (myJsonObject.getString("state") == "off") {
                            gun3State = false //its off
                        }
                    }
                }
            } else if (responseJsonObject.getString("espResult") == "fail") {
                ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
                if (myJsonObject.getString("androidReq") == "gun3") {
                    gun3State = false
                    ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                }
            }
        } catch (e: java.lang.Exception) {
            ma_tv_connectionState.text = getString(R.string.connection_state_problem)
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
            if (myJsonObject.getString("androidReq") == "gun3") {
                gun3State = false
                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
            }
        }
    }

    private fun mapRange(range1: IntRange, range2: IntRange, value: Int): Int {
        return try {
            range2.start + (value - range1.start) * (range2.endInclusive - range2.start) / (range1.endInclusive - range1.start)
        } catch (e: Exception) {
            0
        }
    }

    //Change the color of system bar in Apis above lollipop
    private fun setSystemBarColor(act: Activity, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = act.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = ContextCompat.getColor(this, color)
        }
    }

    override fun onPause() {
        onPaused = true
        mSocket.disconnect()
        super.onPause()
    }

    override fun onStop() {
        onPaused = true
        mSocket.disconnect()
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
