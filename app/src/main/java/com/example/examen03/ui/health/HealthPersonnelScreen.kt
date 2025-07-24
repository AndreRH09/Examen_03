package com.example.examen03.ui.health


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.examen03.data.model.HealthStatus
import com.example.examen03.data.model.User


@Composable
fun HealthPersonnelScreen(
    viewModel: HealthPersonnelViewModel,
    onUserClick: (String) -> Unit
) {
    val users by viewModel.users.observeAsState(emptyList())
    val error by viewModel.error.observeAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Usuarios",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(
                    user = user,
                    onClick = { onUserClick(user.id) }
                )
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
fun UserCard(
    user: User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "ID: ${user.id}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HealthStatusChip(healthStatus = user.healthStatus)
        }
    }
}

@Composable
fun HealthStatusChip(healthStatus: HealthStatus) {
    val (color, text) = when (healthStatus) {
        HealthStatus.HEALTHY -> MaterialTheme.colorScheme.primary to "Sano"
        HealthStatus.AT_RISK -> MaterialTheme.colorScheme.tertiary to "En Riesgo"
        HealthStatus.INFECTED -> MaterialTheme.colorScheme.error to "Infectado"
    }

    Surface(
        color = color.copy(alpha = 0.2f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium
        )
    }
}