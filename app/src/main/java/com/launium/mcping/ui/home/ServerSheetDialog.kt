package com.launium.mcping.ui.home

import android.content.Context
import android.text.Editable
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.launium.mcping.R
import com.launium.mcping.server.MinecraftServer

class ServerSheetDialog(context: Context, private val server: MinecraftServer) :
    BottomSheetDialog(context) {

    init {
        setContentView(R.layout.server_sheet)
    }

    override fun onStart() {
        super.onStart()

        findViewById<TextView>(R.id.server_sheet_name).let {
            it?.text = server.name
        }
        findViewById<TextInputEditText>(R.id.server_sheet_address).let {
            it?.text = Editable.Factory.getInstance().newEditable(server.address)
        }
        findViewById<Button>(R.id.server_sheet_copy_icon).let {
            it?.setOnClickListener {
                Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}