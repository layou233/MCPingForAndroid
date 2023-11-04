package com.launium.mcping.server

import com.launium.mcping.Application
import com.launium.mcping.common.network.DnsServersDetector
import io.ktor.network.sockets.Datagram
import io.ktor.network.sockets.TypeOfService
import io.ktor.network.sockets.aSocket
import io.ktor.utils.io.core.ByteReadPacket
import io.ktor.utils.io.core.readShort
import io.ktor.utils.io.core.readTextExactBytes
import io.ktor.utils.io.core.readUShort
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.URI
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random


// https://en.wikipedia.org/wiki/List_of_DNS_record_types
private const val TYPE_SRV: Short = 33

private const val CLASS_IN: Short = 1
private const val FLAG_RESPONSE: UShort = 32768U // most significant bit
private const val FLAG_RCODE_MASK: UShort = 15U

class MinecraftResolver(uri: String) {

    val serverURI = URI(uri)

    @OptIn(ExperimentalUnsignedTypes::class)
    suspend fun querySRV(serviceName: String) =
        withContext<List<InetSocketAddress>?>(Dispatchers.IO) {
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
                    val responseTransactionID = response.readShort()
                    if (responseTransactionID != transactionID) {
                        throw DNSResolveException(
                            serviceName,
                            "bad transaction ID: expect $transactionID, got $responseTransactionID"
                        )
                    }
                    val flags = response.readUShort()
                    if (flags and FLAG_RESPONSE != FLAG_RESPONSE) {
                        throw DNSResolveException(serviceName, "not response")
                    }
                    when (val rCode = flags and FLAG_RCODE_MASK) {
                        0.toUShort() -> {} // 0 stands for NO_ERROR
                        3.toUShort() -> return@withContext null // NX_DOMAIN
                        else -> throw DNSResolveException(serviceName, "RCode: $rCode")
                    }

                    val questionsCount = response.readShort()
                    if (questionsCount != 1.toShort()) {
                        throw DNSResolveException(
                            serviceName,
                            "bad question numbers: $questionsCount"
                        )
                    }
                    val answersCount = response.readShort().toInt()
                    response.discard(4) // ignore authority RRs and additional RRs
                    response.discard(queryLength)

                    if (answersCount == 0) return@withContext null
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
                    return@withContext results
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
    suspend inline fun lookupSRV(): List<InetSocketAddress>? =
        querySRV("_minecraft._tcp.${serverURI.host}")

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