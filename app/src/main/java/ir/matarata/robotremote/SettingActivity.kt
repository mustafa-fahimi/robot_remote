package ir.matarata.robotremote

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.neovisionaries.ws.client.WebSocket
import com.neovisionaries.ws.client.WebSocketAdapter
import com.neovisionaries.ws.client.WebSocketFactory
import com.neovisionaries.ws.client.WebSocketFrame
import dmax.dialog.SpotsDialog
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject

class SettingActivity : AppCompatActivity() {

    private lateinit var handler: CoroutineExceptionHandler
    private lateinit var mSocket: WebSocket
    private lateinit var myJsonObject: JSONObject
    private lateinit var responseJsonObject: JSONObject
    private val socketURL = "ws://192.168.4.1:80"
    private lateinit var websSocketFactory: WebSocketFactory
    private lateinit var alertDialog: AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        handler = CoroutineExceptionHandler { _, Throwable ->
            Log.d(MainActivity.TAG, Throwable.toString())
        }

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
                sa_et_newPasswordAgain.text.toString() != sa_et_newPassword.text.toString() -> {
                    sa_et_newPasswordAgain.error = "تکرار رمز اشتباه است"
                    return@setOnClickListener
                }
            }
            //TODO: add a progress loader
            progressDialog(this)
            val newPassword = sa_et_newPassword.text.toString()
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MatarataSecretToken1994")
            myJsonObject.put("androidReq", "changePassword")
            myJsonObject.put("newPassword", newPassword)
            socketSendReceive(myJsonObject)
        }

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
                Thread.sleep(1500)
                alertDialog.cancel()
                sa_et_newPassword.text?.clear()
                sa_et_newPasswordAgain.text?.clear()
                //TODO: show alert dialog for success
            } else if (responseJsonObject.getString("espResult") == "fail") {
                //TODO: show alert dialog for failure
            }
        } catch (e: Exception) {
            //TODO: show alert dialog for failure
        }
    }

    private fun progressDialog(context: Context) {
        alertDialog = SpotsDialog.Builder()
            .setContext(context)
            .setMessage("منتظر بمانید ...")
            .setCancelable(false)
            .build()
            .apply {
                show()
            }
    }

    override fun onPause() {
        super.onPause()
        mSocket.disconnect()
        finish()
    }

    override fun onStop() {
        super.onStop()
        mSocket.disconnect()
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mSocket.disconnect()
        intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

}
