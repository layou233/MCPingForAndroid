package com.launium.mcping.ui.settings

import android.content.Context
import android.os.Build
import com.launium.mcping.Application

private const val PACKAGE_ID = "com.launium.mcping"
private const val PREFERENCE_ID = "$PACKAGE_ID.SETTINGS"

// preference keys
private const val SRV_RESOLVER = "SRV_RESOLVER"
private const val MAX_CONCURRENT_PINGS = "MAX_CONCURRENT_PINGS"

object Preferences {

    private val sharedPreferences by lazy {
        Application.instance.getSharedPreferences(
            PREFERENCE_ID, Context.MODE_PRIVATE
        )
    }

    enum class SRVResolver {
        COMPATIBLE, SYSTEM_API
    }

    var srvResolver: SRVResolver
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return SRVResolver.COMPATIBLE
            }
            val resolverID = sharedPreferences.getInt(SRV_RESOLVER, -1)
            if (resolverID == -1) {
                return SRVResolver.SYSTEM_API
            }
            return SRVResolver.values()[resolverID]
        }
        set(srvResolver) {
            if (srvResolver == SRVResolver.SYSTEM_API) { // set to default
                sharedPreferences.edit().remove(SRV_RESOLVER).apply()
            } else {
                sharedPreferences.edit().putInt(SRV_RESOLVER, srvResolver.ordinal).apply()
            }
        }

    var maxConcurrentPings: Int
        get() {
            val stored = sharedPreferences.getInt(MAX_CONCURRENT_PINGS, -1)
            return if (stored > 0) {
                stored
            } else {
                16
            }
        }
        set(value) {
            if (value == 16) { // set to default
                sharedPreferences.edit().remove(MAX_CONCURRENT_PINGS).apply()
            } else if (value > 0) {
                sharedPreferences.edit().putInt(MAX_CONCURRENT_PINGS, value).apply()
            }
        }

}