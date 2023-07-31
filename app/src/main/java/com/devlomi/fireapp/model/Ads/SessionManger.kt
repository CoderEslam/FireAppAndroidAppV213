package com.devlomi.fireapp.model.Ads

import android.content.Context

object SessionManger {

    fun Context.setToken(v: String) {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.TOKEN.text, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(ConstantsSession.TOKEN.text, v)
        editor.apply()
    }

    fun Context.getToken(): String? {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.TOKEN.text, Context.MODE_PRIVATE)
        return sharedPreference.getString(ConstantsSession.TOKEN.text, "")
    }


    fun Context.setName(v: String) {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.NAME.text, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(ConstantsSession.NAME.text, v)
        editor.apply()
    }

    fun Context.getName(): String? {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.NAME.text, Context.MODE_PRIVATE)
        return sharedPreference.getString(ConstantsSession.NAME.text, "")
    }

    fun Context.setID(v: String) {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.ID.text, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(ConstantsSession.ID.text, v)
        editor.apply()
    }

    fun Context.getID(): String? {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.ID.text, Context.MODE_PRIVATE)
        return sharedPreference.getString(ConstantsSession.ID.text, "")
    }

    fun Context.setPhone(v: String) {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.PHONE.text, Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(ConstantsSession.PHONE.text, v)
        editor.apply()
    }

    fun Context.getPhone(): String? {
        val sharedPreference =
            getSharedPreferences(ConstantsSession.PHONE.text, Context.MODE_PRIVATE)
        return sharedPreference.getString(ConstantsSession.PHONE.text, "")
    }

}