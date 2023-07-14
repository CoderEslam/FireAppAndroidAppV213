package com.devlomi.fireapp.model.Ads

import android.content.Context

object SessionManger {

    fun Context.setToken(v: String) {
        val sharedPreference = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(ConstantsSession.TOKEN.text, v)
        editor.apply()
    }

    fun Context.getToken(): String? {
        val sharedPreference = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        return sharedPreference.getString(ConstantsSession.TOKEN.text, "")
    }


    fun Context.setName(v: String) {
        val sharedPreference = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        val editor = sharedPreference.edit()
        editor.putString(ConstantsSession.NAME.text, v)
        editor.apply()
    }

    fun Context.getName(): String? {
        val sharedPreference = getSharedPreferences("PREFERENCE_NAME", Context.MODE_PRIVATE)
        return sharedPreference.getString(ConstantsSession.NAME.text, "")
    }

}