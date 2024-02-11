package com.launium.mcping.server

import com.alibaba.fastjson2.JSON
import com.alibaba.fastjson2.JSONArray
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.JSONReader
import io.ktor.network.sockets.InetSocketAddress
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import kotlin.system.measureTimeMillis

private const val NEXT_STATE_STATUS: Byte = 1
private const val NEXT_STATE_LOGIN: Byte = 2

private const val ID_HANDSHAKE: Byte = 0
private const val ID_STATUS: Byte = 0
private const val ID_PING: Byte = 1

class MinecraftClient(private val connection: Socket) : Closeable {

    private val readChannel = connection.openReadChannel()
    private val writeChannel = connection.openWriteChannel()

    suspend fun requestStatus(version: Int): MinecraftServerStatus = withContext(Dispatchers.IO) {
        // https://wiki.vg/Protocol#Status_Request
        sendHandshake(version, NEXT_STATE_STATUS, byteArrayOf(1, ID_STATUS))

        readChannel.readVarInt() // packet length, ignored
        val packetID = readChannel.readByte()
        if (packetID != ID_STATUS) {
            throw UnexpectedPacketException(ID_STATUS, packetID)
        }
        val statusLength = readChannel.readVarInt()
        val statusArray = ByteArray(statusLength)
        readChannel.readFully(statusArray)
        val statusObject =
            JSON.parse(statusArray, JSONReader.Feature.InitStringFieldAsEmpty) as JSONObject
        val serverFavicon = (statusObject["favicon"] as? String) ?: ""
        var serverDescriptionText = ""
        when (val description = statusObject["description"]) {
            is JSONObject -> {
                serverDescriptionText = description["text"] as String
                description["extra"]?.let {
                    (it as JSONArray).forEach { item ->
                        if (item is JSONObject) {
                            serverDescriptionText += item["text"] as String
                        }
                    }
                }
            }

            is String -> serverDescriptionText = description
        }
        var serverOnline = 0
        var serverMaxOnline = 0
        var serverPlayers = listOf<MinecraftServerStatus.Player>()
        when (val serverPlayersObject = statusObject["players"]) {
            is JSONObject -> {
                serverOnline = (serverPlayersObject["online"] as? Int) ?: 0
                serverMaxOnline = (serverPlayersObject["max"] as? Int) ?: 0
                val serverSamples = serverPlayersObject["sample"]
                if (serverSamples is JSONArray) {
                    serverPlayers = serverSamples.mapNotNull {
                        if (it is JSONObject) {
                            MinecraftServerStatus.Player(
                                (it["name"] as? String) ?: "",
                                (it["id"] as? String) ?: "",
                            )
                        } else null
                    }
                }
            }
        }
        val serverVersion =
            (statusObject["version"] as? JSONObject)?.let { it["name"] as? String } ?: ""

        // https://wiki.vg/Server_List_Ping#Ping_Request
        writeChannel.write(10) {
            it.put(9)
            it.put(ID_PING)
            it.putLong(System.currentTimeMillis())
        }
        writeChannel.flush()
        val latency = measureTimeMillis {
            readChannel.awaitContent()
        }

        return@withContext MinecraftServerStatus(
            serverFavicon,
            latency,
            serverDescriptionText,
            serverVersion,
            serverOnline,
            serverMaxOnline,
            serverPlayers,
        )
    }

    private suspend fun sendHandshake(version: Int, nextState: Byte, appendix: ByteArray?) =
        withContext(Dispatchers.IO) {
            val address = connection.remoteAddress as InetSocketAddress
            val hostname = address.hostname.toByteArray()
            val estimatedLength =
                1 + estimateVarIntBinaryLength(version) + estimateVarIntBinaryLength(hostname.size) + hostname.size + 2 + 1
            writeChannel.write(
                estimatedLength + estimateVarIntBinaryLength(estimatedLength) + (appendix?.size
                    ?: 0)
            ) {
                it.putVarInt(estimatedLength)
                it.put(ID_HANDSHAKE)
                it.putVarInt(version)
                it.putVarInt(hostname.size)
                it.put(hostname)
                it.putShort(address.port.toShort())
                it.put(nextState)
                appendix?.let { _ -> it.put(appendix) }
            }
            writeChannel.flush()
        }

    override fun close() {
        writeChannel.flush()
        connection.close()
    }

    class UnexpectedPacketException(wanted: Byte, got: Byte) : Exception("wanted $wanted, got $got")

}