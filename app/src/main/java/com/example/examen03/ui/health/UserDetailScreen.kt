package com.example.examen03.ui.health

import com.example.examen03.data.model.HealthStatus


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



@Composable
fun UserDetailScreen(
    userId: String,
    viewModel: UserDetailViewModel,
    onNavigateBack: () -> Unit
) {
    val user by viewModel.user.observeAsState()
    val updateResult by viewModel.updateResult.observeAsState()
    val error by viewModel.error.observeAsState()

    var selectedStatus by remember { mutableStateOf<HealthStatus?>(null) }

    LaunchedEffect(userId) {
        viewModel.loadUser(userId)
    }

    LaunchedEffect(user) {
        user?.let {
            selectedStatus = it.healthStatus
        }
    }

    LaunchedEffect(updateResult) {
        if (updateResult == true) {
            onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        user?.let { currentUser ->
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Detalles del Usuario",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    DetailRow("Email", currentUser.email)
                    DetailRow("ID", currentUser.id)
                    DetailRow("Tipo", if (currentUser.isHealthPersonnel) "Personal de Salud" else "Usuario Regular")

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Estado de Salud",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HealthStatus.values().forEach { status ->
                            FilterChip(
                                selected = selectedStatus == status,
                                onClick = { selectedStatus = status },
                                label = {
                                    Text(
                                        when (status) {
                                            HealthStatus.HEALTHY -> "Sano"
                                            HealthStatus.AT_RISK -> "En Riesgo"
                                            HealthStatus.INFECTED -> "Infectado"
                                        }
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            selectedStatus?.let {
                                viewModel.updateHealthStatus(currentUser.id, it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = selectedStatus != currentUser.healthStatus
                    ) {
                        Text("Actualizar Estado")
                    }
                }
            }
        }
    }

    error?.let {
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(it)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}