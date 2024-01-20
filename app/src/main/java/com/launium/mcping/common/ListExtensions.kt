package com.launium.mcping.common

inline fun <T> List<T>.toArrayList(): ArrayList<T> = this as? ArrayList<T> ?: ArrayList(this)
