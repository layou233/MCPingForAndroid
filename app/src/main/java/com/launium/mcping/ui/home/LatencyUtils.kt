package com.launium.mcping.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.launium.mcping.R
import com.launium.mcping.common.chooseThemedResColor

@SuppressLint("SetTextI18n")
internal fun setLatency(context: Context, textView: TextView, latency: Int) {
    if (latency > 300000) {
        textView.setText(R.string.description_untested)
        textView.setTextColor(Color.DKGRAY)
    } else {
        textView.text = "$latency ms"
        textView.setTextColor(
            MaterialColors.harmonizeWithPrimary(
                context,
                if (latency > 200) {
                    context.chooseThemedResColor(
                        android.R.color.holo_red_light,
                        android.R.color.holo_red_dark
                    )
                } else if (latency > 160) {
                    context.chooseThemedResColor(
                        android.R.color.holo_orange_light,
                        android.R.color.holo_orange_dark
                    )
                } else if (latency > 100) {
                    context.chooseThemedResColor(
                        android.R.color.holo_blue_light,
                        android.R.color.holo_blue_dark
                    )
                } else {
                    context.chooseThemedResColor(
                        android.R.color.holo_green_light,
                        android.R.color.holo_green_dark
                    )
                }
            )
        )
    }
}