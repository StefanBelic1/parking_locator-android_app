package hr.ferit.belic.parking3.model

import android.app.Application
import com.google.android.libraries.places.api.Places

class Parking3Application : Application() {
    override fun onCreate() {
        super.onCreate()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyB2YiSlI12JU-lWNUFWEIETg4BW3PmOImU")
        }
    }
}