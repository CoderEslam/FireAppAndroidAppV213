package com.devlomi.fireapp.model.Ads.Ads

data class AdsModel(
    val description: String,
    val feedbacks: List<Any>,
    val from_date: String,
    val id: Int,
    val link: String,
    val media: String,
    val name: String,
    val status: String,
    val to_date: String,
    val user_id: Int
)