package com.example.examen03

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.examen03.ui.theme.Examen03Theme

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.examen03.bluetooth.BleService
import com.example.examen03.ui.login.LoginScreen
import com.example.examen03.ui.login.LoginViewModel
import com.example.examen03.ui.patient.PatientScreen
import com.example.examen03.ui.patient.PatientViewModel
import com.example.examen03.ui.health.HealthPersonnelScreen
import com.example.examen03.ui.health.HealthPersonnelViewModel
import com.example.examen03.ui.health.UserDetailScreen
import com.example.examen03.ui.health.UserDetailViewModel

import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            setContent { ContactTracingApp() }
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (checkPermissions()) {
            setContent { ContactTracingApp() }
        } else {
            requestPermissions()
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = getRequiredPermissions()
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun requestPermissions() {
        permissionLauncher.launch(getRequiredPermissions())
    }

    @Composable
    fun ContactTracingApp() {
        Examen03Theme {
            val navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = "login"
            ) {
                composable("login") {
                    val loginViewModel: LoginViewModel = viewModel()
                    LoginScreen(
                        viewModel = loginViewModel,
                        onLoginSuccess = { user ->
                            // Iniciar servicio BLE
                            val serviceIntent = Intent(this@MainActivity, BleService::class.java)
                            serviceIntent.putExtra("USER_ID", user.id)
                            startService(serviceIntent)

                            // Navegar según tipo de usuario
                            if (user.isHealthPersonnel) {
                                navController.navigate("health_personnel")
                            } else {
                                navController.navigate("patient")
                            }
                        }
                    )
                }

                composable("patient") {
                    val patientViewModel: PatientViewModel = viewModel()
                    PatientScreen(viewModel = patientViewModel)
                }

                composable("health_personnel") {
                    val healthViewModel: HealthPersonnelViewModel = viewModel()
                    HealthPersonnelScreen(
                        viewModel = healthViewModel,
                        onUserClick = { userId ->
                            navController.navigate("user_detail/$userId")
                        }
                    )
                }

                composable("user_detail/{userId}") { backStackEntry ->
                    val userId = backStackEntry.arguments?.getString("userId") ?: ""
                    val detailViewModel: UserDetailViewModel = viewModel()
                    UserDetailScreen(
                        userId = userId,
                        viewModel = detailViewModel,
                        onNavigateBack = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }


}