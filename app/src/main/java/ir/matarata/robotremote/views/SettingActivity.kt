package ir.matarata.robotremote.views

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.neovisionaries.ws.client.*
import com.varunest.sparkbutton.SparkEventListener
import dmax.dialog.SpotsDialog
import ir.matarata.robotremote.R
import ir.matarata.robotremote.models.RelaysEntity
import ir.matarata.robotremote.utils.Tools
import ir.matarata.robotremote.viewmodels.RelaysVM
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.android.synthetic.main.relay_setting_dialog.*
import kotlinx.android.synthetic.main.wifi_name_dialog.*
import kotlinx.android.synthetic.main.wifi_pass_dialog.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import libs.mjn.prettydialog.PrettyDialog
import org.json.JSONObject


class SettingActivity : AppCompatActivity() {

    private lateinit var handler: CoroutineExceptionHandler //handle exceptions of coroutines
    private lateinit var mSocket: WebSocket //the main socket
    private lateinit var myJsonObject: JSONObject //json object to send to Esp
    private lateinit var responseJsonObject: JSONObject //json object to receive data from Esp
    private val socketURL = "ws://192.168.4.1:80" //WebSocket url
    private lateinit var websSocketFactory: WebSocketFactory //WebSocket factory
    private lateinit var progressDialog: AlertDialog //variable to store progress dialog
    private lateinit var prettyDialog: PrettyDialog
    private lateinit var customDialog: Dialog
    private lateinit var customLayoutParam: WindowManager.LayoutParams
    private var tempRelayType = "None"
    private lateinit var relaysVM: RelaysVM //ViewModel instance
    private lateinit var relaysData: List<RelaysEntity> //list variable to store all relays data

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        Tools.setSystemBarColor(this, R.color.grey_800) //change the color of system bar
        socketCreate() //create socket for first time app start
        handler = CoroutineExceptionHandler { _, _ ->
            showInfoDialog(
                getString(R.string.dialog_btn_confirm),
                getString(R.string.fail_dialog_title),
                getString(R.string.fail_dialog_message),
                "fail"
            ) //show alert dialog for fail
        }
        relaysVM = ViewModelProvider(this).get(RelaysVM::class.java)
        relaysVM.allRelaysData.observe(this, Observer { data ->
            data?.let {
                if (!data.isNullOrEmpty()) {
                    relaysData = data
                }
            }
        })

        sa_change_wifi_name_tv.setOnClickListener {
            showChangeWifiNameDialog()
        }
        sa_change_wifi_pass_tv.setOnClickListener {
            showChangeWifiPassDialog()
        }
        sa_relay1_settings_tv.setOnClickListener {
            showRelaySettingsDialog(1)
        }
        sa_relay2_settings_tv.setOnClickListener {
            showRelaySettingsDialog(2)
        }
        sa_relay3_settings_tv.setOnClickListener {
            showRelaySettingsDialog(3)
        }
        sa_about_us_btn.setOnClickListener {
            startActivity(Intent(this, AboutUsActivity::class.java))
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
                    //this.cancel() //cancel the coroutine
                    showInfoDialog(
                        getString(R.string.dialog_btn_confirm),
                        getString(R.string.wifi_dialog_title),
                        getString(R.string.wifi_dialog_message),
                        "wifi_fail"
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
                    delay(2000)
                    customDialog.dismiss()
                    //sa_et_newPassword.text?.clear()
                    //sa_et_newPasswordAgain.text?.clear()
                    showInfoDialog(
                        getString(R.string.dialog_btn_confirm),
                        getString(R.string.success_dialog_title),
                        getString(R.string.success_dialog_message),
                        "success"
                    ) //show alert dialog for success
                } else if (responseJsonObject.getString("espResult") == "fail") {
                    //response from Esp is "fail"
                    showInfoDialog(
                        getString(R.string.dialog_btn_confirm),
                        getString(R.string.fail_dialog_title),
                        getString(R.string.fail_dialog_message),
                        "fail"
                    ) //show alert dialog for fail
                }
            } catch (e: Exception) {
                //didn't get a response from Esp
                showInfoDialog(
                    getString(R.string.dialog_btn_confirm),
                    getString(R.string.fail_dialog_title),
                    getString(R.string.fail_dialog_message),
                    "fail"
                ) //show alert dialog for fail
            }
        }
    }

    //create and show the alert dialog for wifi problem
    private fun showInfoDialog(btnText: String, title: String, message: String, type: String) {
        //create a coroutine in Main thread and append handler to it
        CoroutineScope(Dispatchers.Main + handler).launch {
            progressDialog.cancel()
            prettyDialog = PrettyDialog(this@SettingActivity)
            when (type) {
                "success" -> {
                    prettyDialog.setTitle(title)
                        .setMessage(message)
                        .setIcon(R.drawable.pdlg_icon_success)
                        .setIconTint(R.color.pdlg_color_green)
                        .addButton(
                            btnText,
                            R.color.white,
                            R.color.pdlg_color_green
                        ) {
                            prettyDialog.dismiss()
                        }.show()
                }
                "fail" -> {
                    prettyDialog.setTitle(title)
                        .setMessage(message)
                        .setIcon(R.drawable.pdlg_icon_close)
                        .setIconTint(R.color.pdlg_color_red)
                        .addButton(
                            btnText,
                            R.color.white,
                            R.color.pdlg_color_green
                        ) {
                            prettyDialog.dismiss()
                        }.show()
                }
                "wifi_fail" -> {
                    prettyDialog.setTitle(title)
                        .setMessage(message)
                        .setIcon(R.drawable.pdlg_icon_info)
                        .setIconTint(R.color.pdlg_color_blue)
                        .addButton(
                            btnText,
                            R.color.white,
                            R.color.pdlg_color_green
                        ) {
                            prettyDialog.dismiss()
                        }.show()
                }
            }
        }
    }

    private fun showChangeWifiPassDialog() {
        customDialog = Dialog(this)
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        customDialog.setContentView(R.layout.wifi_pass_dialog)
        customDialog.setCancelable(false)
        customLayoutParam = WindowManager.LayoutParams()
        customLayoutParam.copyFrom(customDialog.window!!.attributes)
        customLayoutParam.width = WindowManager.LayoutParams.MATCH_PARENT
        customLayoutParam.height = WindowManager.LayoutParams.WRAP_CONTENT
        customDialog.wpd_btn_submit.setOnClickListener {
            when {
                //check password editText is not empty and both are equal and bigger than 8 character
                customDialog.wpd_et_newPassword.text.isNullOrEmpty() -> {
                    customDialog.wpd_et_newPassword.error = "نباید خالی باشد"
                    return@setOnClickListener
                }
                customDialog.wpd_et_newPassword.text.toString().length < 8 -> {
                    customDialog.wpd_et_newPassword.error = "حداقل 8 کاراکتر باشد"
                    return@setOnClickListener
                }
                customDialog.wpd_et_newPasswordAgain.text.toString() != customDialog.wpd_et_newPasswordAgain.text.toString() -> {
                    customDialog.wpd_et_newPasswordAgain.error = "تکرار رمز اشتباه است"
                    return@setOnClickListener
                }
            }
            progressDialog() //show progress dialog
            val newPassword = customDialog.wpd_et_newPassword.text.toString() //store the new password user entered in variable
            //prepare json to send for Esp
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MataSecToken")
            myJsonObject.put("androidReq", "changeWifiPass")
            myJsonObject.put("newWifiPass", newPassword)
            socketSendData(myJsonObject)
        }
        customDialog.wpd_btn_cancel.setOnClickListener {
            customDialog.dismiss()
        }
        customDialog.show()
        customDialog.window!!.attributes = customLayoutParam
    }

    private fun showChangeWifiNameDialog() {
        customDialog = Dialog(this)
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        customDialog.setContentView(R.layout.wifi_name_dialog)
        customDialog.setCancelable(false)
        customLayoutParam = WindowManager.LayoutParams()
        customLayoutParam.copyFrom(customDialog.window!!.attributes)
        customLayoutParam.width = WindowManager.LayoutParams.MATCH_PARENT
        customLayoutParam.height = WindowManager.LayoutParams.WRAP_CONTENT
        customDialog.wnd_btn_submit.setOnClickListener {
            when {
                //check name editText is not empty and both are equal and bigger than 8 character
                customDialog.wnd_et_newName.text.isNullOrEmpty() -> {
                    customDialog.wnd_et_newName.error = "نباید خالی باشد"
                    return@setOnClickListener
                }
                customDialog.wnd_et_newName.text.toString().length < 4 -> {
                    customDialog.wnd_et_newName.error = "حداقل 4 کاراکتر باشد"
                    return@setOnClickListener
                }
            }
            progressDialog() //show progress dialog
            val newName = customDialog.wnd_et_newName.text.toString() //store the new password user entered in variable
            //prepare json to send for Esp
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MataSecToken")
            myJsonObject.put("androidReq", "changeWifiName")
            myJsonObject.put("newWifiName", newName)
            socketSendData(myJsonObject)
        }
        customDialog.wnd_btn_cancel.setOnClickListener {
            customDialog.cancel()
        }
        customDialog.show()
        customDialog.window!!.attributes = customLayoutParam
    }

    private fun showRelaySettingsDialog(whichRelay: Int) {
        tempRelayType = "None"
        customDialog = Dialog(this)
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        customDialog.setContentView(R.layout.relay_setting_dialog)
        customDialog.setCancelable(false)
        customLayoutParam = WindowManager.LayoutParams()
        customLayoutParam.copyFrom(customDialog.window!!.attributes)
        customLayoutParam.width = WindowManager.LayoutParams.MATCH_PARENT
        customLayoutParam.height = WindowManager.LayoutParams.WRAP_CONTENT
        when(whichRelay){
            1 -> {
                customDialog.rsd_relay_name_et.setText(relaysData[0].relayTitle)
                when (relaysData[0].relayType) {
                    "single" -> {
                        tempRelayType = "single"
                        customDialog.rsd_single_shot_btn.isChecked = true
                    }
                    "multi" -> {
                        tempRelayType = "multi"
                        customDialog.rsd_multi_shot_btn.isChecked = true
                    }
                    "switch" -> {
                        tempRelayType = "switch"
                        customDialog.rsd_switch_btn.isChecked = true
                    }
                }
            }
            2 -> {
                customDialog.rsd_relay_name_et.setText(relaysData[1].relayTitle)
                when (relaysData[1].relayType) {
                    "single" -> {
                        tempRelayType = "single"
                        customDialog.rsd_single_shot_btn.isChecked = true
                    }
                    "multi" -> {
                        tempRelayType = "multi"
                        customDialog.rsd_multi_shot_btn.isChecked = true
                    }
                    "switch" -> {
                        tempRelayType = "switch"
                        customDialog.rsd_switch_btn.isChecked = true
                    }
                }
            }
            3 -> {
                customDialog.rsd_relay_name_et.setText(relaysData[2].relayTitle)
                when (relaysData[2].relayType) {
                    "single" -> {
                        tempRelayType = "single"
                        customDialog.rsd_single_shot_btn.isChecked = true
                    }
                    "multi" -> {
                        tempRelayType = "multi"
                        customDialog.rsd_multi_shot_btn.isChecked = true
                    }
                    "switch" -> {
                        tempRelayType = "switch"
                        customDialog.rsd_switch_btn.isChecked = true
                    }
                }
            }
        }
        customDialog.rsd_root_cl.requestFocus()
        customDialog.rsd_single_shot_btn.setEventListener(object : SparkEventListener {
            override fun onEventAnimationEnd(button: ImageView?, buttonState: Boolean) {}
            override fun onEventAnimationStart(button: ImageView?, buttonState: Boolean) {}
            override fun onEvent(button: ImageView?, buttonState: Boolean) {
                if (buttonState) {
                    //here single check button turned on
                    tempRelayType = "single"
                    customDialog.rsd_multi_shot_btn.isChecked = false
                    customDialog.rsd_switch_btn.isChecked = false
                } else {
                    //here single check button turned off
                    tempRelayType = "None"
                    customDialog.rsd_multi_shot_btn.isChecked = false
                    customDialog.rsd_switch_btn.isChecked = false
                }
            }
        })
        customDialog.rsd_multi_shot_btn.setEventListener(object : SparkEventListener {
            override fun onEventAnimationEnd(button: ImageView?, buttonState: Boolean) {}
            override fun onEventAnimationStart(button: ImageView?, buttonState: Boolean) {}
            override fun onEvent(button: ImageView?, buttonState: Boolean) {
                if (buttonState) {
                    //here multi check button turned on
                    tempRelayType = "multi"
                    customDialog.rsd_single_shot_btn.isChecked = false
                    customDialog.rsd_switch_btn.isChecked = false
                } else {
                    //here multi check button turned off
                    tempRelayType = "None"
                    customDialog.rsd_single_shot_btn.isChecked = false
                    customDialog.rsd_switch_btn.isChecked = false
                }
            }
        })
        customDialog.rsd_switch_btn.setEventListener(object : SparkEventListener {
            override fun onEventAnimationEnd(button: ImageView?, buttonState: Boolean) {}
            override fun onEventAnimationStart(button: ImageView?, buttonState: Boolean) {}
            override fun onEvent(button: ImageView?, buttonState: Boolean) {
                if (buttonState) {
                    //here switch check button turned on
                    tempRelayType = "switch"
                    customDialog.rsd_single_shot_btn.isChecked = false
                    customDialog.rsd_multi_shot_btn.isChecked = false
                } else {
                    //here switch check button turned off
                    tempRelayType = "None"
                    customDialog.rsd_single_shot_btn.isChecked = false
                    customDialog.rsd_multi_shot_btn.isChecked = false
                }
            }
        })
        customDialog.rsd_save_btn.setOnClickListener {
            if (customDialog.rsd_relay_name_et.text.isNullOrEmpty()) {
                customDialog.rsd_relay_name_et.error = "نباید خالی باشد"
                return@setOnClickListener
            } else if (customDialog.rsd_relay_name_et.text.toString().length < 5) {
                customDialog.rsd_relay_name_et.error = "حداقل 5 کاراکتر باشد"
                return@setOnClickListener
            } else if (tempRelayType == "None") {
                return@setOnClickListener
            }
            progressDialog() //show progress dialog
            val tempUpdateRelays = RelaysEntity(whichRelay, customDialog.rsd_relay_name_et.text.toString(), tempRelayType)
            CoroutineScope(Dispatchers.IO + handler).launch {
                relaysVM.updateRelay(tempUpdateRelays).collect {
                    if (it == 1) {
                        progressDialog.dismiss()
                        customDialog.dismiss()
                        showInfoDialog(
                            getString(R.string.dialog_btn_confirm),
                            getString(R.string.success_dialog_title),
                            getString(R.string.success_dialog_message2),
                            "success"
                        ) //show alert dialog for success
                    } else {
                        progressDialog.dismiss()
                        showInfoDialog(
                            getString(R.string.dialog_btn_confirm),
                            getString(R.string.fail_dialog_title),
                            getString(R.string.fail_dialog_message),
                            "fail"
                        ) //show alert dialog for fail
                    }
                }
            }
        }
        customDialog.rsd_cancel_btn.setOnClickListener {
            customDialog.dismiss()
        }
        customDialog.show()
        customDialog.window!!.attributes = customLayoutParam
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

    override fun onBackPressed() {
        super.onBackPressed()
        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
