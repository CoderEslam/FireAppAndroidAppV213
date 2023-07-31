package com.devlomi.fireapp.model.API.Login

import android.os.Parcel
import android.os.Parcelable

data class CallbackLogin(
    val role: String?,
    val user: User?,
    val errors: List<String?>?,
    val message: String?,
    val phone: String?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        parcel.readParcelable(User::class.java.classLoader),
        parcel.createStringArrayList(),
        parcel.readString(),
        parcel.readString()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(role)
        parcel.writeParcelable(user, flags)
        parcel.writeStringList(errors)
        parcel.writeString(message)
        parcel.writeString(phone)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun toString(): String {
        return "CallbackLogin(role=$role, user=$user, errors=$errors, message=$message, phone=$phone)"
    }

    companion object CREATOR : Parcelable.Creator<CallbackLogin> {
        override fun createFromParcel(parcel: Parcel): CallbackLogin {
            return CallbackLogin(parcel)
        }

        override fun newArray(size: Int): Array<CallbackLogin?> {
            return arrayOfNulls(size)
        }
    }
}