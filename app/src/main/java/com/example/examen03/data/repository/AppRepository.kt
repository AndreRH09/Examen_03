package com.example.examen03.data.repository

import com.example.examen03.data.model.Contact
import com.example.examen03.data.model.HealthStatus
import com.example.examen03.data.model.User
import com.example.examen03.network.SupabaseService

class AppRepository {
    private val supabaseService = SupabaseService()

    fun login(email: String, password: String, callback: SupabaseService.Callback<User?>) {
        supabaseService.login(email, password, callback)
    }

    fun getAllUsers(callback: SupabaseService.Callback<List<User>>) {
        supabaseService.getAllUsers(callback)
    }

    fun getUserById(userId: String, callback: SupabaseService.Callback<User?>) {
        supabaseService.getUserById(userId, callback)
    }

    fun updateHealthStatus(userId: String, newStatus: HealthStatus, callback: SupabaseService.Callback<Boolean>) {
        supabaseService.updateHealthStatus(userId, newStatus, callback)
    }

    fun saveContact(contact: Contact, callback: SupabaseService.Callback<Boolean>) {
        supabaseService.saveContact(contact, callback)
    }
}