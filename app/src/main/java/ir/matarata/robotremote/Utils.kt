package ir.matarata.robotremote

import android.app.Activity
import android.os.Build
import android.view.Window
import android.view.WindowManager
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

object Utils {

    const val TAG = "MATATAG" //Tag for logging

    //Change the color of system bar in APIs above lollipop
    fun setSystemBarColor(act: Activity, @ColorRes color: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = act.window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = ContextCompat.getColor(MyApplication.myApplication, color)
        }
    }

    //map a number in a range to a number of another range
    fun mapRange(range1: IntRange, range2: IntRange, value: Int): Int {
        return try {
            range2.start + (value - range1.start) * (range2.endInclusive - range2.start) / (range1.endInclusive - range1.start)
        } catch (e: Exception) {
            0
        }
    }

}

class IntRange(override val start: Int, override val endInclusive: Int) : ClosedRange<Int>