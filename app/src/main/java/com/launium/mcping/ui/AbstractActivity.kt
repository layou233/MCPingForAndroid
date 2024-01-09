package com.launium.mcping.ui

import android.os.Bundle
import android.util.TypedValue
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.elevation.SurfaceColors
import com.launium.mcping.R

abstract class AbstractActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val color = SurfaceColors.SURFACE_2.getColor(this)
        window.statusBarColor = color
        window.navigationBarColor = color
    }

    fun setupHomeButton() {
        supportActionBar?.setHomeAsUpIndicator(AppCompatResources.getDrawable(
            this, R.drawable.ic_arrow_back_24dp
        )!!.apply {
            setTint(buttonColor)
        })
    }

    val buttonColor: Int
        get() {
            val typedValue = TypedValue()
            theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface, typedValue, true
            )
            return typedValue.data
        }

}