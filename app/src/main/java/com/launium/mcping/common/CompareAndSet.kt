package com.launium.mcping.common

import kotlin.reflect.KMutableProperty0

inline fun <T> KMutableProperty0<T>.compareAndSet(value: T): Boolean =
    if (get() != value) {
        set(value)
        true
    } else {
        false
    }
