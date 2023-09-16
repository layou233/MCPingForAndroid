package com.launium.mcping.server

import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Base64
import androidx.core.graphics.drawable.toDrawable
import androidx.room.ColumnInfo
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import com.launium.mcping.Application

@Entity(tableName = "servers")
class MinecraftServer() {
    constructor(name: String, address: String) : this() {
        this.name = name
        this.address = address
    }

    @PrimaryKey
    var name = ""

    @ColumnInfo("address")
    var address = ""

    @ColumnInfo("latest_ping")
    var latestPing = Int.MAX_VALUE

    @ColumnInfo("motd_icon")
    var motdIcon = ""

    @Ignore
    var icon: Drawable? = null
        get() {
            if (motdIcon.isEmpty()) {
                return null
            }
            val content = Base64.decode(
                motdIcon.removePrefix("data:image/png;base64,"),
                Base64.DEFAULT
            )
            return BitmapFactory.decodeByteArray(content, 0, content.size)
                .toDrawable(Application.instance.resources)
        }

    @androidx.room.Dao
    interface Dao {
        @Query("SELECT * FROM servers")
        fun list(): List<MinecraftServer>

        @Insert
        fun add(server: MinecraftServer)

        @Delete
        fun delete(server: MinecraftServer)
    }

}