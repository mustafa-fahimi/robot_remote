package ir.matarata.robotremote.views

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.neovisionaries.ws.client.*
import com.ornach.nobobutton.NoboButton
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import ir.matarata.robotremote.R
import ir.matarata.robotremote.models.RelaysEntity
import ir.matarata.robotremote.utils.Tools
import ir.matarata.robotremote.viewmodels.RelaysVM
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    private var relay1State = false //state variable for changing the color of relay1
    private var relay2State = false //state variable for changing the color of relay2
    private var relay3State = false //state variable for changing the color of relay3
    private var onPaused = false //variable to determine if activity paused or stop
    private lateinit var handler: CoroutineExceptionHandler //handle exceptions of coroutines
    private lateinit var mSocket: WebSocket //the main socket
    private lateinit var myJsonObject: JSONObject //json object to send to Esp
    private lateinit var responseJsonObject: JSONObject //json object to receive data from Esp
    private val socketURL = "ws://192.168.4.1:80" //WebSocket url
    private lateinit var websSocketFactory: WebSocketFactory //WebSocket factory
    private var tempVoltage: Double = 0.0 //variable to store Esp voltage
    private var mDialog: Dialog? = null //variable to store alert dialog
    private lateinit var relaysVM: RelaysVM
    private lateinit var relaysData: List<RelaysEntity>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Tools.setSystemBarColor(this, R.color.grey_10) //change the color of system bar
        socketCreate() //create socket for first time app start
        handler = CoroutineExceptionHandler { _, _ ->
            //change the text and textColor of connection state
            ma_tv_connectionState.text = getString(R.string.connection_state_problem)
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this, R.color.red_color))
        }

        relaysVM = ViewModelProvider(this).get(RelaysVM::class.java)
        relaysVM.allRelaysData.observe(this, Observer { data ->
            data?.let {
                if(!data.isNullOrEmpty()){
                    relaysData = data
                    ma_btn_relay1.text = relaysData[0].relayTitle
                    ma_btn_relay2.text = relaysData[1].relayTitle
                    ma_btn_relay3.text = relaysData[2].relayTitle
                }
            }
        })

        ma_btn_relay1.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    relayButtonPressHandler(ma_btn_relay1, "relay1", relaysData[0].relayType, "pressed")
                }
                MotionEvent.ACTION_UP -> {
                    relayButtonPressHandler(ma_btn_relay1, "relay1", relaysData[0].relayType, "released")
                }
            }
            false
        }
        ma_btn_relay2.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    relayButtonPressHandler(ma_btn_relay2, "relay2", relaysData[1].relayType, "pressed")
                }
                MotionEvent.ACTION_UP -> {
                    relayButtonPressHandler(ma_btn_relay2, "relay2", relaysData[1].relayType, "released")
                }
            }
            false
        }
        ma_btn_relay3.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    relayButtonPressHandler(ma_btn_relay3, "relay3", relaysData[2].relayType, "pressed")
                }
                MotionEvent.ACTION_UP -> {
                    relayButtonPressHandler(ma_btn_relay3, "relay3", relaysData[2].relayType, "released")
                }
            }
            false
        }
        ma_btn_settings.setOnClickListener {
            //send socket close request to Esp and then disconnect
            mSocket.sendClose()
            mSocket.disconnect()
            intent = Intent(this, SettingActivity::class.java)
            startActivity(intent)
            finish()
        }
        ma_iv_up.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_forward")
                    socketSendData(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_off")
                    socketSendData(myJsonObject)
                }
            }
            false
        }
        ma_iv_down.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_backward")
                    socketSendData(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_off")
                    socketSendData(myJsonObject)
                }
            }
            false
        }
        ma_iv_right.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_right")
                    socketSendData(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_off")
                    socketSendData(myJsonObject)
                }
            }
            false
        }
        ma_iv_left.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_left")
                    socketSendData(myJsonObject)
                }
                MotionEvent.ACTION_UP -> {
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", "motor_off")
                    socketSendData(myJsonObject)
                }
            }
            false
        }
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
                    showInfoDialog(
                        R.color.red_color,
                        R.drawable.ic_wifi_white_50dp,
                        getString(R.string.dialog_btn_confirm),
                        getString(R.string.wifi_dialog_title),
                        getString(R.string.wifi_dialog_message)
                    ) //show the wifi problem dialog to user
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
                    successResponseFromEsp()
                } else if (responseJsonObject.getString("espResult") == "fail") {
                    //response from Esp is "fail"
                    failResponseFromEsp()
                }
            } catch (e: Exception) {
                //didn't get a response from Esp
                failResponseFromEsp()
            }
        }
    }

    //create and show the alert dialog for wifi problem
    private fun showInfoDialog(topColor: Int, icon: Int, btnText: String, title: String, message: String) {
        //create a coroutine in Main thread and append handler to it
        CoroutineScope(Dispatchers.Main + handler).launch {
            ma_tv_connectionState.text = getString(R.string.connection_state_problem) //change connection state textView
            ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_color)) //change connection state textView color to red
            relay3State = false //set relay3 state variable to on
            ma_btn_relay3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue) //change the background color of relay3 to red
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

    private fun relayButtonPressHandler(relayRelatedBtn: NoboButton, relayNumber: String, relayType: String, actionType: String) {
        var currentRelayState = false
        when (relayNumber) {
            "relay1" -> {
                currentRelayState = relay1State
            }
            "relay2" -> {
                currentRelayState = relay2State
            }
            "relay3" -> {
                currentRelayState = relay3State
            }
        }
        if (actionType == "pressed") {
            when (relayType) {
                "single" -> {
                    //change the background color of relay to green
                    relayRelatedBtn.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    //prepare json to send for Esp
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", relayNumber)
                    myJsonObject.put("relayType", "single")
                    socketSendData(myJsonObject)
                }
                "multi" -> {
                    //change the background color of relay to green
                    relayRelatedBtn.backgroundColor = ContextCompat.getColor(this, R.color.colorAccent)
                    //prepare json to send for Esp
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", relayNumber)
                    myJsonObject.put("relayType", "multi")
                    myJsonObject.put("state", "on")
                    socketSendData(myJsonObject)
                }
                "switch" -> {
                    if (!currentRelayState) {
                        //prepare json to send for Esp
                        myJsonObject = JSONObject()
                        myJsonObject.put("token", "MataSecToken")
                        myJsonObject.put("androidReq", relayNumber)
                        myJsonObject.put("relayType", "switch")
                        myJsonObject.put("state", "on")
                        socketSendData(myJsonObject)
                    } else if (currentRelayState) {
                        //prepare json to send for Esp
                        myJsonObject = JSONObject()
                        myJsonObject.put("token", "MataSecToken")
                        myJsonObject.put("androidReq", relayNumber)
                        myJsonObject.put("relayType", "switch")
                        myJsonObject.put("state", "off")
                        socketSendData(myJsonObject)
                    }
                }
            }
        } else if (actionType == "released") {
            when (relayType) {
                "single" -> {
                    //change the background color of relay to red
                    relayRelatedBtn.backgroundColor = ContextCompat.getColor(this, R.color.dark_blue)
                }
                "multi" -> {
                    //change the background color of relay to green
                    relayRelatedBtn.backgroundColor = ContextCompat.getColor(this, R.color.dark_blue)
                    //prepare json to send for Esp
                    myJsonObject = JSONObject()
                    myJsonObject.put("token", "MataSecToken")
                    myJsonObject.put("androidReq", relayNumber)
                    myJsonObject.put("relayType", "multi")
                    myJsonObject.put("state", "off")
                    socketSendData(myJsonObject)
                }
            }
        }
    }

    private fun successResponseFromEsp(){
        when (myJsonObject.getString("relayType")) {
            "single" -> {
                //store the voltage that comes from Esp and show it
                tempVoltage = responseJsonObject.getDouble("voltage")
                ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
            }
            "multi" -> {
                //store the voltage that comes from Esp and show it
                tempVoltage = responseJsonObject.getDouble("voltage")
                ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
            }
            "switch" -> {
                when(myJsonObject.getString("androidReq")){
                    "relay1" ->{
                        if (myJsonObject.getString("state") == "on") {
                            //current request of relay1 is "on" and run this
                            relay1State = true //set relay1 state variable to on
                            //store the voltage that comes from Esp and show it and change the background color of relay1 to green
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                            ma_btn_relay1.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
                        }else if (myJsonObject.getString("state") == "off") {
                            //current request of relay1 is "off" and run this
                            relay1State = false //set relay1 state variable to off
                            //store the voltage that comes from Esp and show it and change the background color of relay1 to green
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                            ma_btn_relay1.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue)
                        }
                    }
                    "relay2" -> {
                        if (myJsonObject.getString("state") == "on") {
                            //current request of relay2 is "on" and run this
                            relay2State = true //set relay2 state variable to on
                            //store the voltage that comes from Esp and show it and change the background color of relay2 to green
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                            ma_btn_relay2.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
                        }else if (myJsonObject.getString("state") == "off") {
                            //current request of relay2 is "off" and run this
                            relay2State = false //set relay2 state variable to off
                            //store the voltage that comes from Esp and show it and change the background color of relay2 to green
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                            ma_btn_relay2.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue)
                        }
                    }
                    "relay3" -> {
                        if (myJsonObject.getString("state") == "on") {
                            //current request of relay3 is "on" and run this
                            relay3State = true //set relay3 state variable to on
                            //store the voltage that comes from Esp and show it and change the background color of relay3 to green
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                            ma_btn_relay3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.colorAccent)
                        }else if (myJsonObject.getString("state") == "off") {
                            //current request of relay3 is "off" and run this
                            relay3State = false //set relay3 state variable to off
                            //store the voltage that comes from Esp and show it and change the background color of relay3 to green
                            tempVoltage = responseJsonObject.getDouble("voltage")
                            ma_tv_batteryVoltage.text = "ولتاژ باتری موتور: $tempVoltage ولت"
                            ma_btn_relay3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue)
                        }
                    }
                }
            }
        }
        ma_tv_connectionState.text = getString(R.string.connection_state_connected) //change connection state textView
        ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorAccent)) //change connection state textView color
    }

    private fun failResponseFromEsp(){
       //change connection state textView color to red
        if (myJsonObject.getString("relayType") == "switch") {
            when(myJsonObject.getString("androidReq")){
                "relay1" -> {
                    relay1State = false
                    ma_btn_relay1.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue)
                }
                "relay2" -> {
                    relay2State = false
                    ma_btn_relay2.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue)
                }
                "relay3" -> {
                    relay3State = false
                    ma_btn_relay3.backgroundColor = ContextCompat.getColor(this@MainActivity, R.color.dark_blue)
                }
            }
        }
        ma_tv_connectionState.text = getString(R.string.connection_state_problem) //change connection state textView
        ma_tv_connectionState.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.red_color))
        showInfoDialog(
            R.color.dark_red_color,
            R.drawable.ic_fail_white_50dp,
            getString(R.string.dialog_btn_save),
            getString(R.string.fail_dialog_title),
            getString(R.string.fail_dialog_message)
        ) //show alert dialog for fail
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
    }

    override fun onRestart() {
        super.onRestart()
        onPaused = false
    }

}


