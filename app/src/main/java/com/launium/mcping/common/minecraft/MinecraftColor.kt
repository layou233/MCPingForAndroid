package com.launium.mcping.common.minecraft

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import androidx.annotation.ColorInt

enum class MinecraftColors(@ColorInt val rgb: Int) {
    BLACK(0xFF000000U.toInt()), // 0
    DARK_BLUE(0xFF0000AAU.toInt()), // 1
    DARK_GREEN(0xFF00AA00U.toInt()), // 2
    DARK_CYAN(0xFF00AAAAU.toInt()), // 3
    DARK_RED(0xFFAA0000U.toInt()), // 4
    PURPLE(0xFFAA00AAU.toInt()), // 5
    GOLD(0xFFFFAA00U.toInt()), // 6
    GRAY(0xFFAAAAAAU.toInt()), // 7
    DARK_GRAY(0xFF555555U.toInt()), // 8
    BLUE(0xFF5555FFU.toInt()), // 9
    BRIGHT_GREEN(0xFF55FF55U.toInt()), // a
    CYAN(0xFF55FFFFU.toInt()), // b
    RED(0xFFFF5555U.toInt()), // c
    PINK(0xFFFF55FFU.toInt()), // d
    YELLOW(0xFFFFFF55U.toInt()), // e
    WHITE(0xFFFFFFFFU.toInt()), // f
}

fun parseMinecraftColor(message: String): SpannableStringBuilder {
    val builder = SpannableStringBuilder()
    if (message.isBlank()) return builder

    var expectStyleCode = false
    val styleCodes = mutableListOf<Pair<Char, Int>>()
    message.forEach {
        if (expectStyleCode) {
            expectStyleCode = false
            styleCodes.add(Pair(it.lowercaseChar(), builder.length))
        } else if (it == '§') {
            expectStyleCode = true
        } else {
            builder.append(it)
        }
    }

    val resetSymbols = styleCodes.filter { it.first == 'r' }
    styleCodes.forEach {
        val span = when {
            (it.first in '0'..'9') || (it.first in 'a'..'f') -> {
                val color = MinecraftColors.values()[it.first.digitToInt(16)]
                ForegroundColorSpan(color.rgb)
            }

            //it.first == 'k' -> null // obfuscated unimplemented
            it.first == 'l' -> StyleSpan(Typeface.BOLD)
            it.first == 'm' -> StrikethroughSpan()
            it.first == 'n' -> UnderlineSpan()
            it.first == 'o' -> StyleSpan(Typeface.ITALIC)

            else -> null
        }
        if (span != null) {
            builder.setSpan(
                span,
                it.second,
                resetSymbols.find { r -> it.second <= r.second }?.second ?: builder.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }
    }

    return builder
}
