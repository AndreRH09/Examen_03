package com.example.examen03.ui.patient


import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class PatientViewModel : ViewModel() {
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage

    init {
        _statusMessage.value = "La aplicación está en funcionamiento\n\nEl rastreo de contactos está activo"
    }
}