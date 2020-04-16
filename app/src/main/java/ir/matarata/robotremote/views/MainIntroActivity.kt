package ir.matarata.robotremote.views

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage
import ir.matarata.robotremote.R
import ir.matarata.robotremote.utils.Tools

class MainIntroActivity : AppIntro() {

    private val sliderPage1 = SliderPage()
    private val sliderPage2 = SliderPage()
    private val sliderPage3 = SliderPage()
    private val sliderPage4 = SliderPage()
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val sawIntroActivity = sharedPref.getBoolean(getString(R.string.saw_intro_key), false)
        if(sawIntroActivity){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        with(sliderPage1){
            title = getString(R.string.intro1_title)
            description = getString(R.string.intro1_description)
            imageDrawable = R.drawable.app_description_intro
            bgColor = ContextCompat.getColor(applicationContext, R.color.lime_intro)
        }

        with(sliderPage2){
            title = getString(R.string.intro2_title)
            description = getString(R.string.intro2_description)
            imageDrawable = R.drawable.board_description_intro
            bgColor = ContextCompat.getColor(applicationContext, R.color.teal_intro)
        }

        with(sliderPage3){
            title = getString(R.string.intro3_title)
            description = getString(R.string.intro3_description)
            imageDrawable = R.drawable.board_purchase_intro
            bgColor = ContextCompat.getColor(applicationContext, R.color.light_blue_intro)
        }

        with(sliderPage4){
            title = getString(R.string.intro4_title)
            description = getString(R.string.intro4_description)
            imageDrawable = R.drawable.more_description_intro
            bgColor = ContextCompat.getColor(applicationContext, R.color.dark_blue_intro)
        }

        addSlide(AppIntroFragment.newInstance(sliderPage1))
        addSlide(AppIntroFragment.newInstance(sliderPage2))
        addSlide(AppIntroFragment.newInstance(sliderPage3))
        addSlide(AppIntroFragment.newInstance(sliderPage4))

        showSkipButton(false)
        setFadeAnimation()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        with (sharedPref.edit()) {
            putBoolean(getString(R.string.saw_intro_key), true)
            commit()
        }
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    override fun onSlideChanged(oldFragment: Fragment?, newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        when(newFragment?.tag.toString()){
            "android:switcher:2131231086:0" -> {
                Tools.setSystemBarColor(this, R.color.lime_intro) //change the color of system bar
            }
            "android:switcher:2131231086:1" -> {
                Tools.setSystemBarColor(this, R.color.teal_intro) //change the color of system bar
            }
            "android:switcher:2131231086:2" -> {
                Tools.setSystemBarColor(this, R.color.light_blue_intro) //change the color of system bar
            }
            "android:switcher:2131231086:3" -> {
                Tools.setSystemBarColor(this, R.color.dark_blue_intro) //change the color of system bar
            }
        }
    }

    override fun onResume() {
        super.onResume()
        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        val sawIntroActivity = sharedPref.getBoolean(getString(R.string.saw_intro_key), false)
        if(sawIntroActivity){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

}
