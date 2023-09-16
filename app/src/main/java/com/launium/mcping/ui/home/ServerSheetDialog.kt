package com.launium.mcping.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.Editable
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputEditText
import com.launium.mcping.R
import com.launium.mcping.server.MinecraftServer

class ServerSheetDialog(context: Context, private val server: MinecraftServer) :
    BottomSheetDialog(context) {

    companion object {
        private const val TAG = "ServerSheetDialog"
    }

    init {
        setContentView(R.layout.server_sheet)
    }

    override fun onStart() {
        super.onStart()

        try {
            findViewById<TextView>(R.id.server_sheet_name)!!.let {
                it.text = server.name
            }
            findViewById<TextInputEditText>(R.id.server_sheet_address)!!.let {
                it.text = Editable.Factory.getInstance().newEditable(server.address)
            }
            findViewById<Button>(R.id.server_sheet_copy_icon)!!.let {
                it.setOnClickListener {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(
                        ClipData.newPlainText("ServerAddress", server.address)
                    )
                    Toast.makeText(context, "Copied!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        } catch (e: Exception) {
            val stackTrace = Log.getStackTraceString(e)
            Log.e(TAG, stackTrace)
            Toast.makeText(context, stackTrace, Toast.LENGTH_LONG)
                .show()
        }
    }

}