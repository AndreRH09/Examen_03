package com.example.examen03.ui.health

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.examen03.data.model.HealthStatus
import com.example.examen03.data.model.User
import com.example.examen03.data.repository.AppRepository
import com.example.examen03.network.SupabaseService

class UserDetailViewModel : ViewModel() {
    private val repository = AppRepository()

    private val _user = MutableLiveData<User>()
    val user: LiveData<User> = _user

    private val _updateResult = MutableLiveData<Boolean>()
    val updateResult: LiveData<Boolean> = _updateResult

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadUser(userId: String) {
        repository.getUserById(userId, object : SupabaseService.Callback<User?> {
            override fun onSuccess(user: User?) {
                user?.let { _user.postValue(it) }
            }

            override fun onError(error: String) {
                _error.postValue(error)
            }
        })
    }

    fun updateHealthStatus(userId: String, newStatus: HealthStatus) {
        repository.updateHealthStatus(userId, newStatus, object : SupabaseService.Callback<Boolean> {
            override fun onSuccess(result: Boolean) {
                _updateResult.postValue(result)
            }

            override fun onError(error: String) {
                _error.postValue(error)
            }
        })
    }
}