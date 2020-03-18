package ir.matarata.robotremote

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neovisionaries.ws.client.*
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.json.JSONObject
import kotlin.math.log

class MainActivity : AppCompatActivity() {

    private var gun3State = false //state variable for changing the color of gun3
    private var joystickState = "" //state of joystick for sending to Esp
    private var onPaused = false //variable to determine if activity paused or stop
    private lateinit var handler: CoroutineExceptionHandler //handle exceptions of coroutines
    private lateinit var mSocket: WebSocket //the main socket
    private lateinit var myJsonObject: JSONObject //json object to send to Esp
    private lateinit var responseJsonObject: JSONObject //json object to receive data from Esp
    private val socketURL = "ws://192.168.4.1:80" //WebSocket url
    private lateinit var websSocketFactory: WebSocketFactory //WebSocket factory
    private var tempVoltage: Double = 0.0 //variable to store Esp voltage
    private var mDialog: Dialog? = null //variable to store alert dialog
    private var joystickOffFlag = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Utils.setSystemBarColor(this, R.color.darker_blue) //change the color of system bar
        socketCreate() //create socket for first time app start
        handler = CoroutineExceptionHandler { _, _ ->
            //change the text and textColor of connection state
            ma_tv_connectionState.text = getString(R.string.connection_state_problem)
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
        }

        ma_btn_gun1.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    //change the background color of gun1 to green
                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    //prepare json to send for Esp
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "gun1")
                    socketSendData(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    //change the background color of gun1 to red
                    ma_btn_gun1.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                }
            }
            true
        }
        ma_btn_gun2.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    //change the background color of gun2 to green
                    ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    //prepare json to send for Esp
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "gun2")
                    myJsonObject.put("state", "on")
                    socketSendData(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    //change the background color of gun2 to red
                    ma_btn_gun2.backgroundColor = ContextCompat.getColor(this, R.color.red_color)
                    //prepare json to send for Esp
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "gun2")
                    myJsonObject.put("state", "off")
                    socketSendData(myJsonObject)
                }
            }
            true
        }
        ma_btn_gun3.setOnClickListener {
            if (!gun3State) {
                //prepare json to send for Esp
                myJsonObject = JSONObject()
                myJsonObject.put("token", "MataSecToken")
                myJsonObject.put("androidReq", "gun3")
                myJsonObject.put("state", "on")
                socketSendData(myJsonObject)
            } else if (gun3State) {
                //prepare json to send for Esp
                myJsonObject = JSONObject()
                myJsonObject.put("token", "MataSecToken")
                myJsonObject.put("androidReq", "gun3")
                myJsonObject.put("state", "off")
                socketSendData(myJsonObject)
            }
        }
        ma_btn_settings.setOnClickListener {
            //send socket close request to Esp and then disconnect
            mSocket.sendClose()
            mSocket.disconnect()
            intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            finish()
        }
        ma_root_cl.setOnTouchListener { v, event ->
            Log.d(Utils.TAG, "touch")
            false
        }
        ma_jsv_joystick.setOnMoveListener({ angle, strength ->
            joystickState = ""
            if (onPaused) {
                //If user playing with joysticks and suddenly hit Home or Recent button in his device then in onPause and onStop method we set the onPaused variable to true
                // here we send motor_off request to Esp
                joystickState = "motor_off"
                myJsonObject = JSONObject()
                myJsonObject.put("token", "MataSecToken")
                myJsonObject.put("androidReq", joystickState)
                socketSendData(myJsonObject)
                return@setOnMoveListener
            }
            //fill out joystickState variable base on joystick angle to send to Esp
            joystickState = if(strength <= 13 && joystickOffFlag){
                joystickOffFlag = false
                "motor_off"
            }else if (strength > 13 && (angle in 315..360 || angle in 0..45)) {
                joystickOffFlag = true
                "motor_right"
            } else if (strength > 13 && angle in 45..135) {
                joystickOffFlag = true
                "motor_forward"
            } else if (strength > 13 && angle in 135..225) {
                joystickOffFlag = true
                "motor_left"
            } else if (strength > 13 && angle in 225..315) {
                joystickOffFlag = true
                "motor_backward"
            }else{
                ""
            }
            //prepare json to send for Esp
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MataSecToken")
            myJsonObject.put("androidReq", joystickState)
            socketSendData(myJsonObject)
        }, 150) //Loop interval of joystick
    }

    //Method for create a new socket and append the listener to it
    private fun socketCreate() {
        websSocketFactory = WebSocketFactory().setConnectionTimeout(1200) //socket timeout
        mSocket = websSocketFactory.createSocket(socketURL) //create socket
        //socket listener
        mSocket.addListener(object : WebSocketAdapter() {
            override fun onTextMessage(websocket: WebSocket?, text: String?) {
                if (text.toString() != "null")
                    processReceivedResult(text.toString())
            }

            override fun onError(websocket: WebSocket?, cause: WebSocketException?) {
                mSocket.sendClose()
                mSocket.disconnect()
            }

            override fun onUnexpectedError(websocket: WebSocket?, cause: WebSocketException?) {
                mSocket.sendClose()
                mSocket.disconnect()
            }

            override fun onDisconnected(websocket: WebSocket?, serverCloseFrame: WebSocketFrame?, clientCloseFrame: WebSocketFrame?, closedByServer: Boolean) {
                mSocket.sendClose()
                mSocket.disconnect()
            }
        })
    }

    //call the create socket if needed and then send data to Esp using socket connection
    private fun socketSendData(jsonObj: JSONObject) {
        if (mSocket.state.toString() == "CLOSED") {
            socketCreate() //create a new socket if socket state is CLOSED
        }
        if (mSocket.state.toString() == "CONNECTING")
            return //return and do nothing if socket state is CONNECTING
        //launch a coroutine for multi threading and add a handler to it for exceptions
        CoroutineScope(Dispatchers.IO + handler).launch {
            if (mSocket.state.toString() == "CLOSED" || mSocket.state.toString() == "CREATED") {
                try {
                    mSocket.connect() //if socket is created before then try to connect to it
                } catch (e: java.lang.Exception) {
                    this.cancel() //cancel the coroutine
                    /*showInfoDialog(
                        R.color.red_color,
                        R.drawable.ic_wifi_white_50dp,
                        getString(R.string.dialog_btn),
                        getString(R.string.wifi_dialog_title),
                        getString(R.string.wifi_dialog_message)
                    ) //show the wifi problem dialog to user*/
                }
            }
            if (mSocket.isOpen) {
                mSocket.sendText(jsonObj.toString()) //send json object as a string to socket
            }
        }
    }

    //handle the response from Esp
    private fun processReceivedResult(res: String) {
        //launch a coroutine in Main thread and append a handler to it
        CoroutineScope(Dispatchers.Main + handler).launch {
            try {
                responseJsonObject = JSONObject(res) //create a json object from string response of Esp
                if (responseJsonObject.getString("espResult") == "done") {
                    //response from Esp is "done"
                    ma_tv_connectionState.text = getString(R.string.connection_state_connected) //change connection state textView
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorAccent)) //change connection state textView color
                    when (myJsonObject.getString("androidReq")) {
                        "gun2" -> {
                            //runs when current request was gun2
                            //store the voltage that comes from Esp and show it
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                        }
                        "gun3" -> {
                            //runs when current request was gun3
                            if (myJsonObject.getString("state") == "on") {
                                //current request of gun3 is "on" and run this
                                gun3State = true //set gun3 state variable to on
                                //store the voltage that comes from Esp and show it and change the background color of gun3 to green
                                tempVoltage = responseJsonObject.getDouble("voltage")
                                ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
                            } else if (myJsonObject.getString("state") == "off") {
                                gun3State = false //set gun3 state variable to on
                                ma_btn_gun3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.red_color) //change the background color of gun3 to red
                            }
                        }
                    }
                } else if (responseJsonObject.getString("espResult") == "fail") {
                    //response from Esp is "fail"
                    ma_tv_connectionState.text = getString(R.string.connection_state_problem) //change connection state textView
                    ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_color)) //change connection state textView color to red
                    if (myJsonObject.getString("androidReq") == "gun3") {
                        gun3State = false
                        ma_btn_gun3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.red_color)
                    }
                }
            } catch (e: Exception) {
                //didn't get a response from Esp
                ma_tv_connectionState.text = getString(R.string.connection_state_problem) //change connection state textView
                ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_color)) //change connection state textView color to red
                if (myJsonObject.getString("androidReq") == "gun3") {
                    gun3State = false //set gun3 state variable to on
                    ma_btn_gun3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.red_color) //change the background color of gun3 to red
                }
            }
        }
    }

    //create and show the alert dialog for wifi problem
    private fun showInfoDialog(topColor: Int, icon: Int, btnText: String, title: String, message: String) {
        //create a coroutine in Main thread and append handler to it
        CoroutineScope(Dispatchers.Main + handler).launch {
            ma_tv_connectionState.text = getString(R.string.connection_state_problem) //change connection state textView
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_color)) //change connection state textView color to red
            gun3State = false //set gun3 state variable to on
            ma_btn_gun3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.red_color) //change the background color of gun3 to red
            if (mDialog == null) {
                //its the first time to showing dialog so we create it here and show
                mDialog = LovelyInfoDialog(this@MainActivity)
                    .setTopColorRes(topColor)
                    .setIcon(icon)
                    .setConfirmButtonText(btnText)
                    .setTitle(title)
                    .setMessage(message)
                    .show()
            } else {
                if (!mDialog!!.isShowing) {
                    //there isn't another dialog showing so we show dialog
                    mDialog = LovelyInfoDialog(this@MainActivity)
                        .setTopColorRes(topColor)
                        .setIcon(icon)
                        .setConfirmButtonText(btnText)
                        .setTitle(title)
                        .setMessage(message)
                        .show()
                }
            }
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


