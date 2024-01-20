package com.launium.mcping.server

import android.os.Parcel
import android.os.Parcelable

data class MinecraftServerStatus internal constructor(
    val favicon: String,
    val latency: Long,
    val description: String,
    val version: String,
    val online: Int,
    val maxOnline: Int,
    val players: List<Player>,
) {
    data class Player(val name: String, val uuid: String) : Parcelable {

        constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeString(name)
            parcel.writeString(uuid)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<Player> {

            override fun createFromParcel(parcel: Parcel): Player {
                return Player(parcel)
            }

            override fun newArray(size: Int): Array<Player?> {
                return arrayOfNulls(size)
            }

        }

    }
}
