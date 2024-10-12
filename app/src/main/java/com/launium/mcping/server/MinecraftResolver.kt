package com.launium.mcping.server

import android.annotation.SuppressLint
import android.net.DnsResolver
import android.os.Build
import androidx.annotation.RequiresApi
import com.launium.mcping.Application
import com.launium.mcping.common.network.DnsServersDetector
import com.launium.mcping.ui.settings.Preferences
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.TypeOfService
import io.ktor.network.sockets.aSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.readUShort
import kotlinx.io.writeString
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.URI
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

// https://en.wikipedia.org/wiki/List_of_DNS_record_types
private const val TYPE_SRV: Short = 33

private const val CLASS_IN: Short = 1
private const val FLAG_RESPONSE: UShort = 32768U // most significant bit
private const val FLAG_RCODE_MASK: UShort = 15U

private const val COMPRESSED_LABEL_MARK: UByte = 0xC0U

class MinecraftResolver(uri: String) {

    private val serverURI = URI(uri)

    @OptIn(ExperimentalUnsignedTypes::class)
    fun parseSRVResponse(
        response: Source,
        serviceName: String,
        transactionID: Short = -1,
        queryLength: Int = -1,
        verifyTransactionID: Boolean = true
    ): List<InetSocketAddress>? {
        val responseTransactionID = response.readShort()
        if (verifyTransactionID && responseTransactionID != transactionID) {
            throw DNSResolveException(
                serviceName, "bad transaction ID: expect $transactionID, got $responseTransactionID"
            )
        }
        val flags = response.readUShort()
        if (flags and FLAG_RESPONSE != FLAG_RESPONSE) {
            throw DNSResolveException(serviceName, "not response")
        }
        when (val rCode = flags and FLAG_RCODE_MASK) {
            0.toUShort() -> {} // 0 stands for NO_ERROR
            3.toUShort() -> return null // NX_DOMAIN
            else -> throw DNSResolveException(serviceName, "RCode: $rCode")
        }

        val questionsCount = response.readShort()
        if (questionsCount != 1.toShort()) {
            throw DNSResolveException(
                serviceName, "bad question numbers: $questionsCount"
            )
        }
        val answersCount = response.readShort().toInt()
        response.skip(4) // ignore authority RRs and additional RRs
        response.skip(if (queryLength != -1) {
            queryLength
        } else {
            var queryLength = 1 + 2 + 2 // end of domain + TYPE_SRV + CLASS_IN
            serviceName.trimEnd('.').split('.').forEach {
                queryLength += 1 + it.length
            }
            queryLength
        }.toLong())

        if (answersCount == 0) return null
        val results = ArrayList<InetSocketAddress>(answersCount)
        val addressBuilder = StringBuilder()
        repeat(answersCount) {
            addressBuilder.clear()
            while (true) { // skip domain name
                val bytesCount = response.readByte().toUByte()
                if (bytesCount == 0.toUByte()) break
                if (bytesCount and COMPRESSED_LABEL_MARK == COMPRESSED_LABEL_MARK) {
                    response.skip(1)
                    break
                }
                response.skip(bytesCount.toLong())
            }
            response.skip(14)
            val port = response.readUShort()
            while (true) {
                val bytesCount = response.readByte().toUInt()
                if (bytesCount == 0U) break
                addressBuilder.append(response.readByteArray(bytesCount.toInt()))
                addressBuilder.append('.')
            }
            results.add(InetSocketAddress(addressBuilder.toString(), port.toInt()))
        }
        return results
    }

    @SuppressLint("WrongConstant")
    @OptIn(ExperimentalStdlibApi::class)
    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun querySRVPlatform(serviceName: String) = withContext(Dispatchers.IO) {
        return@withContext suspendCoroutine { continuation ->
            DnsResolver.getInstance().rawQuery(null,
                serviceName,
                CLASS_IN.toInt(),
                TYPE_SRV.toInt(),
                DnsResolver.FLAG_EMPTY,
                Dispatchers.IO.asExecutor(),
                null,
                object : DnsResolver.Callback<ByteArray> {

                    override fun onAnswer(answer: ByteArray, rCode: Int) {
                        when (rCode) {
                            0 -> try { // 0 stands for NO_ERROR
                                continuation.resume(
                                    parseSRVResponse(
                                        ByteArrayInputStream(answer).asSource().buffered(),
                                        serviceName,
                                        verifyTransactionID = false
                                    )
                                )
                            } catch (e: Exception) {
                                continuation.resumeWithException(IOException("Fail to parse SRV response: [00000000 ${
                                    answer.joinToString(separator = " ") { it.toHexString() } // hex dump
                                }]", e))
                            }

                            3 -> continuation.resume(null) // NX_DOMAIN

                            else -> continuation.resumeWithException(
                                DNSResolveException(
                                    serviceName, "RCode: $rCode"
                                )
                            )
                        }
                    }

                    override fun onError(e: DnsResolver.DnsException) {
                        continuation.resumeWithException(e)
                    }

                } as DnsResolver.Callback<ByteArray>)
        }
    }

    private suspend fun querySRVCompat(serviceName: String) = withContext(Dispatchers.IO) {
        lateinit var lastException: Exception
        DnsServersDetector(Application.instance).servers.forEach { dnsServer ->
            try {
                val address = io.ktor.network.sockets.InetSocketAddress(dnsServer, 53)
                val socket = aSocket(Application.ktorSelectorManager).udp().configure {
                    reuseAddress = true
                    typeOfService = TypeOfService.IPTOS_RELIABILITY
                }.connect(address)

                val transactionID = Random.nextBits(16).toShort()
                val buffer = Buffer()
                buffer.writeShort(transactionID)
                buffer.writeByte(1)
                buffer.writeByte(0)
                buffer.writeShort(1) // 1 question
                buffer.writeShort(0) // no answer RR
                buffer.writeShort(0) // no authority RR
                buffer.writeShort(0) // no additional RR
                var queryLength = 1 + 2 + 2 // end of domain + TYPE_SRV + CLASS_IN
                serviceName.trimEnd('.').split('.').forEach {
                    buffer.writeByte(it.length.toByte())
                    buffer.writeString(it)
                    queryLength += 1 + it.length
                }
                buffer.writeByte(0) // end of domain
                buffer.writeShort(TYPE_SRV)
                buffer.writeShort(CLASS_IN)
                socket.outgoing.send(Datagram(buffer, address))

                val response = socket.incoming.receive().packet
                return@withContext parseSRVResponse(
                    response, serviceName, transactionID, queryLength
                )
            } catch (e: SocketException) {
                // things like network unreachable etc
                lastException = e
            }
        }
        throw lastException
    }

    /**
     * Look up SRV-only record for specific Minecraft server.
     * Will throw exception if any error has been occurred.
     */
    suspend fun lookupSRV(): List<InetSocketAddress>? {
        val serviceName = "_minecraft._tcp.${serverURI.host}"
        return when (Preferences.srvResolver) {
            Preferences.SRVResolver.COMPATIBLE -> querySRVCompat(serviceName)

            Preferences.SRVResolver.SYSTEM_API -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    querySRVPlatform(serviceName)
                } else {
                    // should not be reached
                    throw RuntimeException("Unexpected unsupported API")
                }
            }
        }
    }

    suspend fun lookup(): List<InetSocketAddress> {
        try {
            val result = lookupSRV()
            if (!result.isNullOrEmpty()) {
                return result
            }
        } catch (e: Exception) {
            throw e
        }
        var port = serverURI.port
        if (port <= 0) {
            port = 25565
        }
        return listOf(InetSocketAddress(serverURI.host, port))
    }

    class DNSResolveException(domain: String, reason: String) :
        Exception("Resolve $domain: $reason")

}