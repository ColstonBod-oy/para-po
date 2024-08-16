package com.enigma.parapo.models

import android.os.Parcel
import android.os.Parcelable

data class User(
    val id: String? = "",
    val name: String? = "",
    val email: String? = "",
    val image: String? = "profilephoto/profilepic1.png",
    val skin: Int = 0,
    val stickers: ArrayList<Map<String, Any?>> = arrayListOf()
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readInt(),
        parcel.readArrayList(Map::class.java.classLoader) as ArrayList<Map<String, Any?>>
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(id)
        parcel.writeString(name)
        parcel.writeString(email)
        parcel.writeString(image)
        parcel.writeInt(skin)
        parcel.writeList(stickers)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<User> {
        override fun createFromParcel(parcel: Parcel): User {
            return User(parcel)
        }

        override fun newArray(size: Int): Array<User?> {
            return arrayOfNulls(size)
        }
    }
}