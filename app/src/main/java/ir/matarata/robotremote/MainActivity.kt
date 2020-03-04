package ir.matarata.robotremote

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter


class MainActivity : AppCompatActivity() {

    companion object {
        const val socketURL = "192.168.4.1"
        const val socketPort = 8888
        const val TAG = "MATATAG"
    }

    private var gun3State = false
    private var joystickState = ""
    private var mappedStrength = 0
    private var mappedStrengthReverse = 0
    private var onPaused = false
    private lateinit var handler: CoroutineExceptionHandler
    //private var socket: Socket = Socket()
    private lateinit var myCoroutineRes: Deferred<String>
    private lateinit var myJsonObject: JSONObject
    private lateinit var responseJsonObject: JSONObject
    private lateinit var outStream: OutputStream
    private lateinit var inStream: InputStream
    private lateinit var printWriter: PrintWriter
    private var responseString = ""
    private var availableBytes: Int = 0
    private lateinit var buffer: ByteArray

    private lateinit var socket2: io.socket.client.Socket

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handler = CoroutineExceptionHandler { _, _ ->
            ma_tv_connectionState.text = getString(R.string.connection_state_problem)
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
        }

        ma_btn_gun1.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    socketIOConnect()

                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    /*myJsonObject = JSONObject()
                    myJsonObject.put("token", "MatarataSecretToken1994")
                    myJsonObject.put("androidReq", "gun1")
                    socketSendReceive(myJsonObject)*/
                }
                MotionEvent.ACTION_UP -> {
                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                }
            }
            true
        }
        /*ma_btn_gun2.setOnTouchListener { _, event ->
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
        }*/
        ma_btn_settings.setOnClickListener {
            intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            finish()
        }
        ma_jsv_joystick.setOnMoveListener({ angle, strength ->
            if (onPaused) {
                joystickState = "motor_off"
                val jsonObj = JSONObject(
                    "{\"token\":\"MatarataSecretToken1994\"," +
                            "\"request\":\"$joystickState\"," +
                            "\"strength\":\"0\"," +
                            "\"strengthRev\":\"1023\"}"
                )
                //volleyJsonReqJoystick(jsonObj)
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
            /*val jsonObj = JSONObject(
                "{\"token\":\"MatarataSecretToken1994\"," +
                        "\"request\":\"$joystickState\"," +
                        "\"strength\":\"$mappedStrength\"," +
                        "\"strengthRev\":\"$mappedStrengthReverse\"}"
            )
            volleyJsonReqJoystick(jsonObj)*/
        }, 100)

    }

    /* private fun volleyJsonReq(jsonObj: JSONObject, elementName: String) {
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
     }*/

    private fun mapRange(range1: IntRange, range2: IntRange, value: Int): Int {
        return try {
            range2.start + (value - range1.start) * (range2.endInclusive - range2.start) / (range1.endInclusive - range1.start)
        } catch (e: Exception) {
            0
        }
    }

    private fun socketIOConnect() = runBlocking(handler){
        launch {
            socket2 = IO.socket("http://192.168.4.1")
            socket2.connect()
                .on(Socket.EVENT_CONNECT, { Log.d(TAG, "Connected") })
                .on(Socket.EVENT_DISCONNECT, { Log.d(TAG, "Disonnected") })
        }
    }

    /*private fun socketSendReceive(jsonObj: JSONObject) = runBlocking(handler) {
        myCoroutineRes = GlobalScope.async(handler) {
            try {
                Log.d(TAG, "here")
                socket = Socket(socketURL, socketPort)
                socket.apply {
                    outStream = this.getOutputStream()
                    printWriter = PrintWriter(outStream)
                    printWriter.print(jsonObj.toString())
                    printWriter.flush()
                    responseString = ""
                    availableBytes = 0
                    Thread.sleep(10)
                    inStream = this.getInputStream()
                    availableBytes = inStream.available()
                    if (availableBytes > 0) {
                        buffer = ByteArray(availableBytes)
                        inStream.read(buffer, 0, availableBytes)
                        responseString = String(buffer)
                    }
                }
            } catch (e: java.lang.Exception) {
                Log.d(TAG, e.toString())
                responseString = ""
            }
            responseString
        }
        processReceivedResult(myCoroutineRes.await())
    }

    private fun processReceivedResult(res: String) {
        try {
            responseJsonObject = JSONObject(res)
            if(responseJsonObject.getString("espResult") == "done"){
                when(myJsonObject.getString("androidReq")){
                    "gun1" ->{
                        val tempVoltage = responseJsonObject.getString("voltage")
                        ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                    }
                    "gun3" -> {
                        if (myJsonObject.getString("state") == "on") {
                            gun3State = true //its on
                        }else if (myJsonObject.getString("state") == "off") {
                            gun3State = false //its off
                        }
                    }
                }
                ma_tv_connectionState.text = getString(R.string.connection_state_connected)
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
            }else if(responseJsonObject.getString("espResult") == "fail"){
                if(myJsonObject.getString("androidReq") == "gun3") {
                    gun3State = false
                    ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                }
                ma_tv_connectionState.text = getString(R.string.connection_state_problem)
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
            }
        } catch (e: java.lang.Exception) {
            if(myJsonObject.getString("androidReq") == "gun3") {
                gun3State = false
                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
            }
        }
    }*/

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
