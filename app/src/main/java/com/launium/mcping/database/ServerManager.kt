package com.launium.mcping.database

import androidx.room.Room
import com.launium.mcping.Application
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object ServerManager {

    private val instance by lazy {
        Room
            .databaseBuilder(
                Application.instance,
                ServerDatabase::class.java,
                "servers.db"
            )
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .setQueryExecutor { GlobalScope.launch { it.run() } }
            .allowMainThreadQueries()
            .build()
    }

    val serverDao get() = instance.serverDao()

}