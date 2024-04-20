package com.launium.mcping.common

import android.content.Context
import android.os.Build
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

inline fun Context.isDarkTheme() =
    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) && (resources.configuration.isNightModeActive)

@ColorInt
fun Context.getAttrColor(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

@ColorInt
fun Context.chooseThemedColor(
    @ColorInt colorLight: Int,
    @ColorInt colorDark: Int,
): Int {
    if (isDarkTheme()) return colorDark
    return colorLight
}

@ColorInt
fun Context.chooseThemedResColor(
    @ColorRes colorLight: Int,
    @ColorRes colorDark: Int,
): Int {
    if (isDarkTheme()) return ContextCompat.getColor(this, colorDark)
    return ContextCompat.getColor(this, colorLight)
}
