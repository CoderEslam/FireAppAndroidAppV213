package com.devlomi.fireapp.model.Ads.Login

data class User(
    val device_token: String,
    val email: String,
    val id: Int,
    val name: String,
    val phone: String,
    val status: String,
)