package ir.matarata.robotremote

import android.app.Application

//This class is for using application context any where in the app package
class MyApplication : Application() {

    companion object {
        lateinit var myApplication: MyApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        myApplication = this
    }

}