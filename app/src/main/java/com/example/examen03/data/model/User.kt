package com.example.examen03.data.model


data class User(
    val id: String,
    val email: String,
    val isHealthPersonnel: Boolean,
    val healthStatus: HealthStatus,
    val deviceId: String
)