package com.launium.mcping.server

import android.net.DnsResolver
import android.os.Build
import androidx.annotation.RequiresApi
import com.launium.mcping.Application
import com.launium.mcping.common.network.DnsServersDetector
import com.launium.mcping.ui.settings.Preferences
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.TypeOfService
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.Input
import io.ktor.utils.io.core.readShort
import io.ktor.utils.io.core.readTextExactBytes
import io.ktor.utils.io.core.readUShort
import io.ktor.utils.io.streams.asInput
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random

// https://en.wikipedia.org/wiki/List_of_DNS_record_types
private const val TYPE_SRV: Short = 33

private const val CLASS_IN: Short = 1
private const val FLAG_RESPONSE: UShort = 32768U // most significant bit
private const val FLAG_RCODE_MASK: UShort = 15U

class MinecraftResolver(uri: String) {

    private val serverURI = URI(uri)

    @OptIn(ExperimentalUnsignedTypes::class)
    fun parseSRVResponse(
        response: Input,
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
        response.discard(4) // ignore authority RRs and additional RRs
        response.discard(if (queryLength != -1) {
            queryLength
        } else {
            var queryLength = 1 + 2 + 2 // end of domain + TYPE_SRV + CLASS_IN
            serviceName.trimEnd('.').split('.').forEach {
                queryLength += 1 + it.length
            }
            queryLength
        })

        if (answersCount == 0) return null
        val results = ArrayList<InetSocketAddress>(answersCount)
        val addressBuilder = StringBuilder()
        repeat(answersCount) {
            addressBuilder.clear()
            response.discard(16)
            val port = response.readUShort()
            while (true) {
                val bytesCount = response.readByte().toInt()
                if (bytesCount == 0) break
                addressBuilder.append(response.readTextExactBytes(bytesCount))
                addressBuilder.append('.')
            }
            results.add(InetSocketAddress(addressBuilder.toString(), port.toInt()))
        }
        return results
    }

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
                                        ByteArrayInputStream(answer).asInput(),
                                        serviceName,
                                        verifyTransactionID = false
                                    )
                                )
                            } catch (e: Exception) {
                                continuation.resumeWithException(IOException("Fail to parse SRV response: [00000000 ${
                                    answer.joinToString { it.toHexString() + " " }
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
                val buffer = ByteBuffer.allocate(512)
                buffer.order(ByteOrder.BIG_ENDIAN)
                buffer.putShort(transactionID)
                buffer.put(1)
                buffer.put(0)
                buffer.putShort(1) // 1 question
                buffer.putShort(0) // no answer RR
                buffer.putShort(0) // no authority RR
                buffer.putShort(0) // no additional RR
                var queryLength = 1 + 2 + 2 // end of domain + TYPE_SRV + CLASS_IN
                serviceName.trimEnd('.').split('.').forEach {
                    buffer.put(it.length.toByte())
                    buffer.put(it.toByteArray())
                    queryLength += 1 + it.length
                }
                buffer.put(0) // end of domain
                buffer.putShort(TYPE_SRV)
                buffer.putShort(CLASS_IN)
                buffer.flip()
                socket.outgoing.send(Datagram(ByteReadPacket(buffer), address))

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