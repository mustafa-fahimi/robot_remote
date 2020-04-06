package ir.matarata.robotremote.views

import android.os.Bundle
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro
import com.github.paolorotolo.appintro.AppIntroFragment
import com.github.paolorotolo.appintro.model.SliderPage
import ir.matarata.robotremote.R


class MainIntroActivity : AppIntro() {

    private val sliderPage1 = SliderPage()
    private val sliderPage2 = SliderPage()
    private val sliderPage3 = SliderPage()
    private val sliderPage4 = SliderPage()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sliderPage1.title = getString(R.string.into1_title)
        sliderPage1.description = getString(R.string.intro1_description)
        sliderPage1.imageDrawable = R.drawable.app_description_intro
        sliderPage1.bgColor = ContextCompat.getColor(this, R.color.green_intro)

        sliderPage2.title = getString(R.string.into2_title)
        sliderPage2.description = getString(R.string.intro2_description)
        sliderPage2.imageDrawable = R.drawable.board_description_intro
        sliderPage2.bgColor = ContextCompat.getColor(this, R.color.teal_intro)

        sliderPage3.title = getString(R.string.into3_title)
        sliderPage3.description = getString(R.string.intro3_description)
        sliderPage3.imageDrawable = R.drawable.board_purchase_intro
        sliderPage3.bgColor = ContextCompat.getColor(this, R.color.light_blue_intro)

        sliderPage4.title = getString(R.string.into4_title)
        sliderPage4.description = getString(R.string.intro4_description)
        sliderPage4.imageDrawable = R.drawable.more_description_intro
        sliderPage4.bgColor = ContextCompat.getColor(this, R.color.dark_blue_intro)

        addSlide(AppIntroFragment.newInstance(sliderPage1))
        addSlide(AppIntroFragment.newInstance(sliderPage2))
        addSlide(AppIntroFragment.newInstance(sliderPage3))
        addSlide(AppIntroFragment.newInstance(sliderPage4))

        showSkipButton(false)
        setFadeAnimation()
    }

    override fun onDonePressed(currentFragment: Fragment?) {
        super.onDonePressed(currentFragment)
        // Do something when users tap on Done button.
    }

    override fun onSlideChanged(@Nullable oldFragment: Fragment?, @Nullable newFragment: Fragment?) {
        super.onSlideChanged(oldFragment, newFragment)
        // Do something when the slide changes.
    }

}
