package com.launium.mcping.server

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
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
import com.launium.mcping.common.compareAndSet
import io.ktor.network.sockets.SocketOptions
import io.ktor.network.sockets.TypeOfService
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.InetSocketAddress
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

    @ColumnInfo("ignore_srv")
    var ignoreSRVRedirect = false

    @Ignore
    var description = ""

    @Ignore
    var version = ""

    @Ignore
    var online = 0

    @Ignore
    var maxOnline = 0

    @Ignore
    var players = listOf<MinecraftServerStatus.Player>()

    @Ignore
    var removed = false

    val icon: Bitmap?
        get() {
            if (motdIcon.isEmpty()) {
                return null
            }
            val content = Base64.decode(
                motdIcon.removePrefix("data:image/png;base64,"), Base64.DEFAULT
            )
            return BitmapFactory.decodeByteArray(content, 0, content.size)
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

    val uri
        get() = "mc://$address${
            if (name.isNotBlank()) {
                "/#${Uri.encode(name)}"
            } else ""
        }"

    suspend fun connect(): MinecraftClient = withContext(Dispatchers.IO) {
        val serverAddresses = if (ignoreSRVRedirect) {
            val serverURI = URI(uri)
            var port = serverURI.port
            if (port <= 0) {
                port = 25565
            }
            listOf(InetSocketAddress(serverURI.host, port))
        } else {
            MinecraftResolver(uri).lookup()
        }
        val socketFactory = aSocket(Application.ktorSelectorManager).configure {
            reuseAddress = true
            reusePort = true
            typeOfService = TypeOfService.IPTOS_LOWDELAY
            if (this is SocketOptions.TCPClientSocketOptions) {
                keepAlive = false
                noDelay = true
                lingerSeconds = 10
                socketTimeout = 10
            }
        }

        var lastException: Exception? = null
        for (address in serverAddresses) {
            try {
                val socket = socketFactory.tcp().connect(address.hostName, address.port)
                return@withContext MinecraftClient(socket)
            } catch (e: Exception) {
                lastException = e
            }
        }
        throw IOException(
            "Fail to connect to " + serverAddresses.joinToString(prefix = "[", postfix = "]"),
            lastException
        )
    }

    suspend fun requestStatus(): Boolean {
        val client = connect()
        val status = try {
            client.requestStatus(47)
        } catch (t: Throwable) {
            client.close()
            throw t
        }
        client.close()

        val changed = listOf(
            this::latestPing.compareAndSet(status.latency.toInt()),
            this::motdIcon.compareAndSet(status.favicon),
            this::description.compareAndSet(status.description),
            this::version.compareAndSet(status.version),
            this::online.compareAndSet(status.online),
            this::maxOnline.compareAndSet(status.maxOnline),
            this::players.compareAndSet(status.players),
        ).find { it } ?: false

        return changed
    }

}