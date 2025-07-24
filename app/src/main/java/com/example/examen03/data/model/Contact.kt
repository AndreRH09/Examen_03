package com.example.examen03.data.model


data class Contact(
    val id: String,
    val userId: String,
    val contactUserId: String,
    val timestamp: Long,
    val rssi: Int
)