package com.launium.mcping.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import com.launium.mcping.R

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
                    Color.RED
                } else if (latency > 160) {
                    Color.YELLOW
                } else if (latency > 100) {
                    Color.BLUE
                } else {
                    Color.GREEN
                }
            )
        )
    }
}