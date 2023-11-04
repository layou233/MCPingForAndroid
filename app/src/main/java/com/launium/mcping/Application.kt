package com.launium.mcping

import android.app.Application
import android.content.Context
import io.ktor.network.selector.SelectorManager
import kotlinx.coroutines.Dispatchers

class Application : Application() {

    companion object {
        lateinit var instance: Application

        val ktorSelectorManager by lazy { SelectorManager(Dispatchers.IO) }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(newBase)

        instance = this
    }

}