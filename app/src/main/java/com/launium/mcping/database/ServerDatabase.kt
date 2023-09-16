package com.launium.mcping.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.launium.mcping.server.MinecraftServer

@Database(entities = [MinecraftServer::class], version = 1)
abstract class ServerDatabase : RoomDatabase() {
    abstract fun serverDao(): MinecraftServer.Dao
}