package com.launium.mcping.server

import android.graphics.drawable.Icon

class MinecraftServer() {
    constructor(name: String, address: String) : this() {
        this.name = name
        this.address = address
    }

    var name = ""
    var address = ""
    var icon: Icon? = null
}