package com.devlomi.fireapp.api

import com.devlomi.fireapp.model.API.Login.*
import com.devlomi.fireapp.model.API.Message.CallbackMessage
import com.devlomi.fireapp.model.API.Message.MessageApi
import com.devlomi.fireapp.model.Ads.Ads.AdsList
import retrofit2.Call
import retrofit2.http.*


interface API {


    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("register_with_phone")
    fun registerWithPhone(
        @Body phone: LoginPhone
    ): Call<CallbackLogin>

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("login")
    fun login(
        @Body phone: LoginPhone
    ): Call<CallbackLogin>

    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("messages")
    fun messages(
        @Header("Authorization") token: String,
        @Body message: MessageApi
    ): Call<CallbackMessage>


    @Headers("Content-Type: application/json", "Accept: application/json")
    @GET("ads")
    fun getAds(
        @Header("Authorization") token: String
    ): Call<AdsList>


}