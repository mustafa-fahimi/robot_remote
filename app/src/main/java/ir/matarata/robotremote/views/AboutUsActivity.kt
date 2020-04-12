package ir.matarata.robotremote.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import ir.matarata.robotremote.R
import ir.matarata.robotremote.utils.Tools

class AboutUsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_us)

        Tools.setSystemBarColor(this, R.color.about_us_bg) //change the color of system bar
    }

    override fun onBackPressed() {
        super.onBackPressed()

    }

}
