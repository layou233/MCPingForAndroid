package com.launium.mcping.server

import io.ktor.utils.io.ByteReadChannel
import java.nio.ByteBuffer

// modified from https://wiki.vg/Protocol#VarInt_and_VarLong

private const val SEGMENT_BITS = 0x7F
private const val CONTINUE_BIT = 0x80

suspend fun ByteReadChannel.readVarInt(): Int {
    var value = 0
    var position = 0
    var currentByte: Byte
    while (true) {
        currentByte = readByte()
        value = value or (currentByte.toInt() and SEGMENT_BITS shl position)
        if (currentByte.toInt() and CONTINUE_BIT == 0) break
        position += 7
        if (position >= 32) throw RuntimeException("VarInt is too big")
    }
    return value
}

fun ByteBuffer.readVarInt(): Int {
    var value = 0
    var position = 0
    var currentByte: Byte
    while (true) {
        currentByte = get()
        value = value or (currentByte.toInt() and SEGMENT_BITS shl position)
        if (currentByte.toInt() and CONTINUE_BIT == 0) break
        position += 7
        if (position >= 32) throw RuntimeException("VarInt is too big")
    }
    return value
}

fun ByteBuffer.writeVarInt(value: Int): Int {
    var value = value
    var writtenBytes = 0
    while (true) {
        if (value and SEGMENT_BITS.inv() == 0) {
            put(value.toByte())
            return writtenBytes + 1
        }
        put((value and SEGMENT_BITS or CONTINUE_BIT).toByte())

        // Note: >>> means that the sign bit is shifted with the rest of the number rather than being left alone
        value = value ushr 7
        writtenBytes++
    }
}

fun estimateVarIntBinaryLength(value: Int): Int {
    return if (value < 0) {
        5
    } else if (value < (1 shl 7)) {
        1
    } else if (value < (1 shl 7 * 2)) {
        2
    } else if (value < (1 shl 7 * 3)) {
        3
    } else if (value < (1 shl 7 * 4)) {
        4
    } else {
        5
    }
}
