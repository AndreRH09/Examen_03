package com.example.examen03.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.examen03.data.model.User
import com.example.examen03.data.repository.AppRepository
import com.example.examen03.network.SupabaseService

class LoginViewModel : ViewModel() {
    private val repository = AppRepository()

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun login(email: String, password: String) {
        _isLoading.value = true

        repository.login(email, password, object : SupabaseService.Callback<User?> {
            override fun onSuccess(user: User?) {
                _isLoading.postValue(false)
                if (user != null) {
                    _loginResult.postValue(LoginResult.Success(user))
                } else {
                    _loginResult.postValue(LoginResult.Error("Usuario no encontrado"))
                }
            }

            override fun onError(error: String) {
                _isLoading.postValue(false)
                _loginResult.postValue(LoginResult.Error(error))
            }
        })
    }

    sealed class LoginResult {
        data class Success(val user: User) : LoginResult()
        data class Error(val message: String) : LoginResult()
    }
}