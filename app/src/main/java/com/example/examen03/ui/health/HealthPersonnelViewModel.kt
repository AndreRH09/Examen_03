package com.example.examen03.ui.health

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.examen03.data.model.User
import com.example.examen03.data.repository.AppRepository
import com.example.examen03.network.SupabaseService

class HealthPersonnelViewModel : ViewModel() {
    private val repository = AppRepository()

    private val _users = MutableLiveData<List<User>>()
    val users: LiveData<List<User>> = _users

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadUsers() {
        repository.getAllUsers(object : SupabaseService.Callback<List<User>> {
            override fun onSuccess(users: List<User>) {
                _users.postValue(users)
            }

            override fun onError(error: String) {
                _error.postValue(error)
            }
        })
    }
}