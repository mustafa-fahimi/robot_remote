package ir.matarata.robotremote

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_setting.*
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.lang.Exception
import java.net.Socket

class SettingActivity : AppCompatActivity() {

    private lateinit var handler: CoroutineExceptionHandler
    private var socket: Socket = Socket()
    private lateinit var myCoroutineRes: Deferred<String>
    private lateinit var myJsonObject: JSONObject
    private lateinit var outStream: OutputStream
    private lateinit var inStream: InputStream
    private lateinit var printWriter: PrintWriter
    private var responseString = ""
    private var availableBytes: Int = 0
    private lateinit var buffer: ByteArray

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        handler = CoroutineExceptionHandler { _, throwable ->
            Log.d(MainActivity.TAG, "handler: $throwable")
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
            val newPassword = sa_et_newPassword.text.toString()
            myJsonObject = JSONObject()
            myJsonObject.put("token", "MatarataSecretToken1994")
            myJsonObject.put("androidReq", "changePassword")
            myJsonObject.put("newPassword", newPassword)
            socketSendReceive(myJsonObject)
        }

    }

    private fun socketSendReceive(jsonObj: JSONObject) = runBlocking(handler) {
        myCoroutineRes = GlobalScope.async(handler) {
            try {
                socket = Socket(MainActivity.socketURL, MainActivity.socketPort)
                socket.use {
                    outStream = it.getOutputStream()
                    printWriter = PrintWriter(outStream)
                    printWriter.print(jsonObj.toString())
                    printWriter.flush()
                    responseString = ""
                    availableBytes = 0
                    Thread.sleep(10)
                    inStream = it.getInputStream()
                    availableBytes = inStream.available()
                    if(availableBytes > 0){
                        buffer = ByteArray(availableBytes)
                        inStream.read(buffer, 0, availableBytes)
                        responseString = String(buffer)
                    }
                }
            }catch (e: Exception){
                Log.d(MainActivity.TAG, e.toString())
                responseString = ""
            }
            responseString
        }
        Log.d(MainActivity.TAG, myCoroutineRes.await())
    }

    /*private fun volleyJsonReq(jsonObj: JSONObject) {
        val requestQueue = Volley.newRequestQueue(this)
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.POST,
            nodeMcuURL,
            jsonObj,
            Response.Listener<JSONObject?> { response ->
                val res: String? = response?.getString("result")
                if (res.equals("done")) {
                    //TODO: show alert dialog for success
                    sa_et_newPassword.text?.clear()
                    sa_et_newPasswordAgain.text?.clear()
                    Toast.makeText(this, "done",Toast.LENGTH_SHORT).show()
                } else {
                    //TODO: show alert dialog for failure
                    Toast.makeText(this, response.toString(),Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener {
                //TODO: show alert dialog for failure
                Toast.makeText(this, it.toString(),Toast.LENGTH_SHORT).show()
            }
        )
        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(
            1000,
            1,
            2F
        )
        requestQueue.add(jsonObjectRequest)
    }*/

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
