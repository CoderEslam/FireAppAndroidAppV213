package com.devlomi.fireapp.Advertisement.api

import com.devlomi.fireapp.model.Ads.Ads.AdsList
import com.devlomi.fireapp.model.Ads.Login.LoginModel
import com.devlomi.fireapp.model.Ads.Login.UserModel
import retrofit2.Call
import retrofit2.http.*


interface API {


    @Headers("Content-Type: application/json", "Accept: application/json")
    @POST("login")
    fun login(
        @Body login: LoginModel
    ): Call<UserModel>

    @Headers("Content-Type: application/json", "Accept: application/json")
    @GET
    fun getAds(
        @Header("Authorization") token: String
    ): Call<AdsList>


}