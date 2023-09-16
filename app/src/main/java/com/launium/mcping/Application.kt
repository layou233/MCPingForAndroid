package com.launium.mcping

import android.app.Application
import android.content.Context

class Application : Application() {

    companion object {
        lateinit var instance: Context
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        instance = this
    }

}