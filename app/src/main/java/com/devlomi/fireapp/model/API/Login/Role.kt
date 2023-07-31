package com.devlomi.fireapp.model.API.Login

import android.os.Parcel
import android.os.Parcelable

data class Role(
    val guard_name: String?,
    val id: Int,
    val name: String?,
    val pivot: Pivot?,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readInt(),
        parcel.readString(),
        parcel.readParcelable(Pivot::class.java.classLoader)
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(guard_name)
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeParcelable(pivot, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Role> {
        override fun createFromParcel(parcel: Parcel): Role {
            return Role(parcel)
        }

        override fun newArray(size: Int): Array<Role?> {
            return arrayOfNulls(size)
        }
    }
}