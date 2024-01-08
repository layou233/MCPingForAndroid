package com.launium.mcping.ui

import android.os.Bundle
import com.launium.mcping.server.MinecraftServer
import com.launium.mcping.ui.home.ServerSheetDialog

class ServerURIActivity : AbstractActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent.data!!
        ServerSheetDialog(
            layoutInflater.context,
            MinecraftServer(uri.fragment ?: "", uri.authority ?: ""),
            null,
            false
        ).let {
            it.setOnCancelListener {
                finish()
            }
            it.show()
        }
    }

}