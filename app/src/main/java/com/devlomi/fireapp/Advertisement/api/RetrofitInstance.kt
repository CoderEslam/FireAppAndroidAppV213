package com.devlomi.fireapp.Advertisement.api

import com.devlomi.fireapp.Advertisement.api.Constants.BASE_URL
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(
                GsonConverterFactory
                    .create(
                        GsonBuilder()
                            .setLenient()
                            .create()
                    )
            )
            .build()
    }

    val api: API by lazy {
        retrofit.create(API::class.java)
    }

}