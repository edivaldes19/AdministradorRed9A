package com.manuel.administradorred

import android.app.Application
import com.manuel.administradorred.fcm.VolleyHelper

class AdministradorRedApplication : Application() {
    companion object {
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate() {
        super.onCreate()
        volleyHelper = VolleyHelper.getInstance(this)
    }
}