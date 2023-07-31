package com.devlomi.fireapp.model.API.Message

data class MessageApi(
    val content: String,
    val from_user_id_firebase: String,
    val to_user_id_firebase: String,
    val from_user_id_mysql: String,
    val to_user_id_mysql: String,
    val timestamp: String,
    val type: Int
)