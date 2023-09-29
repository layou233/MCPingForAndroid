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
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.launium.mcping.Application
import com.launium.mcping.database.ServerManager
import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import java.net.URI

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
    var removed = false

    @Ignore
    var icon: Drawable? = null
        get() {
            if (motdIcon.isEmpty()) {
                return null
            }
            val content = Base64.decode(
                motdIcon.removePrefix("data:image/png;base64,"), Base64.DEFAULT
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

        @Update(onConflict = OnConflictStrategy.REPLACE)
        fun update(server: MinecraftServer)

        @Delete
        fun delete(server: MinecraftServer)
    }

    val uri get() = "mc://$address"

    suspend fun connect(): MinecraftClient {
        val serverURI = URI(uri)
        var port = serverURI.port
        if (port <= 0) {
            port = 25565
        }
        val selectorManager = SelectorManager(Dispatchers.IO)
        val socket = aSocket(selectorManager).configure {
            reuseAddress = true
            reusePort = true
            if (this is SocketOptions.TCPClientSocketOptions) {
                keepAlive = false
                noDelay = true
                lingerSeconds = 10
                socketTimeout = 10
            }
        }.tcp().connect(serverURI.host, port)
        return MinecraftClient(selectorManager, socket)
    }

    suspend fun requestStatus(): Boolean {
        val client = connect()
        val status = client.requestStatus(47)
        client.close()

        var changed = false
        status.latency.toInt().let {
            if (it != latestPing) {
                changed = true
                latestPing = it
            }
        }
        status.favicon.let {
            if (it != motdIcon) {
                changed = true
                motdIcon = it
            }
        }

        if (changed) {
            ServerManager.serverDao.update(this)
        }
        return changed
    }

}