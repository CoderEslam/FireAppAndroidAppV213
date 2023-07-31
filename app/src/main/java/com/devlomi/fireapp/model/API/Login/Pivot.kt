package com.devlomi.fireapp.model.API.Login

import android.os.Parcel
import android.os.Parcelable

data class Pivot(
    val model_id: Int,
    val model_type: String?,
    val role_id: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readString(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(model_id)
        parcel.writeString(model_type)
        parcel.writeInt(role_id)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Pivot> {
        override fun createFromParcel(parcel: Parcel): Pivot {
            return Pivot(parcel)
        }

        override fun newArray(size: Int): Array<Pivot?> {
            return arrayOfNulls(size)
        }
    }
}