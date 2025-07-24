package com.example.examen03.network

import com.example.examen03.data.model.Contact
import com.example.examen03.data.model.HealthStatus
import com.example.examen03.data.model.User

import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class SupabaseService {
    companion object {
        private const val SUPABASE_URL = "YOUR_SUPABASE_URL"
        private const val SUPABASE_KEY = "YOUR_SUPABASE_ANON_KEY"

        private val client = OkHttpClient()
        private val gson = Gson()
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    interface Callback<T> {
        fun onSuccess(result: T)
        fun onError(error: String)
    }

    fun login(email: String, password: String, callback: Callback<User?>) {
        val url = "$SUPABASE_URL/rest/v1/users?email=eq.$email&select=*"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val users = gson.fromJson(json, Array<UserJson>::class.java)
                    if (users.isNotEmpty()) {
                        callback.onSuccess(users[0].toUser())
                    } else {
                        callback.onSuccess(null)
                    }
                } else {
                    callback.onError("Login failed")
                }
            }
        })
    }

    fun getAllUsers(callback: Callback<List<User>>) {
        val url = "$SUPABASE_URL/rest/v1/users?select=*"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val users = gson.fromJson(json, Array<UserJson>::class.java)
                    callback.onSuccess(users.map { it.toUser() })
                } else {
                    callback.onError("Failed to get users")
                }
            }
        })
    }

    fun updateHealthStatus(userId: String, newStatus: HealthStatus, callback: Callback<Boolean>) {
        val url = "$SUPABASE_URL/rest/v1/users?id=eq.$userId"

        val json = """{"health_status": "${newStatus.name}"}"""
        val body = json.toRequestBody(JSON)

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Prefer", "return=minimal")
            .patch(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response.isSuccessful)
            }
        })
    }

    fun saveContact(contact: Contact, callback: Callback<Boolean>) {
        val url = "$SUPABASE_URL/rest/v1/contacts"

        val json = gson.toJson(ContactJson.fromContact(contact))
        val body = json.toRequestBody(JSON)

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .addHeader("Prefer", "return=minimal")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                callback.onSuccess(response.isSuccessful)
            }
        })
    }

    fun getUserById(userId: String, callback: Callback<User?>) {
        val url = "$SUPABASE_URL/rest/v1/users?id=eq.$userId&select=*"

        val request = Request.Builder()
            .url(url)
            .addHeader("apikey", SUPABASE_KEY)
            .addHeader("Authorization", "Bearer $SUPABASE_KEY")
            .get()
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Network error")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val json = response.body?.string()
                    val users = gson.fromJson(json, Array<UserJson>::class.java)
                    if (users.isNotEmpty()) {
                        callback.onSuccess(users[0].toUser())
                    } else {
                        callback.onSuccess(null)
                    }
                } else {
                    callback.onError("Failed to get user")
                }
            }
        })
    }

    // DTOs para JSON
    private data class UserJson(
        val id: String,
        val email: String,
        val is_health_personnel: Boolean,
        val health_status: String,
        val device_id: String
    ) {
        fun toUser() = User(
            id = id,
            email = email,
            isHealthPersonnel = is_health_personnel,
            healthStatus = HealthStatus.valueOf(health_status),
            deviceId = device_id
        )
    }

    private data class ContactJson(
        val id: String,
        val user_id: String,
        val contact_user_id: String,
        val timestamp: Long,
        val rssi: Int
    ) {
        companion object {
            fun fromContact(contact: Contact) = ContactJson(
                id = contact.id,
                user_id = contact.userId,
                contact_user_id = contact.contactUserId,
                timestamp = contact.timestamp,
                rssi = contact.rssi
            )
        }
    }
}