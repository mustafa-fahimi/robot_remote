package ir.matarata.robotremote

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.neovisionaries.ws.client.*
import com.yarolegovich.lovelydialog.LovelyInfoDialog
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.coroutines.*
import org.json.JSONObject

class SettingActivity : AppCompatActivity() {

    private lateinit var handler: CoroutineExceptionHandler //handle exceptions of coroutines
    private lateinit var mSocket: WebSocket //the main socket
    private lateinit var myJsonObject: JSONObject //json object to send to Esp
    private lateinit var responseJsonObject: JSONObject //json object to receive data from Esp
    private val socketURL = "ws://192.168.4.1:80" //WebSocket url
    private lateinit var websSocketFactory: WebSocketFactory //WebSocket factory
    private lateinit var progressDialog: AlertDialog //variable to store progress dialog
    private var mDialog: Dialog? = null //variable to store alert dialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        Utils.setSystemBarColor(this, R.color.darker_blue) //change the color of system bar
        socketCreate() //create socket for first time app start
        handler = CoroutineExceptionHandler { _, Throwable ->
            showInfoDialog(
                R.color.dark_red_color,
                R.drawable.ic_fail_white_50dp,
                getString(R.string.dialog_btn),
                getString(R.string.fail_dialog_title),
                getString(R.string.fail_dialog_message)
            ) //show alert dialog for fail
        }

        sa_btn_submit.setOnClickListener {
            when {
                //check password editText is not empty and both are equal and bigger than 8 character
                sa_et_newPassword.text.isNullOrEmpty() -> {
                    sa_et_newPassword.error = "نباید خالی باشد"
                    return@setOnClickListener
                }
                sa_et_newPassword.text.toString().length < 8 -> {
                    sa_et_newPassword.error = "حداقل 8 کاراکتر باشد"
                    return@setOnClickListener
                }
                sa_et_newPasswordAgain.text.toString() != sa_et_newPassword.text.toString() -> {
                    sa_et_newPasswordAgain.error = "تکرار رمز اشتباه است"
                    return@setOnClickListener
                }
            }
            progressDialog() //show progress dialog
            val newPassword = sa_et_newPassword.text.toString() //store the new password user entered in variable
            //prepare json to send for Esp
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MataSecToken")
            myJsonObject.put("androidReq", "changePassword")
            myJsonObject.put("newPassword", newPassword)
            socketSendData(myJsonObject)
        }

    }

    //Method for create a new socket and append the listener to it
    private fun socketCreate() {
        websSocketFactory = WebSocketFactory().setConnectionTimeout(2000) //socket timeout
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
                    progressDialog.dismiss()
                    this.cancel() //cancel the coroutine
                    showInfoDialog(
                        R.color.red_color,
                        R.drawable.ic_wifi_white_50dp,
                        getString(R.string.dialog_btn),
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
                    delay(2500)
                    progressDialog.cancel()
                    sa_et_newPassword.text?.clear()
                    sa_et_newPasswordAgain.text?.clear()
                    showInfoDialog(
                        R.color.colorAccent,
                        R.drawable.ic_done_white_50dp,
                        getString(R.string.dialog_btn),
                        getString(R.string.success_dialog_title),
                        getString(R.string.success_dialog_message)
                    ) //show alert dialog for success
                } else if (responseJsonObject.getString("espResult") == "fail") {
                    //response from Esp is "fail"
                    showInfoDialog(
                        R.color.dark_red_color,
                        R.drawable.ic_fail_white_50dp,
                        getString(R.string.dialog_btn),
                        getString(R.string.fail_dialog_title),
                        getString(R.string.fail_dialog_message)
                    ) //show alert dialog for fail
                }
            } catch (e: Exception) {
                //didn't get a response from Esp
                showInfoDialog(
                    R.color.dark_red_color,
                    R.drawable.ic_fail_white_50dp,
                    getString(R.string.dialog_btn),
                    getString(R.string.fail_dialog_title),
                    getString(R.string.fail_dialog_message)
                ) //show alert dialog for fail
            }
        }
    }

    //create and show the alert dialog for wifi problem
    private fun showInfoDialog(topColor: Int, icon: Int, btnText: String, title: String, message: String) {
        progressDialog.dismiss()
        //create a coroutine in Main thread and append handler to it
        CoroutineScope(Dispatchers.Main + handler).launch {
            if (mDialog == null) {
                //its the first time to showing dialog so we create it here and show
                mDialog = LovelyInfoDialog(this@SettingActivity)
                    .setTopColorRes(topColor)
                    .setIcon(icon)
                    .setConfirmButtonText(btnText)
                    .setTitle(title)
                    .setMessage(message)
                    .show()
            } else {
                if (!mDialog!!.isShowing) {
                    //there isn't another dialog showing so we show dialog
                    mDialog = LovelyInfoDialog(this@SettingActivity)
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

    //show the progress dialog
    private fun progressDialog() {
        progressDialog = SpotsDialog.Builder()
            .setContext(this)
            .setMessage(R.string.progress_dialog_title)
            .setCancelable(false)
            .build()
            .apply {
                show()
            }
    }

    override fun onPause() {
        super.onPause()
        finish()
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
