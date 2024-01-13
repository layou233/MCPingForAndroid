package com.launium.mcping.server

data class MinecraftServerStatus internal constructor(
    val favicon: String,
    val latency: Long,
    val description: String,
    val version: String,
    val online: Int,
    val maxOnline: Int,
    val players: List<Player>,
) {
    data class Player(val name: String, val uuid: String)
}
