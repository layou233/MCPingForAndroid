package com.launium.mcping.server

data class MinecraftServerStatus internal constructor(
    val favicon: String,
    val latency: Long,
    val description: String,
)
