package com.launium.mcping.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import com.launium.mcping.R
import com.launium.mcping.common.getAttrColor

abstract class AbstractActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    fun setupHomeButton() {
        supportActionBar?.setHomeAsUpIndicator(AppCompatResources.getDrawable(
            this, R.drawable.ic_arrow_back_24dp
        )!!.apply {
            setTint(getAttrColor(com.google.android.material.R.attr.colorOnSurface))
        })
    }

}